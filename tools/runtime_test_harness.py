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


def write_server_files(work_dir: Path, rcon_port: int, server_port: int, password: str) -> None:
    (work_dir / "eula.txt").write_text("eula=true\n", encoding="ascii")
    properties = {
        "allow-flight": "true",
        "difficulty": "normal",
        "enable-command-block": "false",
        "enable-query": "false",
        "enable-rcon": "true",
        "force-gamemode": "true",
        "gamemode": "creative",
        "generate-structures": "false",
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
        "spawn-npcs": "false",
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


def write_summary(work_dir: Path, summary: dict) -> None:
    (work_dir / "summary.json").write_text(
        json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def command_must_succeed(server: ServerProcess, command: str,
                         expected: str | None = None) -> str:
    response = server.command(command)
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


def prepare_runtime_server(work_dir: Path, reset: bool) -> tuple[int, str]:
    if reset:
        safe_reset_directory(work_dir)
    else:
        work_dir.mkdir(parents=True, exist_ok=True)
    rcon_port, server_port = find_free_ports(2)
    password = secrets.token_urlsafe(24)
    write_server_files(work_dir, rcon_port, server_port, password)
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
    summary: dict = {"status": "failed", "tests": []}
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


def run_save_reload(manifest: dict, startup_timeout: int) -> None:
    work_dir = RUNTIME_ROOT / "save-reload"
    rcon_port, password = prepare_runtime_server(work_dir, reset=True)
    config = manifest["save_reload"]
    machines = config["machines"]
    storage = config["storage"]
    marker = config["storage_marker"]
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
        assert_saved_machine_state(first, machines, storage, marker)
        command_must_succeed(first, "save-all flush", "saved")
        first.graceful_stop()
        first_stopped = True
        snapshot_minecraft_log(work_dir, "save")
        summary["stages"].append("saved")
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


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "command",
        choices=("validate", "resource-contracts", "game-tests", "save-reload", "all"),
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
        print(
            f"[runtime-test] Manifest valid: {len(manifest['resource_contract_tests'])} resource tests, "
            f"{ci_count} CI GameTests, {manual_count} manual restart GameTest",
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
        return 0
    except (HarnessError, OSError, subprocess.SubprocessError) as exc:
        print(f"[runtime-test] ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
