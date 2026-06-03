# In-Game QA Checklist — 2026-06-03

Ten specific things to verify in-game. Everything below has only been verified headlessly
(compile + unit tests); the in-world feel is unconfirmed. Most are this session's new work.

## Crystal Ball predictions (the original "no arrows" bug)

1. **Arrow prophecy actually fires.** Use a working Crystal Ball until you get
   "…will feel the sting of an arrow." Wait ~1–1.5 minutes without taking real arrow
   damage. **Expect:** a Skeleton spawns within a few blocks (witch particles + a pop
   sound) and starts shooting you. (Before the fix this was flavor text only.)

2. **Creative-mode bypass.** In creative with no altar set up, right-click the Crystal Ball.
   **Expect:** it works immediately (no cooldown, no "needs 500 power" rejection) and gives a
   prophecy.

3. **New prophecy — The Sky's Wrath.** Scry until "the heavens have marked you — a bolt will
   seek their crown." Wait. **Expect:** a lightning bolt cracks down ~3 blocks away (near, not
   on you), igniting grass.

4. **New prophecy — Flames at Your Heels.** Scry until the flames prophecy, wait.
   **Expect:** a ring of fire flares up in the 8 cells *around* you (not under you).

5. **New prophecy — The Serpent's Kiss.** Scry until the venom prophecy, wait.
   **Expect:** a cave spider is sent to bite you (or, if you get Poison by any means first, the
   prophecy clears naturally).

## Hellhound (new entity)

6. **Hellhound behaviour.** Spawn one with the Hellhound egg. **Expect:** stands in fire/lava
   unharmed; trails flame particles; sets you alight when it bites; on death drops Tongue of Dog
   and occasionally a Hellhound Head (the trophy block now has a source). Hoglin-style growls,
   wolf footsteps.

7. **Hellhound → Wolf cure.** Hit the hellhound with a Splash Potion of Weakness, then
   right-click it with a **plain** Golden Apple. **Expect:** after a short delay (faster if near
   iron bars/beds) it converts into a dazed vanilla Wolf.

## Mindrake (new crop + mob)

8. **Bulb hatch + taming.** Drop (press Q) a Mindrake Bulb on the ground. **Expect:** after
   ~3 seconds it hatches (poof particle) into a **tamed** Mindrake that follows you. Drop a stack
   → one Mindrake per bulb.

9. **Detonation + crop.** Let a tamed Mindrake reach an enemy (or an untamed one reach you).
   **Expect:** it **explodes on contact** and leaves a flower on grass/dirt; killing one instead
   makes it burst (smaller) and drop Mandrake Seeds. Separately: plant a bulb on farmland,
   bonemeal it through its 5 stages, harvest — **expect** bulbs, and (unlike Mandrake) **no** mob
   rises from the crop.

## Dark Mark (new entity + spell)

10. **Morsmordre → Dark Mark.** With the Mystic Branch, draw the **Morsmordre** glyph
    (stroke sequence begins up-up-right-down-down). **Expect:** a projectile flies from your hand
    and, on impact, conjures a large floating skull-cloud that pours smoke, cackles
    (`witch.celebrate`), and shoves nearby **mobs** (not players) outward within ~10 blocks. It
    should vanish after ~5 minutes. (Also testable directly via the Dark Mark spawn egg.)

---

*Report results back per item number; failures with the on-screen message / what you saw are
most useful.*
