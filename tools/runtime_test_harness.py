#!/usr/bin/env python3
"""Deterministic Witchery resource, GameTest, and server lifecycle gates."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import signal
import socket
import struct
import subprocess
import sys
import threading
import time
from typing import Callable, Iterable


ROOT = Path(__file__).resolve().parents[1]
MANIFEST_PATH = ROOT / "tools" / "runtime_test_manifest.json"
RUNTIME_ROOT = (ROOT / "build" / "runtime-test").resolve()
GAME_TEST_SOURCE_ROOT = ROOT / "src" / "main" / "java"
TEST_SOURCE_ROOT = ROOT / "src" / "test" / "java"
GAME_TEST_REGISTRATION = re.compile(r'TEST_FUNCTIONS\.register\(\s*"([^"]+)"', re.MULTILINE)
RCON_PROBE_TIMEOUT_SECONDS = 10.0
RCON_COMMAND_TIMEOUT_SECONDS = 60.0
STARTUP_FAILURES = (
    "Failed to start the minecraft server",
    "Exception in server tick loop",
    "Mod loading errors encountered",
    "There was an error during mod loading",
    "Failed to load datapacks",
)
GAME_TEST_FAILURES = (
    "required test(s) failed",
    "required tests failed",
    "Game test failed",
    " failed at ",
)


class HarnessError(RuntimeError):
    pass


def load_manifest() -> dict:
    try:
        manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise HarnessError(f"Cannot read {MANIFEST_PATH.relative_to(ROOT)}: {exc}") from exc
    if manifest.get("schema_version") != 1:
        raise HarnessError("runtime_test_manifest.json must use schema_version 1")
    return manifest


def discover_registered_game_tests() -> set[str]:
    discovered: set[str] = set()
    for source in GAME_TEST_SOURCE_ROOT.rglob("*GameTests.java"):
        text = source.read_text(encoding="utf-8")
        discovered.update(f"witchery:{match}" for match in GAME_TEST_REGISTRATION.findall(text))
    return discovered


def validate_manifest(manifest: dict) -> None:
    resource_tests = manifest.get("resource_contract_tests")
    game_tests = manifest.get("game_tests")
    if not isinstance(resource_tests, list) or not resource_tests:
        raise HarnessError("resource_contract_tests must be a non-empty array")
    if not isinstance(game_tests, list) or not game_tests:
        raise HarnessError("game_tests must be a non-empty array")
    if len(resource_tests) != len(set(resource_tests)):
        raise HarnessError("resource_contract_tests contains duplicate classes")

    for class_name in resource_tests:
        if not isinstance(class_name, str) or not class_name.startswith("com.emoniph.witchery."):
            raise HarnessError(f"Invalid resource-contract class: {class_name!r}")
        source = TEST_SOURCE_ROOT / Path(*class_name.split(".")).with_suffix(".java")
        if not source.is_file():
            raise HarnessError(f"Resource-contract source does not exist: {source.relative_to(ROOT)}")
        if not re.search(rf"\bclass\s+{re.escape(class_name.rsplit('.', 1)[-1])}\b",
                         source.read_text(encoding="utf-8")):
            raise HarnessError(f"Resource-contract class declaration is missing: {class_name}")

    manifest_ids: list[str] = []
    for entry in game_tests:
        if not isinstance(entry, dict):
            raise HarnessError("Each game_tests entry must be an object")
        test_id = entry.get("id")
        lane = entry.get("lane")
        timeout = entry.get("timeout_seconds")
        if not isinstance(test_id, str) or not test_id.startswith("witchery:"):
            raise HarnessError(f"Invalid GameTest id: {test_id!r}")
        if lane not in {"ci", "manual_restart"}:
            raise HarnessError(f"Invalid lane for {test_id}: {lane!r}")
        if not isinstance(timeout, int) or timeout <= 0:
            raise HarnessError(f"Invalid timeout_seconds for {test_id}: {timeout!r}")
        if lane == "manual_restart" and not str(entry.get("reason", "")).strip():
            raise HarnessError(f"Manual GameTest {test_id} must explain why it is manual")
        manifest_ids.append(test_id)
    if len(manifest_ids) != len(set(manifest_ids)):
        raise HarnessError("game_tests contains duplicate ids")

    registered = discover_registered_game_tests()
    listed = set(manifest_ids)
    if registered != listed:
        missing = sorted(registered - listed)
        stale = sorted(listed - registered)
        raise HarnessError(f"GameTest manifest drift: missing={missing}, stale={stale}")

    save_reload = manifest.get("save_reload")
    if not isinstance(save_reload, dict):
        raise HarnessError("save_reload must be an object")
    machines = save_reload.get("machines")
    if not isinstance(machines, list) or not machines:
        raise HarnessError("save_reload.machines must be a non-empty array")
    for command_key in ("pre_save_commands", "post_reload_commands"):
        commands = save_reload.get(command_key, [])
        if not isinstance(commands, list) or not all(
                isinstance(command, str) and command.strip() for command in commands):
            raise HarnessError(f"save_reload.{command_key} must be a list of non-empty commands")
    names: set[str] = set()
    positions: set[tuple[int, int, int]] = set()
    for machine in machines:
        if not isinstance(machine, dict):
            raise HarnessError("Each save_reload.machines entry must be an object")
        name = machine.get("name")
        block = machine.get("block")
        position = machine.get("position")
        if not isinstance(name, str) or not name.strip() or name in names:
            raise HarnessError(f"Invalid or duplicate save/reload machine name: {name!r}")
        if not isinstance(block, str) or ":" not in block:
            raise HarnessError(f"Invalid save/reload block for {name}: {block!r}")
        if not (isinstance(position, list) and len(position) == 3
                and all(isinstance(value, int) for value in position)):
            raise HarnessError(f"save_reload machine {name} position must contain three integers")
        position_key = tuple(position)
        if position_key in positions:
            raise HarnessError(f"Duplicate save/reload machine position: {position}")
        names.add(name)
        positions.add(position_key)

        values = machine.get("block_entity_values", {})
        if not isinstance(values, dict) or not all(
                isinstance(key, str) and isinstance(value, int) for key, value in values.items()):
            raise HarnessError(f"Invalid block_entity_values for save/reload machine {name}")
        items = machine.get("items", [])
        if not isinstance(items, list):
            raise HarnessError(f"Invalid items for save/reload machine {name}")
        for item in items:
            if not isinstance(item, dict):
                raise HarnessError(f"Invalid item contract for save/reload machine {name}")
            if not isinstance(item.get("slot"), int) or item["slot"] < 0:
                raise HarnessError(f"Invalid item slot for save/reload machine {name}")
            if not isinstance(item.get("item"), str) or ":" not in item["item"]:
                raise HarnessError(f"Invalid item id for save/reload machine {name}")
            if not isinstance(item.get("count"), int) or item["count"] <= 0:
                raise HarnessError(f"Invalid item count for save/reload machine {name}")
            if not isinstance(item.get("count_path"), str) or not item["count_path"].strip():
                raise HarnessError(f"Missing item count path for save/reload machine {name}")
        setup_blocks = machine.get("setup_blocks", [])
        if not isinstance(setup_blocks, list):
            raise HarnessError(f"Invalid setup_blocks for save/reload machine {name}")
        for setup in setup_blocks:
            if not isinstance(setup, dict) or not isinstance(setup.get("block"), str):
                raise HarnessError(f"Invalid setup block for save/reload machine {name}")
            setup_position = setup.get("position")
            if not (isinstance(setup_position, list) and len(setup_position) == 3
                    and all(isinstance(value, int) for value in setup_position)):
                raise HarnessError(f"Invalid setup block position for save/reload machine {name}")

    village = manifest.get("village_certification")
    if not isinstance(village, dict):
        raise HarnessError("village_certification must be an object")
    structures = village.get("structures")
    if not isinstance(structures, list) or len(structures) != 5:
        raise HarnessError("village_certification.structures must contain five entries")
    biome_names: set[str] = set()
    structure_ids: set[str] = set()
    for entry in structures:
        if not isinstance(entry, dict):
            raise HarnessError("Each village certification entry must be an object")
        biome = entry.get("biome")
        structure = entry.get("structure")
        if not isinstance(biome, str) or not biome.strip() or biome in biome_names:
            raise HarnessError(f"Invalid or duplicate village biome: {biome!r}")
        if not isinstance(structure, str) or ":" not in structure or structure in structure_ids:
            raise HarnessError(f"Invalid or duplicate village structure: {structure!r}")
        biome_names.add(biome)
        structure_ids.add(structure)
    if not isinstance(village.get("force_radius_blocks"), int) or village["force_radius_blocks"] <= 0:
        raise HarnessError("village_certification.force_radius_blocks must be positive")
    if not isinstance(village.get("timeout_seconds"), int) or village["timeout_seconds"] <= 0:
        raise HarnessError("village_certification.timeout_seconds must be positive")
    if not isinstance(village.get("max_candidate_villages"), int) \
            or not 1 <= village["max_candidate_villages"] <= 9:
        raise HarnessError("village_certification.max_candidate_villages must be between 1 and 9")
    if not isinstance(village.get("candidate_probe_spacing_blocks"), int) \
            or village["candidate_probe_spacing_blocks"] <= 0:
        raise HarnessError("village_certification.candidate_probe_spacing_blocks must be positive")

    wild = manifest.get("wild_structure_certification")
    if not isinstance(wild, dict):
        raise HarnessError("wild_structure_certification must be an object")
    wild_structures = wild.get("structures")
    if not isinstance(wild_structures, list) or len(wild_structures) != 6:
        raise HarnessError("wild_structure_certification.structures must contain six entries")
    expected_wild = {
        "witchery:coven_circle", "witchery:straw_man", "witchery:hobgoblin_hut",
        "witchery:witch_hut", "witchery:warped_coven", "witchery:vanishing_hut",
    }
    actual_wild: set[str] = set()
    wild_names: set[str] = set()
    for entry in wild_structures:
        if not isinstance(entry, dict):
            raise HarnessError("Each wild structure certification entry must be an object")
        name = entry.get("name")
        structure = entry.get("structure")
        dimension = entry.get("dimension")
        if not isinstance(name, str) or not name.strip() or name in wild_names:
            raise HarnessError(f"Invalid or duplicate wild structure name: {name!r}")
        if not isinstance(structure, str) or structure not in expected_wild or structure in actual_wild:
            raise HarnessError(f"Invalid or duplicate wild structure id: {structure!r}")
        if dimension not in {"minecraft:overworld", "minecraft:the_nether"}:
            raise HarnessError(f"Invalid wild structure dimension for {name}: {dimension!r}")
        if structure == "witchery:warped_coven" and dimension != "minecraft:the_nether":
            raise HarnessError("warped_coven certification must run in the Nether")
        if structure != "witchery:warped_coven" and dimension != "minecraft:overworld":
            raise HarnessError(f"{structure} certification must run in the Overworld")
        wild_names.add(name)
        actual_wild.add(structure)
    if actual_wild != expected_wild:
        raise HarnessError("wild structure certification roster drifted")
    for field in ("force_radius_blocks", "timeout_seconds", "relocation_timeout_seconds"):
        if not isinstance(wild.get(field), int) or wild[field] <= 0:
            raise HarnessError(f"wild_structure_certification.{field} must be positive")

    wells = manifest.get("witch_well_certification")
    if not isinstance(wells, dict):
        raise HarnessError("witch_well_certification must be an object")
    if wells.get("variants") != ["regular", "small", "cave"]:
        raise HarnessError("witch_well_certification.variants must be regular, small, cave")
    for field in ("force_radius_blocks", "timeout_seconds", "max_candidate_wells",
                  "spacing", "separation", "salt"):
        if not isinstance(wells.get(field), int) or wells[field] <= 0:
            raise HarnessError(f"witch_well_certification.{field} must be positive")
    if wells.get("level_seed") != -119995921:
        raise HarnessError("witch_well_certification.level_seed must match witchery-runtime-test")
    if wells["spacing"] != 40 or wells["separation"] != 12 or wells["salt"] != 912347651:
        raise HarnessError("witch_well_certification random-spread contract drifted")
    if wells["separation"] >= wells["spacing"]:
        raise HarnessError("witch_well_certification separation must be below spacing")
    if wells["max_candidate_wells"] > 64:
        raise HarnessError("witch_well_certification.max_candidate_wells must not exceed 64")


def safe_reset_directory(target: Path) -> None:
    target = target.resolve()
    try:
        target.relative_to(RUNTIME_ROOT)
    except ValueError as exc:
        raise HarnessError(f"Refusing to reset path outside {RUNTIME_ROOT}: {target}") from exc
    if target == RUNTIME_ROOT:
        raise HarnessError("Refusing to reset the runtime-test root itself")
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True)


def find_free_ports(count: int) -> tuple[int, ...]:
    sockets: list[socket.socket] = []
    try:
        for _ in range(count):
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.bind(("127.0.0.1", 0))
            sockets.append(sock)
        return tuple(int(sock.getsockname()[1]) for sock in sockets)
    finally:
        for sock in sockets:
            sock.close()


def write_server_files(work_dir: Path, rcon_port: int, server_port: int, password: str,
                       generate_structures: bool = False, spawn_npcs: bool = False) -> None:
    (work_dir / "eula.txt").write_text("eula=true\n", encoding="ascii")
    properties = {
        "allow-flight": "true",
        "difficulty": "normal",
        "enable-command-block": "false",
        "enable-query": "false",
        "enable-rcon": "true",
        "force-gamemode": "true",
        "gamemode": "creative",
        "generate-structures": str(generate_structures).lower(),
        "level-name": "runtime-world",
        "level-seed": "witchery-runtime-test",
        "max-players": "4",
        "max-tick-time": "-1",
        "motd": "Witchery Runtime Test",
        "online-mode": "false",
        "pause-when-empty-seconds": "0",
        "prevent-proxy-connections": "false",
        "query.port": str(server_port),
        "rcon.password": password,
        "rcon.port": str(rcon_port),
        "server-ip": "127.0.0.1",
        "server-port": str(server_port),
        "simulation-distance": "4",
        "spawn-animals": "false",
        "spawn-monsters": "true",
        "spawn-npcs": str(spawn_npcs).lower(),
        "sync-chunk-writes": "true",
        "view-distance": "4",
    }
    text = "".join(f"{key}={value}\n" for key, value in sorted(properties.items()))
    (work_dir / "server.properties").write_text(text, encoding="ascii")


def gradle_command(arguments: Iterable[str]) -> list[str]:
    args = list(arguments)
    if os.name == "nt":
        return ["cmd.exe", "/d", "/s", "/c", str(ROOT / "gradlew.bat"), *args]
    return [str(ROOT / "gradlew"), *args]


def run_logged_command(label: str, arguments: list[str], output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    log_path = output_dir / f"{label}.log"
    command = gradle_command(arguments)
    print(f"[runtime-test] {' '.join(command)}", flush=True)
    with log_path.open("w", encoding="utf-8", newline="\n") as log:
        process = subprocess.Popen(
            command,
            cwd=ROOT,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            bufsize=1,
        )
        assert process.stdout is not None
        for line in process.stdout:
            print(line, end="", flush=True)
            log.write(line)
            log.flush()
        return_code = process.wait()
    if return_code != 0:
        raise HarnessError(f"{label} failed with exit code {return_code}; see {log_path}")


def receive_exact(sock: socket.socket, size: int) -> bytes:
    chunks = bytearray()
    while len(chunks) < size:
        chunk = sock.recv(size - len(chunks))
        if not chunk:
            raise HarnessError("RCON connection closed unexpectedly")
        chunks.extend(chunk)
    return bytes(chunks)


def receive_rcon_packet(sock: socket.socket) -> tuple[int, int, str]:
    length = struct.unpack("<i", receive_exact(sock, 4))[0]
    if length < 10 or length > 4 * 1024 * 1024:
        raise HarnessError(f"Invalid RCON packet length: {length}")
    payload = receive_exact(sock, length)
    request_id, packet_type = struct.unpack("<ii", payload[:8])
    body = payload[8:-2].decode("utf-8", errors="replace")
    return request_id, packet_type, body


def send_rcon_packet(sock: socket.socket, request_id: int, packet_type: int, body: str) -> None:
    encoded = body.encode("utf-8")
    payload = struct.pack("<ii", request_id, packet_type) + encoded + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(payload)) + payload)


def rcon_command(port: int, password: str, command: str, timeout: float = 10.0) -> str:
    with socket.create_connection(("127.0.0.1", port), timeout=timeout) as sock:
        sock.settimeout(timeout)
        auth_id = 100
        send_rcon_packet(sock, auth_id, 3, password)
        for _ in range(3):
            response_id, _, _ = receive_rcon_packet(sock)
            if response_id == -1:
                raise HarnessError("RCON authentication failed")
            if response_id == auth_id:
                break
        else:
            raise HarnessError("RCON authentication response was not received")

        command_id = 101
        send_rcon_packet(sock, command_id, 2, command)
        for _ in range(3):
            response_id, _, body = receive_rcon_packet(sock)
            if response_id == command_id:
                return body
        raise HarnessError(f"RCON response was not received for command: {command}")


class ServerProcess:
    def __init__(self, work_dir: Path, label: str, rcon_port: int, password: str,
                 startup_timeout: int) -> None:
        self.work_dir = work_dir
        self.label = label
        self.rcon_port = rcon_port
        self.password = password
        self.startup_timeout = startup_timeout
        self.process: subprocess.Popen[str] | None = None
        self.lines: list[str] = []
        self.condition = threading.Condition()
        self.reader: threading.Thread | None = None
        self.log = None

    def start(self) -> None:
        runtime_dir = self.work_dir.relative_to(ROOT)
        command = gradle_command([
            "runRuntimeTestServer",
            f"-PwitcheryRuntimeDir={runtime_dir.as_posix()}",
            "--console=plain",
            "--stacktrace",
            "--no-daemon",
        ])
        log_path = self.work_dir / f"process-{self.label}.log"
        self.log = log_path.open("w", encoding="utf-8", newline="\n")
        creation_flags = subprocess.CREATE_NEW_PROCESS_GROUP if os.name == "nt" else 0
        self.process = subprocess.Popen(
            command,
            cwd=ROOT,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            bufsize=1,
            creationflags=creation_flags,
            start_new_session=os.name != "nt",
        )
        self.reader = threading.Thread(target=self._read_output, name=f"server-{self.label}", daemon=True)
        self.reader.start()
        self.wait_for_line(lambda line: "Done (" in line and "For help" in line,
                           self.startup_timeout, 0, STARTUP_FAILURES)
        self._wait_for_rcon()

    def _read_output(self) -> None:
        assert self.process is not None and self.process.stdout is not None and self.log is not None
        for line in self.process.stdout:
            print(line, end="", flush=True)
            self.log.write(line)
            self.log.flush()
            with self.condition:
                self.lines.append(line.rstrip("\r\n"))
                self.condition.notify_all()
        with self.condition:
            self.condition.notify_all()

    def _wait_for_rcon(self) -> None:
        deadline = time.monotonic() + 30
        last_error: Exception | None = None
        while time.monotonic() < deadline:
            try:
                response = self.command("list", RCON_PROBE_TIMEOUT_SECONDS)
                if "player" in response.lower():
                    return
            except (OSError, HarnessError) as exc:
                last_error = exc
            time.sleep(0.5)
        raise HarnessError(f"RCON did not become ready: {last_error}")

    def command(self, command: str, timeout: float = RCON_COMMAND_TIMEOUT_SECONDS) -> str:
        return rcon_command(self.rcon_port, self.password, command, timeout)

    def line_count(self) -> int:
        with self.condition:
            return len(self.lines)

    def wait_for_line(self, success: Callable[[str], bool], timeout: float, start_index: int,
                      failures: Iterable[str] = ()) -> tuple[int, str]:
        deadline = time.monotonic() + timeout
        failure_text = tuple(value.lower() for value in failures)
        cursor = start_index
        while True:
            with self.condition:
                while cursor < len(self.lines):
                    line = self.lines[cursor]
                    cursor += 1
                    lowered = line.lower()
                    if any(value in lowered for value in failure_text):
                        raise HarnessError(f"Server reported failure: {line}")
                    if success(line):
                        return cursor - 1, line
                process_done = self.process is not None and self.process.poll() is not None
                if process_done:
                    raise HarnessError(
                        f"Server exited with code {self.process.returncode} before the expected log marker")
                remaining = deadline - time.monotonic()
                if remaining <= 0:
                    raise HarnessError(f"Timed out after {timeout:.0f}s waiting for a server log marker")
                self.condition.wait(timeout=min(remaining, 0.5))

    def graceful_stop(self) -> None:
        if self.process is None or self.process.poll() is not None:
            raise HarnessError("Server exited before graceful shutdown could be requested")
        start_index = self.line_count()
        response = self.command("stop")
        if response and "Stopping" not in response:
            print(f"[runtime-test] stop response: {response}", flush=True)
        self.wait_for_line(lambda line: "All dimensions are saved" in line,
                           180, start_index, STARTUP_FAILURES)
        try:
            return_code = self.process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            print("[runtime-test] Server saved cleanly; terminating the lingering Gradle wrapper.", flush=True)
            self.force_stop()
            return
        if self.reader is not None:
            self.reader.join(timeout=10)
        if self.log is not None:
            self.log.close()
            self.log = None
        if return_code != 0:
            raise HarnessError(f"Server Gradle process exited with code {return_code}")

    def force_stop(self) -> None:
        if self.process is None or self.process.poll() is not None:
            return
        if os.name == "nt":
            subprocess.run(
                ["taskkill", "/PID", str(self.process.pid), "/T", "/F"],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False,
            )
        else:
            try:
                os.killpg(self.process.pid, signal.SIGTERM)
                self.process.wait(timeout=20)
            except (ProcessLookupError, subprocess.TimeoutExpired):
                try:
                    os.killpg(self.process.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
        if self.reader is not None:
            self.reader.join(timeout=5)
        if self.log is not None:
            self.log.close()
            self.log = None


def snapshot_minecraft_log(work_dir: Path, label: str) -> None:
    latest = work_dir / "logs" / "latest.log"
    if latest.is_file():
        shutil.copy2(latest, work_dir / f"minecraft-{label}.log")


def assert_no_wall_suffocation(work_dir: Path, label: str) -> None:
    log_path = work_dir / f"minecraft-{label}.log"
    if not log_path.is_file():
        raise HarnessError(f"Village certification log is missing: {log_path}")
    if "suffocated in a wall" in log_path.read_text(encoding="utf-8", errors="replace"):
        raise HarnessError(f"Village wall generation suffocated a living entity: {log_path}")


def write_summary(work_dir: Path, summary: dict) -> None:
    (work_dir / "summary.json").write_text(
        json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def command_must_succeed(server: ServerProcess, command: str,
                         expected: str | None = None,
                         timeout: float = RCON_COMMAND_TIMEOUT_SECONDS) -> str:
    response = server.command(command, timeout)
    lowered = response.lower()
    failures = (
        "unknown command",
        "incorrect argument",
        "could not",
        "cannot",
        "no tests",
        "not found",
        "not loaded",
        "no items were modified",
    )
    if any(value in lowered for value in failures):
        raise HarnessError(f"Command failed: {command}\nRCON response: {response}")
    if expected is not None and expected.lower() not in lowered:
        raise HarnessError(
            f"Command did not report expected value {expected!r}: {command}\nRCON response: {response}")
    return response


def assert_saved_machine_state(server: ServerProcess, machines: list[dict],
                               storage: str, marker: int) -> None:
    for machine in machines:
        x, y, z = machine["position"]
        command_must_succeed(
            server,
            f"execute if block {x} {y} {z} {machine['block']} "
            f"run data get storage {storage} marker",
            str(marker),
        )
        for key, value in machine.get("block_entity_values", {}).items():
            command_must_succeed(server, f"data get block {x} {y} {z} {key}", str(value))
        for item in machine.get("items", []):
            command_must_succeed(
                server,
                f"execute if items block {x} {y} {z} container.{item['slot']} {item['item']} "
                f"run data get storage {storage} marker",
                str(marker),
            )
            command_must_succeed(
                server,
                f"data get block {x} {y} {z} {item['count_path']}",
                str(item["count"]),
            )


def prepare_runtime_server(work_dir: Path, reset: bool,
                           generate_structures: bool = False,
                           spawn_npcs: bool = False) -> tuple[int, str]:
    if reset:
        safe_reset_directory(work_dir)
    else:
        work_dir.mkdir(parents=True, exist_ok=True)
    rcon_port, server_port = find_free_ports(2)
    password = secrets.token_urlsafe(24)
    write_server_files(work_dir, rcon_port, server_port, password,
                       generate_structures=generate_structures, spawn_npcs=spawn_npcs)
    return rcon_port, password


def run_resource_contracts(manifest: dict) -> None:
    output_dir = RUNTIME_ROOT / "resource-contracts"
    safe_reset_directory(output_dir)
    arguments = ["test", "--console=plain", "--stacktrace", "--no-daemon"]
    for class_name in manifest["resource_contract_tests"]:
        arguments.extend(("--tests", class_name))
    run_logged_command("resource-contracts", arguments, output_dir)
    write_summary(output_dir, {
        "status": "passed",
        "resource_contract_tests": manifest["resource_contract_tests"],
    })


def run_game_tests(manifest: dict, startup_timeout: int,
                   selected_ids: set[str] | None = None) -> None:
    work_dir = RUNTIME_ROOT / "game-tests"
    rcon_port, password = prepare_runtime_server(work_dir, reset=True)
    server = ServerProcess(work_dir, "game-tests", rcon_port, password, startup_timeout)
    summary: dict = {
        "status": "failed",
        "tests": [],
        "fresh_world": True,
        "level_seed": "witchery-runtime-test",
    }
    started = False
    stopped = False
    try:
        server.start()
        started = True
        ci_entries = [item for item in manifest["game_tests"] if item["lane"] == "ci"]
        available_ids = {item["id"] for item in ci_entries}
        if selected_ids is not None:
            unknown = selected_ids - available_ids
            if unknown:
                raise HarnessError(f"Selected GameTests are not in the CI lane: {sorted(unknown)}")
            ci_entries = [item for item in ci_entries if item["id"] in selected_ids]
        for test_index, entry in enumerate(ci_entries):
            test_id = entry["id"]
            print(f"[runtime-test] Running {test_id}", flush=True)
            test_x = test_index * 128
            test_y = 200
            area_start = test_x - 16
            area_end = test_x + 96
            command_must_succeed(
                server, f"forceload add {area_start} -16 {area_end} 96")
            command_must_succeed(
                server, f"setblock {test_x} {test_y} 0 minecraft:stone destroy")
            start_index = server.line_count()
            started_at = time.monotonic()
            response = command_must_succeed(
                server, f"execute positioned {test_x} {test_y} 0 run test run {test_id}")
            if response:
                print(f"[runtime-test] RCON: {response}", flush=True)
            completion_index, _ = server.wait_for_line(
                lambda line: "Game Test complete! 1 test(s) were run" in line,
                entry["timeout_seconds"], start_index, GAME_TEST_FAILURES)
            server.wait_for_line(
                lambda line: "All required tests passed :)" in line,
                15, completion_index, GAME_TEST_FAILURES)
            duration = round(time.monotonic() - started_at, 3)
            summary["tests"].append({"id": test_id, "status": "passed", "seconds": duration})
            command_must_succeed(
                server, f"execute positioned {test_x} {test_y} 0 run test clearall")
        server.graceful_stop()
        stopped = True
        snapshot_minecraft_log(work_dir, "game-tests")
        summary["status"] = "passed"
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not stopped and server.process is not None:
            if not started:
                server.force_stop()
            else:
                try:
                    server.graceful_stop()
                    snapshot_minecraft_log(work_dir, "game-tests")
                except Exception as stop_error:
                    summary["shutdown_error"] = str(stop_error)
                    server.force_stop()
        write_summary(work_dir, summary)


def run_blight_heart_restart(manifest: dict, startup_timeout: int) -> None:
    test_id = "witchery:blight_heart_restart_resume"
    entry = next((item for item in manifest["game_tests"] if item["id"] == test_id), None)
    if entry is None or entry["lane"] != "manual_restart":
        raise HarnessError(f"Missing manual restart GameTest manifest entry: {test_id}")

    work_dir = RUNTIME_ROOT / "blight-heart-restart"
    summary: dict = {"status": "failed", "test": test_id, "stages": []}

    def run_phase(server: ServerProcess, phase: str) -> None:
        command_must_succeed(server, "forceload add -16 -16 96 96")
        command_must_succeed(server, "setblock 0 200 0 minecraft:stone destroy")
        start_index = server.line_count()
        response = command_must_succeed(
            server, f"execute positioned 0 200 0 run test run {test_id}")
        if response:
            print(f"[runtime-test] RCON: {response}", flush=True)
        completion_index, _ = server.wait_for_line(
            lambda line: "Game Test complete! 1 test(s) were run" in line,
            entry["timeout_seconds"], start_index, GAME_TEST_FAILURES)
        server.wait_for_line(
            lambda line: "All required tests passed :)" in line,
            15, completion_index, GAME_TEST_FAILURES)
        command_must_succeed(server, "execute positioned 0 200 0 run test clearall")
        command_must_succeed(server, "save-all flush", "saved")
        summary["stages"].append(phase)

    first_port, first_password = prepare_runtime_server(work_dir, reset=True)
    first = ServerProcess(work_dir, "stage-one", first_port, first_password, startup_timeout)
    first_started = False
    first_stopped = False
    try:
        first.start()
        first_started = True
        run_phase(first, "partial_ledger_saved")
        first.graceful_stop()
        first_stopped = True
        snapshot_minecraft_log(work_dir, "stage-one")
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not first_stopped and first.process is not None:
            if first_started:
                try:
                    first.graceful_stop()
                    snapshot_minecraft_log(work_dir, "stage-one")
                except Exception:
                    first.force_stop()
            else:
                first.force_stop()
        if "error" in summary:
            write_summary(work_dir, summary)

    second_port, second_password = prepare_runtime_server(work_dir, reset=False)
    second = ServerProcess(work_dir, "stage-two", second_port, second_password, startup_timeout)
    second_started = False
    second_stopped = False
    try:
        second.start()
        second_started = True
        run_phase(second, "restart_resumed_and_cleansed")
        second.graceful_stop()
        second_stopped = True
        snapshot_minecraft_log(work_dir, "stage-two")
        summary["status"] = "passed"
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not second_stopped and second.process is not None:
            if second_started:
                try:
                    second.graceful_stop()
                    snapshot_minecraft_log(work_dir, "stage-two")
                except Exception as stop_error:
                    summary["shutdown_error"] = str(stop_error)
                    second.force_stop()
            else:
                second.force_stop()
        write_summary(work_dir, summary)


def run_save_reload(manifest: dict, startup_timeout: int) -> None:
    work_dir = RUNTIME_ROOT / "save-reload"
    rcon_port, password = prepare_runtime_server(work_dir, reset=True)
    config = manifest["save_reload"]
    machines = config["machines"]
    storage = config["storage"]
    marker = config["storage_marker"]
    pre_save_commands = config.get("pre_save_commands", [])
    post_reload_commands = config.get("post_reload_commands", [])
    summary: dict = {"status": "failed", "stages": []}

    first = ServerProcess(work_dir, "save", rcon_port, password, startup_timeout)
    first_started = False
    first_stopped = False
    try:
        first.start()
        first_started = True
        for machine in machines:
            x, y, z = machine["position"]
            command_must_succeed(first, f"forceload add {x} {z}")
            for setup in machine.get("setup_blocks", []):
                setup_x, setup_y, setup_z = setup["position"]
                command_must_succeed(
                    first, f"setblock {setup_x} {setup_y} {setup_z} {setup['block']}")
            command_must_succeed(first, f"setblock {x} {y} {z} {machine['block']}")
            values = machine.get("block_entity_values", {})
            if values:
                nbt = ",".join(f"{key}:{value}" for key, value in values.items())
                command_must_succeed(first, f"data merge block {x} {y} {z} {{{nbt}}}")
            for item in machine.get("items", []):
                command_must_succeed(
                    first,
                    f"item replace block {x} {y} {z} container.{item['slot']} "
                    f"with {item['item']} {item['count']}",
                )
        command_must_succeed(first, f"data modify storage {storage} marker set value {marker}")
        for command in pre_save_commands:
            command_must_succeed(first, command)
        assert_saved_machine_state(first, machines, storage, marker)
        command_must_succeed(first, "save-all flush", "saved")
        first.force_stop()
        first_stopped = True
        snapshot_minecraft_log(work_dir, "save")
        summary["stages"].append("saved_then_process_killed")
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not first_stopped and first.process is not None:
            if first_started:
                try:
                    first.graceful_stop()
                    snapshot_minecraft_log(work_dir, "save")
                except Exception:
                    first.force_stop()
            else:
                first.force_stop()
        if "error" in summary:
            write_summary(work_dir, summary)

    second_port, second_password = prepare_runtime_server(work_dir, reset=False)
    second = ServerProcess(work_dir, "reload", second_port, second_password, startup_timeout)
    second_started = False
    second_stopped = False
    try:
        second.start()
        second_started = True
        for machine in machines:
            x, _, z = machine["position"]
            command_must_succeed(second, f"forceload add {x} {z}")
        assert_saved_machine_state(second, machines, storage, marker)
        for command in post_reload_commands:
            command_must_succeed(second, command)
        command_must_succeed(second, "save-all flush", "saved")
        second.graceful_stop()
        second_stopped = True
        snapshot_minecraft_log(work_dir, "reload")
        level_dat = work_dir / "runtime-world" / "level.dat"
        if not level_dat.is_file():
            raise HarnessError(f"Reloaded world is missing {level_dat}")
        summary["stages"].append("reloaded")
        summary["status"] = "passed"
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not second_stopped and second.process is not None:
            if not second_started:
                second.force_stop()
            else:
                try:
                    second.graceful_stop()
                    snapshot_minecraft_log(work_dir, "reload")
                except Exception as stop_error:
                    summary["shutdown_error"] = str(stop_error)
                    second.force_stop()
        write_summary(work_dir, summary)


VILLAGE_LOCATE_PATTERN = re.compile(r"\[\s*(-?\d+)\s*,\s*(?:~|-?\d+)\s*,\s*(-?\d+)\s*\]")
WILD_ANCHOR_PATTERN = re.compile(r"anchor=(-?\d+),(-?\d+),(-?\d+)")
WELL_VARIANT_PATTERN = re.compile(r"variant=(regular|small|cave)")


def locate_village(server: ServerProcess, structure: str,
                   origin_x: int = 0, origin_z: int = 0) -> tuple[int, int, str]:
    response = server.command(
        f"execute positioned {origin_x} 80 {origin_z} run locate structure {structure}",
        timeout=180,
    )
    match = VILLAGE_LOCATE_PATTERN.search(response)
    if match is None:
        raise HarnessError(f"Could not parse village location for {structure}: {response}")
    return int(match.group(1)), int(match.group(2)), response


def load_village_area(server: ServerProcess, x: int, z: int, radius: int) -> None:
    command_must_succeed(
        server, f"forceload add {x - radius} {z - radius} {x + radius} {z + radius}")


def wait_for_loaded_village_audit(server: ServerProcess, x: int, z: int,
                                  timeout_seconds: int = 60) -> str:
    deadline = time.monotonic() + timeout_seconds
    last_response = ""
    while time.monotonic() < deadline:
        last_response = server.command(
            f"execute positioned {x} 80 {z} run witchery verify village")
        print(f"[runtime-test] Village preflight at {x}, {z}: {last_response}", flush=True)
        if "no_loaded_village" not in last_response:
            return last_response
        time.sleep(2)
    raise HarnessError(
        f"Village failed to load at {x}, {z}; last response: {last_response}")


def village_probe_origins(spacing: int, maximum: int) -> list[tuple[int, int]]:
    directions = (
        (0, 0), (1, 0), (-1, 0), (0, 1), (0, -1),
        (1, 1), (-1, 1), (1, -1), (-1, -1),
    )
    return [(x * spacing, z * spacing) for x, z in directions[:maximum]]


def wait_for_village_certification(server: ServerProcess, x: int, z: int,
                                   timeout_seconds: int) -> str:
    deadline = time.monotonic() + timeout_seconds
    last_response = ""
    while time.monotonic() < deadline:
        last_response = server.command(
            f"execute positioned {x} 80 {z} run witchery verify village")
        print(f"[runtime-test] Village audit at {x}, {z}: {last_response}", flush=True)
        if "[VillageCertification] PASS" in last_response:
            return last_response
        time.sleep(5)
    raise HarnessError(
        f"Village certification timed out at {x}, {z}; last response: {last_response}")


def certify_villages(server: ServerProcess, config: dict, queue_walls: bool,
                     capture_dir: Path, stage: str,
                     known_locations: list[dict] | None = None) -> list[dict]:
    reports: list[dict] = []
    capture_dir.mkdir(parents=True, exist_ok=True)
    for index, entry in enumerate(config["structures"]):
        biome = entry["biome"]
        structure = entry["structure"]
        rejected_candidates: list[dict] = []
        if known_locations is None:
            seen_locations: set[tuple[int, int]] = set()
            selected = None
            for origin_x, origin_z in village_probe_origins(
                    config["candidate_probe_spacing_blocks"],
                    config["max_candidate_villages"]):
                candidate_x, candidate_z, candidate_locate = locate_village(
                    server, structure, origin_x, origin_z)
                if (candidate_x, candidate_z) in seen_locations:
                    continue
                seen_locations.add((candidate_x, candidate_z))
                load_village_area(
                    server, candidate_x, candidate_z, config["force_radius_blocks"])
                preflight = wait_for_loaded_village_audit(server, candidate_x, candidate_z)
                if "no_witchery_buildings" not in preflight:
                    selected = (candidate_x, candidate_z, candidate_locate)
                    break
                rejected_candidates.append({
                    "x": candidate_x,
                    "z": candidate_z,
                    "probe_origin": [origin_x, origin_z],
                    "locate": candidate_locate,
                    "audit": preflight,
                })
                print(
                    f"[runtime-test] Skipping {biome} village at "
                    f"{candidate_x}, {candidate_z}: no Witchery building was selected",
                    flush=True,
                )
                command_must_succeed(server, "forceload remove all")
            if selected is None:
                raise HarnessError(
                    f"No {biome} village with a Witchery building found after "
                    f"{len(seen_locations)} unique candidate(s)")
            x, z, locate_response = selected
        else:
            known = known_locations[index]
            if known["biome"] != biome or known["structure"] != structure:
                raise HarnessError("Village restart location order drifted")
            x, z = known["x"], known["z"]
            locate_response = "reused persisted fresh-world coordinates"

        if known_locations is not None:
            load_village_area(server, x, z, config["force_radius_blocks"])
        wall_response = "persisted"
        if queue_walls:
            wall_response = command_must_succeed(
                server, f"execute positioned {x} 80 {z} run witchery walls")
        audit_response = wait_for_village_certification(
            server, x, z, config["timeout_seconds"])
        report = {
            "biome": biome,
            "structure": structure,
            "x": x,
            "z": z,
            "locate": locate_response,
            "walls": wall_response,
            "audit": audit_response,
            "rejected_candidates": rejected_candidates,
        }
        reports.append(report)
        (capture_dir / f"{stage}-{biome}.txt").write_text(
            f"structure={structure}\ncoordinates={x},80,{z}\n"
            f"locate={locate_response}\nrejected_candidates={rejected_candidates}\n"
            f"walls={wall_response}\naudit={audit_response}\n",
            encoding="utf-8",
        )
        command_must_succeed(server, "forceload remove all")
    return reports


def run_village_certification(manifest: dict, startup_timeout: int) -> None:
    work_dir = RUNTIME_ROOT / "village-certification"
    rcon_port, password = prepare_runtime_server(
        work_dir, reset=True, generate_structures=True, spawn_npcs=True)
    config = manifest["village_certification"]
    captures = work_dir / "captures"
    summary: dict = {"status": "failed", "stages": []}

    first = ServerProcess(work_dir, "fresh", rcon_port, password, startup_timeout)
    first_started = False
    first_stopped = False
    try:
        first.start()
        first_started = True
        fresh_reports = certify_villages(first, config, True, captures, "fresh")
        command_must_succeed(first, "save-all flush", "saved")
        first.graceful_stop()
        first_stopped = True
        snapshot_minecraft_log(work_dir, "fresh")
        assert_no_wall_suffocation(work_dir, "fresh")
        summary["fresh"] = fresh_reports
        summary["stages"].append("five_fresh_villages")
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not first_stopped and first.process is not None:
            if first_started:
                try:
                    first.graceful_stop()
                    snapshot_minecraft_log(work_dir, "fresh")
                except Exception:
                    first.force_stop()
            else:
                first.force_stop()
        if "error" in summary:
            write_summary(work_dir, summary)

    second_port, second_password = prepare_runtime_server(
        work_dir, reset=False, generate_structures=True, spawn_npcs=True)
    second = ServerProcess(work_dir, "restart", second_port, second_password, startup_timeout)
    second_started = False
    second_stopped = False
    try:
        second.start()
        second_started = True
        restart_reports = certify_villages(
            second, config, False, captures, "restart", summary["fresh"])
        command_must_succeed(second, "save-all flush", "saved")
        second.graceful_stop()
        second_stopped = True
        snapshot_minecraft_log(work_dir, "restart")
        assert_no_wall_suffocation(work_dir, "restart")
        summary["restart"] = restart_reports
        summary["stages"].append("restart_recertified")
        summary["status"] = "passed"
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not second_stopped and second.process is not None:
            if second_started:
                try:
                    second.graceful_stop()
                    snapshot_minecraft_log(work_dir, "restart")
                except Exception as stop_error:
                    summary["shutdown_error"] = str(stop_error)
                    second.force_stop()
            else:
                second.force_stop()
        write_summary(work_dir, summary)


def locate_wild_structure(server: ServerProcess, structure: str, dimension: str,
                          origin_x: int = 0, origin_z: int = 0) -> tuple[int, int, str]:
    response = server.command(
        f"execute in {dimension} positioned {origin_x} 80 {origin_z} "
        f"run locate structure {structure}",
        timeout=240,
    )
    match = VILLAGE_LOCATE_PATTERN.search(response)
    if match is None:
        raise HarnessError(f"Could not parse wild structure location for {structure}: {response}")
    return int(match.group(1)), int(match.group(2)), response


def load_wild_structure_area(server: ServerProcess, dimension: str,
                             x: int, z: int, radius: int,
                             timeout_seconds: int) -> None:
    command_must_succeed(
        server,
        f"execute in {dimension} run forceload add "
        f"{x - radius} {z - radius} {x + radius} {z + radius}",
        timeout=timeout_seconds,
    )


def remove_dimension_forceloads(server: ServerProcess, dimension: str) -> None:
    command_must_succeed(server, f"execute in {dimension} run forceload remove all")


def wait_for_wild_structure_certification(server: ServerProcess, structure: str,
                                           dimension: str, x: int, z: int,
                                           timeout_seconds: int) -> str:
    deadline = time.monotonic() + timeout_seconds
    last_response = ""
    while time.monotonic() < deadline:
        last_response = server.command(
            f"execute in {dimension} positioned {x} 80 {z} "
            f"run witchery verify wild_structure {structure}")
        print(
            f"[runtime-test] Wild structure audit for {structure} at {x}, {z}: "
            f"{last_response}",
            flush=True,
        )
        if "[WildStructureCertification] PASS" in last_response:
            return last_response
        time.sleep(3)
    raise HarnessError(
        f"Wild structure certification timed out for {structure} at {x}, {z}; "
        f"last response: {last_response}")


def parse_wild_anchor(response: str, structure: str) -> tuple[int, int, int]:
    match = WILD_ANCHOR_PATTERN.search(response)
    if match is None:
        raise HarnessError(f"Could not parse certified anchor for {structure}: {response}")
    return int(match.group(1)), int(match.group(2)), int(match.group(3))


def relocate_vanishing_hut(server: ServerProcess, config: dict, x: int, z: int) -> tuple[int, int, int, str]:
    structure = "witchery:vanishing_hut"
    dimension = "minecraft:overworld"
    response = command_must_succeed(
        server,
        f"execute in {dimension} positioned {x} 80 {z} "
        "run witchery debug wild_structure relocate_vanishing_hut",
        "queued same-dimension",
    )
    print(f"[runtime-test] Vanishing Hut relocation: {response}", flush=True)
    deadline = time.monotonic() + config["relocation_timeout_seconds"]
    last_audit = ""
    while time.monotonic() < deadline:
        last_audit = server.command(
            f"execute in {dimension} positioned {x} 80 {z} "
            f"run witchery verify wild_structure {structure}")
        if "[WildStructureCertification] PASS" in last_audit:
            anchor_x, anchor_y, anchor_z = parse_wild_anchor(last_audit, structure)
            if (anchor_x, anchor_z) != (x, z):
                return anchor_x, anchor_y, anchor_z, last_audit
        time.sleep(2)
    raise HarnessError(
        f"Vanishing Hut did not complete a certified relocation from {x}, {z}; "
        f"last audit: {last_audit}")


def assert_no_wild_structure_errors(work_dir: Path, label: str) -> None:
    log_path = work_dir / f"minecraft-{label}.log"
    if not log_path.is_file():
        raise HarnessError(f"Wild structure certification log is missing: {log_path}")
    text = log_path.read_text(encoding="utf-8", errors="replace")
    failures = (
        "Could not place template witchery:",
        "Unknown template marker",
        "suffocated in a wall",
    )
    found = [failure for failure in failures if failure in text]
    if found:
        raise HarnessError(f"Wild structure log contains generation errors {found}: {log_path}")


def certify_wild_structures(server: ServerProcess, config: dict, capture_dir: Path,
                            stage: str, known: list[dict] | None = None) -> list[dict]:
    reports: list[dict] = []
    capture_dir.mkdir(parents=True, exist_ok=True)
    for index, entry in enumerate(config["structures"]):
        name = entry["name"]
        structure = entry["structure"]
        dimension = entry["dimension"]
        if known is None:
            x, z, locate_response = locate_wild_structure(server, structure, dimension)
        else:
            prior = known[index]
            if prior["name"] != name or prior["structure"] != structure \
                    or prior["dimension"] != dimension:
                raise HarnessError("Wild structure restart order drifted")
            x = prior["restart_x"]
            z = prior["restart_z"]
            locate_response = "reused certified fresh-world coordinates"

        load_wild_structure_area(
            server, dimension, x, z, config["force_radius_blocks"],
            config["timeout_seconds"])
        audit = wait_for_wild_structure_certification(
            server, structure, dimension, x, z, config["timeout_seconds"])
        anchor_x, anchor_y, anchor_z = parse_wild_anchor(audit, structure)
        report = {
            "name": name,
            "structure": structure,
            "dimension": dimension,
            "x": x,
            "z": z,
            "anchor": [anchor_x, anchor_y, anchor_z],
            "locate": locate_response,
            "audit": audit,
            "restart_x": anchor_x,
            "restart_z": anchor_z,
        }

        if known is None and structure == "witchery:vanishing_hut":
            moved_x, moved_y, moved_z, moved_audit = relocate_vanishing_hut(
                server, config, anchor_x, anchor_z)
            report["relocated_from"] = [anchor_x, anchor_y, anchor_z]
            report["relocated_to"] = [moved_x, moved_y, moved_z]
            report["relocated_audit"] = moved_audit
            report["restart_x"] = moved_x
            report["restart_z"] = moved_z

        reports.append(report)
        (capture_dir / f"{stage}-{name}.txt").write_text(
            f"structure={structure}\ndimension={dimension}\ncoordinates={x},80,{z}\n"
            f"locate={locate_response}\naudit={audit}\n"
            f"relocated_from={report.get('relocated_from')}\n"
            f"relocated_to={report.get('relocated_to')}\n"
            f"relocated_audit={report.get('relocated_audit')}\n",
            encoding="utf-8",
        )
        remove_dimension_forceloads(server, dimension)
    return reports


def run_wild_structure_certification(manifest: dict, startup_timeout: int) -> None:
    work_dir = RUNTIME_ROOT / "wild-structure-certification"
    rcon_port, password = prepare_runtime_server(
        work_dir, reset=True, generate_structures=True, spawn_npcs=True)
    config = manifest["wild_structure_certification"]
    captures = work_dir / "captures"
    summary: dict = {"status": "failed", "stages": []}

    first = ServerProcess(work_dir, "fresh", rcon_port, password, startup_timeout)
    first_started = False
    first_stopped = False
    try:
        first.start()
        first_started = True
        fresh_reports = certify_wild_structures(first, config, captures, "fresh")
        command_must_succeed(first, "save-all flush", "saved")
        first.graceful_stop()
        first_stopped = True
        snapshot_minecraft_log(work_dir, "fresh")
        assert_no_wild_structure_errors(work_dir, "fresh")
        summary["fresh"] = fresh_reports
        summary["stages"].extend(["six_fresh_structures", "vanishing_hut_relocated"])
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not first_stopped and first.process is not None:
            if first_started:
                try:
                    first.graceful_stop()
                    snapshot_minecraft_log(work_dir, "fresh")
                except Exception:
                    first.force_stop()
            else:
                first.force_stop()
        if "error" in summary:
            write_summary(work_dir, summary)

    second_port, second_password = prepare_runtime_server(
        work_dir, reset=False, generate_structures=True, spawn_npcs=True)
    second = ServerProcess(work_dir, "restart", second_port, second_password, startup_timeout)
    second_started = False
    second_stopped = False
    try:
        second.start()
        second_started = True
        restart_reports = certify_wild_structures(
            second, config, captures, "restart", summary["fresh"])
        command_must_succeed(second, "save-all flush", "saved")
        second.graceful_stop()
        second_stopped = True
        snapshot_minecraft_log(work_dir, "restart")
        assert_no_wild_structure_errors(work_dir, "restart")
        summary["restart"] = restart_reports
        summary["stages"].append("restart_recertified")
        summary["status"] = "passed"
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not second_stopped and second.process is not None:
            if second_started:
                try:
                    second.graceful_stop()
                    snapshot_minecraft_log(work_dir, "restart")
                except Exception as stop_error:
                    summary["shutdown_error"] = str(stop_error)
                    second.force_stop()
            else:
                second.force_stop()
        write_summary(work_dir, summary)


def wait_for_witch_well_certification(server: ServerProcess, variant: str,
                                       x: int, z: int, timeout_seconds: int) -> str:
    deadline = time.monotonic() + timeout_seconds
    last_response = ""
    while time.monotonic() < deadline:
        last_response = server.command(
            f"execute in minecraft:overworld positioned {x} 80 {z} "
            f"run witchery verify well {variant}")
        print(
            f"[runtime-test] Witch Well audit for {variant} at {x}, {z}: {last_response}",
            flush=True,
        )
        if "[WitchWellCertification] PASS" in last_response:
            return last_response
        time.sleep(3)
    raise HarnessError(
        f"Witch Well certification timed out for {variant} at {x}, {z}; "
        f"last response: {last_response}")


def parse_well_variant(response: str) -> str:
    match = WELL_VARIANT_PATTERN.search(response)
    if match is None:
        raise HarnessError(f"Could not parse Witch Well variant: {response}")
    return match.group(1)


class JavaRandom:
    MASK = (1 << 48) - 1
    MULTIPLIER = 0x5DEECE66D
    ADDEND = 0xB

    def __init__(self, seed: int):
        self.set_seed(seed)

    def set_seed(self, seed: int) -> None:
        self.seed = (seed ^ self.MULTIPLIER) & self.MASK

    def next_bits(self, bits: int) -> int:
        self.seed = (self.seed * self.MULTIPLIER + self.ADDEND) & self.MASK
        return self.seed >> (48 - bits)

    def next_int(self, bound: int) -> int:
        if bound <= 0:
            raise ValueError("bound must be positive")
        if bound & (bound - 1) == 0:
            return (bound * self.next_bits(31)) >> 31
        while True:
            bits = self.next_bits(31)
            value = bits % bound
            if bits - value + bound - 1 < (1 << 31):
                return value

    def next_long(self) -> int:
        high = self.next_bits(32)
        low = self.next_bits(32)
        if high >= 1 << 31:
            high -= 1 << 32
        if low >= 1 << 31:
            low -= 1 << 32
        return (high << 32) + low


def witch_well_candidate(config: dict, region_x: int, region_z: int) -> tuple[int, int, int]:
    spread = config["spacing"] - config["separation"]
    placement_seed = (region_x * 341873128712 + region_z * 132897987541
                      + config["level_seed"] + config["salt"])
    placement_random = JavaRandom(placement_seed)
    chunk_x = region_x * config["spacing"] + placement_random.next_int(spread)
    chunk_z = region_z * config["spacing"] + placement_random.next_int(spread)

    feature_random = JavaRandom(config["level_seed"])
    x_multiplier = feature_random.next_long()
    z_multiplier = feature_random.next_long()
    feature_random.set_seed(chunk_x * x_multiplier ^ chunk_z * z_multiplier
                            ^ config["level_seed"])
    return chunk_x, chunk_z, feature_random.next_int(10)


def spiral_regions(max_ring: int = 512) -> Iterable[tuple[int, int]]:
    yield 0, 0
    for ring in range(1, max_ring + 1):
        for x in range(-ring, ring + 1):
            yield x, -ring
            yield x, ring
        for z in range(-ring + 1, ring):
            yield -ring, z
            yield ring, z


def candidate_matches_variant(roll: int, variant: str) -> bool:
    return roll == 0 if variant == "cave" else 1 <= roll <= 5 if variant == "small" else roll >= 6


def probe_witch_well_candidate(server: ServerProcess, x: int, z: int) -> str | None:
    last_response = ""
    for _ in range(2):
        last_response = server.command(
            f"execute in minecraft:overworld positioned {x} 80 {z} "
            "run witchery verify well any")
        if WELL_VARIANT_PATTERN.search(last_response) is not None:
            return last_response
        if "failures=no_loaded_start" not in last_response:
            raise HarnessError(
                f"Natural Witch Well candidate at {x}, {z} failed certification: {last_response}")
        time.sleep(0.5)
    return None


def certify_fresh_witch_wells(server: ServerProcess, config: dict,
                              capture_dir: Path) -> tuple[list[dict], list[dict]]:
    found: dict[str, dict] = {}
    probes: list[dict] = []
    variants = config["variants"]
    capture_dir.mkdir(parents=True, exist_ok=True)

    index = 0
    for wanted in ("cave", "regular", "small"):
        variant_probes = 0
        for region_x, region_z in spiral_regions():
            if wanted in found:
                break
            chunk_x, chunk_z, roll = witch_well_candidate(config, region_x, region_z)
            if not candidate_matches_variant(roll, wanted):
                continue
            if variant_probes >= config["max_candidate_wells"]:
                break
            variant_probes += 1
            index += 1
            x = chunk_x * 16 + 8
            z = chunk_z * 16 + 8
            load_wild_structure_area(
                server, "minecraft:overworld", x, z, 0, config["timeout_seconds"])
            try:
                initial_audit = probe_witch_well_candidate(server, x, z)
                probe = {"index": index - 1, "region": [region_x, region_z],
                         "chunk": [chunk_x, chunk_z], "roll": roll,
                         "location": [x, z], "generated": initial_audit is not None}
                probes.append(probe)
                if initial_audit is None:
                    continue
                variant = parse_well_variant(initial_audit)
                load_wild_structure_area(
                    server, "minecraft:overworld", x, z, config["force_radius_blocks"],
                    config["timeout_seconds"])
                audit = wait_for_witch_well_certification(
                    server, variant, x, z, config["timeout_seconds"])
                anchor = parse_wild_anchor(audit, f"witch_well/{variant}")
                probe.update({"variant": variant, "anchor": list(anchor), "audit": audit})
                if variant not in found:
                    found[variant] = probe
                    (capture_dir / f"fresh-{variant}.txt").write_text(
                        f"variant={variant}\nregion={region_x},{region_z}\n"
                        f"chunk={chunk_x},{chunk_z}\nroll={roll}\nanchor={anchor}\n"
                        f"audit={audit}\n",
                        encoding="utf-8",
                    )
            finally:
                remove_dimension_forceloads(server, "minecraft:overworld")
        if wanted not in found:
            raise HarnessError(
                f"Fixed-seed Witch Well search did not naturally generate variant {wanted} "
                f"after {variant_probes} deterministic probes")

    missing = [variant for variant in variants if variant not in found]
    if missing:
        raise HarnessError(
            f"Fixed-seed Witch Well search did not naturally generate variants {missing} "
            f"after up to {config['max_candidate_wells']} probes per variant")
    return [found[variant] for variant in variants], probes


def recertify_witch_wells(server: ServerProcess, config: dict,
                          capture_dir: Path, known: list[dict]) -> list[dict]:
    reports: list[dict] = []
    for prior in known:
        variant = prior["variant"]
        anchor_x, anchor_y, anchor_z = prior["anchor"]
        load_wild_structure_area(
            server, "minecraft:overworld", anchor_x, anchor_z,
            config["force_radius_blocks"], config["timeout_seconds"])
        try:
            audit = wait_for_witch_well_certification(
                server, variant, anchor_x, anchor_z, config["timeout_seconds"])
            anchor = parse_wild_anchor(audit, f"witch_well/{variant}")
            if anchor != (anchor_x, anchor_y, anchor_z):
                raise HarnessError(
                    f"Witch Well {variant} anchor drifted across restart: "
                    f"{(anchor_x, anchor_y, anchor_z)} -> {anchor}")
            report = {"variant": variant, "anchor": list(anchor), "audit": audit}
            reports.append(report)
            (capture_dir / f"restart-{variant}.txt").write_text(
                f"variant={variant}\nanchor={anchor}\naudit={audit}\n",
                encoding="utf-8",
            )
        finally:
            remove_dimension_forceloads(server, "minecraft:overworld")
    return reports


def assert_no_witch_well_errors(work_dir: Path, label: str) -> None:
    log_path = work_dir / f"minecraft-{label}.log"
    if not log_path.is_file():
        raise HarnessError(f"Witch Well certification log is missing: {log_path}")
    text = log_path.read_text(encoding="utf-8", errors="replace")
    failures = (
        "Could not place template witchery:witch_well",
        "Unknown template marker",
        "suffocated in a wall",
    )
    found = [failure for failure in failures if failure in text]
    if found:
        raise HarnessError(f"Witch Well log contains generation errors {found}: {log_path}")


def run_witch_well_certification(manifest: dict, startup_timeout: int) -> None:
    work_dir = RUNTIME_ROOT / "witch-well-certification"
    rcon_port, password = prepare_runtime_server(
        work_dir, reset=True, generate_structures=True, spawn_npcs=True)
    config = manifest["witch_well_certification"]
    captures = work_dir / "captures"
    summary: dict = {"status": "failed", "seed": "witchery-runtime-test", "stages": []}

    first = ServerProcess(work_dir, "fresh", rcon_port, password, startup_timeout)
    first_started = False
    first_stopped = False
    try:
        first.start()
        first_started = True
        fresh, probes = certify_fresh_witch_wells(first, config, captures)
        command_must_succeed(first, "save-all flush", "saved")
        first.graceful_stop()
        first_stopped = True
        snapshot_minecraft_log(work_dir, "fresh")
        assert_no_witch_well_errors(work_dir, "fresh")
        summary["fresh"] = fresh
        summary["probes"] = probes
        summary["stages"].append("three_natural_variants")
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not first_stopped and first.process is not None:
            if first_started:
                try:
                    first.graceful_stop()
                    snapshot_minecraft_log(work_dir, "fresh")
                except Exception:
                    first.force_stop()
            else:
                first.force_stop()
        if "error" in summary:
            write_summary(work_dir, summary)

    second_port, second_password = prepare_runtime_server(
        work_dir, reset=False, generate_structures=True, spawn_npcs=True)
    second = ServerProcess(work_dir, "restart", second_port, second_password, startup_timeout)
    second_started = False
    second_stopped = False
    try:
        second.start()
        second_started = True
        restart = recertify_witch_wells(second, config, captures, summary["fresh"])
        command_must_succeed(second, "save-all flush", "saved")
        second.graceful_stop()
        second_stopped = True
        snapshot_minecraft_log(work_dir, "restart")
        assert_no_witch_well_errors(work_dir, "restart")
        summary["restart"] = restart
        summary["stages"].append("restart_recertified")
        summary["status"] = "passed"
    except Exception as exc:
        summary["error"] = str(exc)
        raise
    finally:
        if not second_stopped and second.process is not None:
            if second_started:
                try:
                    second.graceful_stop()
                    snapshot_minecraft_log(work_dir, "restart")
                except Exception as stop_error:
                    summary["shutdown_error"] = str(stop_error)
                    second.force_stop()
            else:
                second.force_stop()
        write_summary(work_dir, summary)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "command",
        choices=("validate", "resource-contracts", "game-tests", "save-reload",
                 "blight-heart-restart", "village-certification",
                 "wild-structure-certification", "witch-well-certification", "all"),
    )
    parser.add_argument("--startup-timeout", type=int, default=600)
    parser.add_argument(
        "--game-test",
        action="append",
        dest="game_tests",
        help="Run only this exact CI GameTest id; may be repeated.",
    )
    args = parser.parse_args()

    try:
        manifest = load_manifest()
        validate_manifest(manifest)
        if args.game_tests and args.command != "game-tests":
            raise HarnessError("--game-test is only valid with the game-tests command")
        ci_count = sum(1 for entry in manifest["game_tests"] if entry["lane"] == "ci")
        manual_count = sum(1 for entry in manifest["game_tests"] if entry["lane"] != "ci")
        manual_label = "GameTest" if manual_count == 1 else "GameTests"
        print(
            f"[runtime-test] Manifest valid: {len(manifest['resource_contract_tests'])} resource tests, "
            f"{ci_count} CI GameTests, {manual_count} manual restart {manual_label}",
            flush=True,
        )
        if args.command == "validate":
            return 0
        if args.command in {"resource-contracts", "all"}:
            run_resource_contracts(manifest)
        if args.command in {"game-tests", "all"}:
            run_game_tests(manifest, args.startup_timeout,
                           set(args.game_tests) if args.game_tests else None)
        if args.command in {"save-reload", "all"}:
            run_save_reload(manifest, args.startup_timeout)
        if args.command == "blight-heart-restart":
            run_blight_heart_restart(manifest, args.startup_timeout)
        if args.command in {"village-certification", "all"}:
            run_village_certification(manifest, args.startup_timeout)
        if args.command in {"wild-structure-certification", "all"}:
            run_wild_structure_certification(manifest, args.startup_timeout)
        if args.command in {"witch-well-certification", "all"}:
            run_witch_well_certification(manifest, args.startup_timeout)
        return 0
    except (HarnessError, OSError, subprocess.SubprocessError) as exc:
        print(f"[runtime-test] ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
