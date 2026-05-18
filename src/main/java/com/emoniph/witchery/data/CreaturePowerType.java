package com.emoniph.witchery.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Active creature powers gained by sacrificing mobs near an altar. */
public enum CreaturePowerType {

    SPEED {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 3));
        }
        @Override public String displayName() { return "Speed"; }
    },

    JUMP {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 400, 3));
            player.fallDistance = 0f; // clear fall damage
        }
        @Override public String displayName() { return "Jump"; }
    },

    HEAL {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        }
        @Override public String displayName() { return "Heal"; }
    },

    ENDERMAN {
        @Override public void activate(Player player) {
            // Raycast up to 16 blocks in look direction, teleport to hit
            HitResult hit = player.pick(16.0, 1.0f, false);
            Vec3 dest = hit.getLocation().subtract(0, 1, 0); // land on top of hit block
            player.teleportTo(dest.x, dest.y, dest.z);
            player.playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1f, 1f);
        }
        @Override public String displayName() { return "Enderman"; }
    },

    BAT {
        @Override public void activate(Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 400, 0));
            player.fallDistance = 0f;
        }
        @Override public String displayName() { return "Bat"; }
    };

    public abstract void activate(Player player);
    public abstract String displayName();

    public static @Nullable CreaturePowerType fromString(String name) {
        for (CreaturePowerType t : values()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }
        return null;
    }
}
