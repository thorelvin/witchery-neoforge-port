package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WitcherySoundEvents {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, WitcheryMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_BEAR_TRAP_CLICK = register("block.bear_trap.click");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_BEAR_TRAP_MANTRAP = register("block.bear_trap.mantrap");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_BANSHEE_SCREAM = register("entity.banshee.scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAVEN_IDLE_1 = register("entity.raven.idle1");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAVEN_IDLE_2 = register("entity.raven.idle2");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAVEN_IDLE_3 = register("entity.raven.idle3");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAVEN_IDLE_4 = register("entity.raven.idle4");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAVEN_IDLE_5 = register("entity.raven.idle5");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_EGASSY_FART = register("entity.egassy.fart");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_LEONARD_SAY = register("entity.leonard.say");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_LEONARD_HIT = register("entity.leonard.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENTITY_LEONARD_DEATH = register("entity.leonard.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_WOLFMAN_HOWL = register("mob.wolfman.howl");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_WOLFMAN_EAT = register("mob.wolfman.eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_WOLFMAN_LORD = register("mob.wolfman.lord");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_WOLFMAN_SAY = register("mob.wolfman.say");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_WOLFMAN_HIT = register("mob.wolfman.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_WOLFMAN_DEATH = register("mob.wolfman.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> RANDOM_HEARTBEAT = register("random.heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> RANDOM_HORN = register("random.horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> RANDOM_SWORD_DRAW = register("random.sworddraw");
    public static final DeferredHolder<SoundEvent, SoundEvent> RANDOM_SWORD_SHEATHE = register("random.swordsheathe");
    public static final DeferredHolder<SoundEvent, SoundEvent> RANDOM_WOOD_CREAK = register("random.wood_creak");

    private WitcherySoundEvents() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path);
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
