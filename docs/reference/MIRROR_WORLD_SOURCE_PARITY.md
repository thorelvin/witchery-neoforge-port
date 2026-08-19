# Mirror World source parity

Audited against the decompiled Witchery 0.24.1 implementation in
`decompiled/witchery-1.7.10-0.24.1`. This contract targets fresh saves and newly
generated Mirror World chunks. Legacy remake room records are deliberately not
migrated yet.

## Authoritative source

- `dimension/WorldChunkManagerMirror.java`: 256-block repeating room lattice.
- `dimension/WorldProviderMirror.java`: fixed darkness, fog, weather, and spawn rules.
- `blocks/BlockMirror.java`: room allocation, traversal, carried links, escape chants,
  collision bounds, and room-clearing rules.
- `entity/EntityReflection.java`: boss attributes, copied player state, combat, damage
  cap, lifetime, and death callback.
- `entity/EntityFollower.java` plus the type-5 grenade branch: passive ten-second
  player decoy behavior.
- `item/ItemMirror.java`: preservation of a linked mirror's block-entity data.

## Fresh-world contract

| Surface | Source behavior | Port status |
|---|---|---|
| Geometry | Height 256; local X/Z 4-12 and local Y 6-14 form a 9 x 9 x 9 chamber every 16 blocks. The source leaves Y 255 as air. | Matched. |
| Allocation | Scan 15 vertical tiers at one X/Z cell, then follow the source outward spiral. | Matched deterministically through `MirrorWorldLayout`. |
| Inner mirror | Top at local `(4, 8, 8)`, lower half at Y 7, linked to the exact source dimension and block. | Matched. |
| Source links | A source mirror owns one room. Breaking it disconnects the inner mirror; a matching item carried by a player outside Mirror World becomes the exit target, and replacing it reconnects the room. | Matched with modern item components and SavedData. |
| Cross-dimension travel | Players alone cross between the source dimension and Mirror World. The source mirror may be outside the Overworld. | Matched. |
| Local mirror travel | Living entities and dropped items can use same-dimension bridges. Opposite-facing mirrors bridge horizontally within 31 blocks. Cleared mirrors outside Mirror World also bridge vertically to same-facing mirrors. | Matched. |
| Mirror World ordinary mirror | Try the horizontal bridge first; otherwise spend 3,000 altar power to move 1-9 blocks forward without entering another occupied inner-mirror cell. It never allocates a deeper room. | Matched. |
| Reflection | 100 health, boss bar, 0.35 speed, 50 follow range, full knockback resistance, 2 base damage, 6-damage incoming cap, fire immunity, and 50 XP. It copies the nearest player's strongest hotbar weapon, armor, effects, and relevant transformation state. | Matched, including ranged weapon switching and source-shaped anti-flight behavior. |
| Room clearing | A room remains inhabited while any Reflection in its bounds lives. Every true Reflection drops one Demon Heart; copied equipment never drops. | Matched. A surviving free Reflection inherits the persisted guardian record. |
| Duplication Grenade | A passive type-5 follower copy lasts ten seconds and distracts hostile mobs. It is not a combat Reflection and has no loot or room authority. | Matched by the dedicated `MirrorDecoyEntity`. |
| Escape speech | `mirror mirror send me home` returns to the recorded inner entry with a five-minute cooldown. `mirror mirror i give up` returns to a valid bed or Overworld spawn with a sixty-minute cooldown. | Matched. |
| Atmosphere | No weather or natural spawning, fixed darkness/celestial state, and fog `(0, 0.03, 0.1)`. | Matched; final visual feel remains real-client QA. |
| Mirror bounds | Collision depth 0.15 blocks and selection depth 0.32 blocks. | Matched. |

## Modern persistence choices

The original discovers free rooms by scanning blocks and stores raw dimension coordinates
in tile NBT. The port records room ownership and guardian UUIDs in SavedData, then audits
them with `/witchery verify mirrorworld`. This is a modern persistence implementation of
the same observable fresh-world ownership rules, not a legacy-save compatibility layer.

## Separate mirror-interaction work

Two `BlockMirror` interaction branches are not part of the completed world lifecycle:

- Applying a player Taglock to a mirror to borrow that player's rendered skin.
- The exact favorite/fairest history and rare distant `EntityFollower` encounter.

The current translucent Mirror Face and optional AI dialogue are deliberate remake
extensions around ordinary mirror interaction. They do not allocate rooms, clear rooms,
drop Demon Hearts, or alter traversal. These branches must not be described as complete
source parity until implemented separately.

## Verification

- `./gradlew test --tests "*MirrorWorld*"` covers allocation order, repeating room
  geometry, the open Y-255 source quirk, persistence, and dimension resources.
- `/witchery verify mirrorworld` audits generated room geometry, links, guardian
  ownership, and cleared state in a live world.
- `/test run witchery:mirror_world_lifecycle` passed on a fresh dedicated world on
  2026-08-18. It covers two players and vertically allocated rooms, copied equipment,
  all-Reflection guardian succession, one heart per true Reflection, decoy isolation,
  a disconnected linked item carried into the Nether, reconnection, persisted return,
  cleared re-entry, and the final certificate.

The lifecycle fixture is manual-only in the registry because NeoForge's ephemeral
`runGameTestServer` creates only vanilla dimensions. Run it through a normal integrated
or dedicated server where `witchery:mirror_world` is loaded.
