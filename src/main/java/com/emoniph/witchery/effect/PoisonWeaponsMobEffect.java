package com.emoniph.witchery.effect;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Source 1.7.10 Poison Weapons: melee attackers poison the entity they hit.
 */
public class PoisonWeaponsMobEffect extends MobEffect {
    public static final int SHORT_POISON_TICKS = 100;
    public static final int LONG_POISON_TICKS = 300;
    public static final int WITHER_TICKS = 400;

    public PoisonWeaponsMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    public static boolean isSourceMeleeDamage(DamageSource source) {
        return source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.PLAYER_ATTACK);
    }

    public static MobEffectInstance attackEffect(int amplifier) {
        return switch (amplifier) {
            case 0 -> new MobEffectInstance(MobEffects.POISON, SHORT_POISON_TICKS, 0);
            case 1 -> new MobEffectInstance(MobEffects.POISON, SHORT_POISON_TICKS, 1);
            case 2 -> new MobEffectInstance(MobEffects.POISON, LONG_POISON_TICKS, 1);
            default -> new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS, 0);
        };
    }
}
