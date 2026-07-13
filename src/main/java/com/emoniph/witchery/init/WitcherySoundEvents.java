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
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_IMP_LAUGH = register("mob.imp.laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_IMP_HIT = register("mob.imp.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_IMP_DEATH = register("mob.imp.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_BABA_LIVING = register("mob.baba.baba_living");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_BABA_DEATH = register("mob.baba.baba_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_OWL_HOOT = register("mob.owl.owl_hoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_OWL_HURT = register("mob.owl.owl_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_TOAD_CROAK = register("mob.toad.toad_croak");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_TOAD_HURT = register("mob.toad.toad_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_GOBLIN_IDLE = register("mob.goblin.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_GOBLIN_HAGGLE = register("mob.goblin.haggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_GOBLIN_HIT = register("mob.goblin.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_GOBLIN_YES = register("mob.goblin.yes");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_GOBLIN_NO = register("mob.goblin.no");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_GOBLIN_DEATH = register("mob.goblin.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_GOBLIN_GULG_IDLE = register("mob.goblin.gulg_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_GOBLIN_MOG_IDLE = register("mob.goblin.mog_idle");
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
