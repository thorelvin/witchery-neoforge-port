package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Preserves the complete modern villager state through the Were-Villager/Wolfman cycle. */
public final class VillagerWerewolfConversion {
    static final String VILLAGER_SNAPSHOT_TAG = "VillagerOriginSnapshot";

    private VillagerWerewolfConversion() {
    }

    public static WereVillagerEntity convertVillager(ServerLevel level, Villager villager, boolean infectious) {
        WereVillagerEntity converted = WitcheryEntities.WERE_VILLAGER.get()
                .create(level, EntitySpawnReason.CONVERSION);
        if (converted == null) {
            return null;
        }
        loadSnapshot(converted, capture(villager), level);
        converted.setInfectious(infectious);
        converted.setPersistenceRequired();
        return replace(level, villager, converted) ? converted : null;
    }

    static WolfmanEntity convertToWolfman(ServerLevel level, WereVillagerEntity villager) {
        WolfmanEntity wolfman = WitcheryEntities.WOLFMAN.get().create(level, EntitySpawnReason.CONVERSION);
        if (wolfman == null) {
            return null;
        }
        wolfman.getPersistentData().put(VILLAGER_SNAPSHOT_TAG, capture(villager));
        if (villager.isInfectious()) {
            wolfman.setInfectious();
        }
        wolfman.setPersistenceRequired();
        copyPose(villager, wolfman);
        return replace(level, villager, wolfman) ? wolfman : null;
    }

    static WereVillagerEntity restoreWereVillager(ServerLevel level, WolfmanEntity wolfman) {
        CompoundTag snapshot = villagerSnapshot(wolfman);
        if (snapshot == null) {
            return null;
        }
        WereVillagerEntity restored = WitcheryEntities.WERE_VILLAGER.get()
                .create(level, EntitySpawnReason.CONVERSION);
        if (restored == null) {
            return null;
        }
        loadSnapshot(restored, snapshot, level);
        restored.setInfectious(wolfman.isInfectious());
        restored.setPersistenceRequired();
        copyPose(wolfman, restored);
        return replace(level, wolfman, restored) ? restored : null;
    }

    public static Villager cure(ServerLevel level, LivingEntity source) {
        CompoundTag snapshot;
        if (source instanceof WereVillagerEntity wereVillager) {
            snapshot = capture(wereVillager);
        } else if (source instanceof WolfmanEntity wolfman) {
            snapshot = villagerSnapshot(wolfman);
        } else {
            return null;
        }
        if (snapshot == null) {
            return null;
        }
        Villager restored = net.minecraft.world.entity.EntityType.VILLAGER
                .create(level, EntitySpawnReason.CONVERSION);
        if (restored == null) {
            return null;
        }
        loadSnapshot(restored, snapshot, level);
        restored.setPersistenceRequired();
        copyPose(source, restored);
        return replace(level, source, restored) ? restored : null;
    }

    static boolean hasVillagerSnapshot(WolfmanEntity wolfman) {
        return wolfman.getPersistentData().contains(VILLAGER_SNAPSHOT_TAG);
    }

    private static CompoundTag villagerSnapshot(WolfmanEntity wolfman) {
        if (!hasVillagerSnapshot(wolfman)) {
            return null;
        }
        return wolfman.getPersistentData().getCompoundOrEmpty(VILLAGER_SNAPSHOT_TAG).copy();
    }

    private static CompoundTag capture(LivingEntity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.saveWithoutId(output);
        CompoundTag snapshot = output.buildResult();
        snapshot.remove("UUID");
        snapshot.remove("UUIDMost");
        snapshot.remove("UUIDLeast");
        snapshot.remove("Passengers");
        snapshot.remove("Leash");
        return snapshot;
    }

    private static void loadSnapshot(LivingEntity entity, CompoundTag snapshot, ServerLevel level) {
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), snapshot.copy()));
    }

    private static void copyPose(LivingEntity source, LivingEntity target) {
        target.snapTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        target.setDeltaMovement(source.getDeltaMovement());
        target.setHealth(Math.min(source.getHealth(), target.getMaxHealth()));
    }

    private static boolean replace(ServerLevel level, LivingEntity source, LivingEntity target) {
        if (!level.addFreshEntity(target)) {
            return false;
        }
        source.discard();
        level.levelEvent(null, 1017, target.blockPosition(), 0);
        return true;
    }
}
