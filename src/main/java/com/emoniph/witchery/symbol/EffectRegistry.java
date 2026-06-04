package com.emoniph.witchery.symbol;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.DarkMarkEntity;
import com.emoniph.witchery.entity.SpellEffectEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.world.TormentHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of Mystic Branch symbol spells, keyed by drawn stroke sequence and by id.
 *
 * Source parity: {@code com.emoniph.witchery.infusion.infusions.symbols.EffectRegistry}
 * (1.7.10-0.24.1). Stroke sequences below are copied verbatim from source; this first port
 * slice registers the non-knowledge, non-unforgivable spells (no persistent spellbook or
 * cooldown is required for those). The iconic projectile and instant spells are covered.
 */
public final class EffectRegistry {

    private static final EffectRegistry INSTANCE = new EffectRegistry();

    private final Map<String, SymbolEffect> byStroke = new HashMap<>();
    private final Map<String, Integer> levelByStroke = new HashMap<>();
    private final Map<Integer, SymbolEffect> byId = new HashMap<>();
    private final List<SymbolEffect> all = new ArrayList<>();

    private EffectRegistry() {
    }

    public static EffectRegistry instance() {
        return INSTANCE;
    }

    /** Touch the class so all spell definitions are registered (called from common setup). */
    public static void bootstrap() {
        instance();
    }

    private static String key(byte[] strokes) {
        StringBuilder sb = new StringBuilder(strokes.length * 2);
        for (byte b : strokes) {
            sb.append((int) b).append(',');
        }
        return sb.toString();
    }

    private SymbolEffect add(SymbolEffect effect, StrokeSet... sets) {
        for (StrokeSet set : sets) {
            String k = key(set.strokes());
            byStroke.put(k, effect);
            levelByStroke.put(k, set.level());
        }
        byId.put(effect.getEffectId(), effect);
        effect.setDefaultStrokes(sets[0].strokes());
        all.add(effect);
        return effect;
    }

    public boolean contains(byte[] strokes) {
        return byStroke.containsKey(key(strokes));
    }

    public SymbolEffect getEffect(byte[] strokes) {
        return byStroke.get(key(strokes));
    }

    public int getLevel(byte[] strokes) {
        return levelByStroke.getOrDefault(key(strokes), 1);
    }

    public SymbolEffect getById(int id) {
        return byId.get(id);
    }

    public List<SymbolEffect> getEffects() {
        return all;
    }

    // ── Effect helpers ───────────────────────────────────────────────────────

    @FunctionalInterface
    private interface EntityAction {
        void act(ServerLevel level, LivingEntity caster, LivingEntity target);
    }

    private static void applyEntityEffect(ServerLevel level, LivingEntity caster, HitResult hit,
                                          Vec3 center, double radius, EntityAction action) {
        if (radius == 0.0) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity target) {
                action.act(level, caster, target);
            }
        } else {
            AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
                    center.x + radius, center.y + radius, center.z + radius);
            double r2 = radius * radius;
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (target.distanceToSqr(center) <= r2) {
                    action.act(level, caster, target);
                }
            }
        }
    }

    private static void disarm(ServerLevel level, LivingEntity target) {
        ItemStack held = target.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }
        if (target instanceof Player player) {
            player.drop(held.copy(), true);
        } else {
            target.spawnAtLocation(level, held.copy());
        }
        target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    private static BlockPos faceAdjacent(BlockHitResult bhr) {
        return bhr.getBlockPos().relative(bhr.getDirection());
    }

    // ── Spells ───────────────────────────────────────────────────────────────

    /** Accio — pull dropped items to the caster. */
    public static final SymbolEffect ACCIO = instance().add(
            new SymbolEffectProjectile(1, "witchery.pott.accio") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (caster == null) {
                        return;
                    }
                    int lvl = spell.getEffectLevel();
                    double r = lvl <= 1 ? 0.8 : (lvl == 2 ? 3.0 : 9.0);
                    AABB box = spell.getBoundingBox().inflate(r);
                    for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                        if (item.distanceToSqr(spell) <= r * r) {
                            item.setPos(caster.getX(), caster.getY() + 1.0, caster.getZ());
                            item.setDeltaMovement(Vec3.ZERO);
                        }
                    }
                }
            }.setColor(0x513726).setSize(1.0F),
            new StrokeSet(1, 3, 0, 2, 2, 1),
            new StrokeSet(1, 3, 0, 2, 2, 2, 1),
            new StrokeSet(2, 3, 0, 0, 2, 2, 1, 1),
            new StrokeSet(2, 3, 0, 0, 2, 2, 2, 1, 1),
            new StrokeSet(3, 3, 0, 0, 0, 2, 2, 2, 1, 1, 1),
            new StrokeSet(3, 3, 0, 0, 0, 2, 2, 2, 2, 1, 1, 1));

    /** Confundus — nausea on the target (and nearby at higher levels). */
    public static final SymbolEffect CONFUNDUS = instance().add(
            new SymbolEffectProjectile(8, "witchery.pott.confundus") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    int lvl = spell.getEffectLevel();
                    double radius = lvl <= 1 ? 0.0 : (lvl == 2 ? 2.0 : 4.0);
                    applyEntityEffect(level, caster, hit, spell.position(), radius, (l, c, t) -> {
                        if (!t.hasEffect(MobEffects.NAUSEA)) {
                            t.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 600));
                        }
                    });
                }
            }.setColor(0xFFE640).setSize(1.5F),
            new StrokeSet(1, 3, 3, 0, 0, 2),
            new StrokeSet(1, 3, 3, 3, 0, 0, 2, 2),
            new StrokeSet(2, 3, 3, 3, 0, 0, 0, 2, 2),
            new StrokeSet(3, 3, 3, 3, 3, 0, 0, 0, 0, 2, 2, 2));

    /** Episkey — heal the caster (and nearby allies at higher levels) by spending food. */
    public static final SymbolEffect EPISKEY = instance().add(
            new SymbolEffect(13, "witchery.pott.episkey", 1, false, false, null, 0) {
                @Override
                public void perform(ServerLevel level, ServerPlayer caster, int effectLevel) {
                    double radius = effectLevel <= 1 ? 0.0 : (effectLevel == 2 ? 2.0 : 4.0);
                    if (radius == 0.0) {
                        heal(level, caster);
                    } else {
                        AABB box = caster.getBoundingBox().inflate(radius);
                        double r2 = radius * radius;
                        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box)) {
                            if (target.distanceToSqr(caster) <= r2) {
                                heal(level, target);
                            }
                        }
                    }
                }

                private void heal(ServerLevel level, LivingEntity target) {
                    int food = target instanceof Player p ? p.getFoodData().getFoodLevel() : 5;
                    if (food > 1 && target.getHealth() < target.getMaxHealth()) {
                        int amount = Math.min(5, food);
                        target.heal(amount);
                        if (target instanceof Player p) {
                            p.getFoodData().setFoodLevel(food - amount);
                        }
                        if (!target.hasEffect(MobEffects.NAUSEA)) {
                            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80));
                        }
                        level.sendParticles(ParticleTypes.HEART, target.getX(), target.getEyeY(), target.getZ(),
                                6, 0.3, 0.3, 0.3, 0.0);
                    }
                }
            },
            new StrokeSet(1, 2, 0, 3, 1, 1, 2),
            new StrokeSet(2, 2, 0, 0, 3, 1, 1, 1, 1, 2),
            new StrokeSet(2, 2, 2, 0, 3, 3, 1, 1, 2, 2),
            new StrokeSet(3, 2, 2, 0, 0, 3, 3, 1, 1, 1, 1, 2, 2));

    /** Expelliarmus — knock the held item out of the target's hand. */
    public static final SymbolEffect EXPELLIARMUS = instance().add(
            new SymbolEffectProjectile(15, "witchery.pott.expelliarmus") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    int lvl = spell.getEffectLevel();
                    double radius = lvl <= 1 ? 0.0 : (lvl == 2 ? 3.0 : 5.0);
                    applyEntityEffect(level, caster, hit, spell.position(), radius, (l, c, t) -> {
                        if (t != c) {
                            disarm(l, t);
                        }
                    });
                }
            }.setColor(0xFF8042).setSize(3.0F),
            new StrokeSet(1, 0, 0, 1),
            new StrokeSet(1, 0, 0, 0, 1, 1),
            new StrokeSet(2, 0, 0, 0, 0, 1, 1, 1),
            new StrokeSet(3, 0, 0, 0, 0, 0, 1, 1, 1, 1));

    /** Flipendo — fling the target away along the spell's flight direction. */
    public static final SymbolEffect FLIPENDO = instance().add(
            new SymbolEffectProjectile(17, "witchery.pott.flipendo") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    int lvl = spell.getEffectLevel();
                    double radius = lvl <= 1 ? 0.0 : (lvl == 2 ? 3.0 : 6.0);
                    Vec3 mv = spell.getDeltaMovement();
                    double hx = mv.x;
                    double hz = mv.z;
                    double len = Math.sqrt(hx * hx + hz * hz);
                    if (len < 1.0E-4 && caster != null) {
                        Vec3 look = caster.getLookAngle();
                        hx = look.x;
                        hz = look.z;
                        len = Math.sqrt(hx * hx + hz * hz);
                    }
                    final double dirX = len > 1.0E-4 ? hx / len : 0.0;
                    final double dirZ = len > 1.0E-4 ? hz / len : 0.0;
                    applyEntityEffect(level, caster, hit, spell.position(), radius, (l, c, t) -> {
                        if (radius == 3.0 || t != c) {
                            t.setDeltaMovement(dirX * 1.6, 0.4, dirZ * 1.6);
                            t.hurtMarked = true;
                        }
                    });
                }
            }.setColor(0xFFFCD9).setSize(3.0F),
            new StrokeSet(1, 2, 2, 3),
            new StrokeSet(1, 2, 2, 2, 3, 3),
            new StrokeSet(2, 2, 2, 2, 2, 3, 3, 3),
            new StrokeSet(3, 2, 2, 2, 2, 2, 3, 3, 3, 3));

    /** Impedimenta — slow the target. */
    public static final SymbolEffect IMPEDIMENTA = instance().add(
            new SymbolEffectProjectile(19, "witchery.pott.impedimenta") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    int lvl = spell.getEffectLevel();
                    double radius = lvl <= 1 ? 0.0 : (lvl == 2 ? 3.0 : 6.0);
                    applyEntityEffect(level, caster, hit, spell.position(), radius, (l, c, t) -> {
                        if (t != c && !t.hasEffect(MobEffects.SLOWNESS)) {
                            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 1));
                        }
                    });
                }
            }.setColor(0x5E78FF).setSize(1.5F),
            new StrokeSet(1, 3, 3, 2),
            new StrokeSet(1, 3, 3, 3, 2, 2),
            new StrokeSet(2, 3, 3, 3, 3, 2, 2, 2),
            new StrokeSet(3, 3, 3, 3, 3, 3, 2, 2, 2, 2));

    /** Incendio — set the struck entity or block face alight. */
    public static final SymbolEffect INCENDIO = instance().add(
            new SymbolEffectProjectile(21, "witchery.pott.incendio") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    int lvl = spell.getEffectLevel();
                    double radius = lvl <= 1 ? 0.0 : (lvl == 2 ? 3.0 : 6.0);
                    if (radius == 0.0) {
                        if (hit instanceof EntityHitResult ehr) {
                            ehr.getEntity().igniteForSeconds(3.0F);
                        } else if (hit instanceof BlockHitResult bhr) {
                            placeFire(level, faceAdjacent(bhr));
                        }
                    } else {
                        applyEntityEffect(level, caster, hit, spell.position(), radius, (l, c, t) -> {
                            if (t != c) {
                                t.igniteForSeconds(lvl);
                            }
                        });
                        if (hit instanceof BlockHitResult bhr) {
                            placeFire(level, faceAdjacent(bhr));
                        }
                    }
                }

                private void placeFire(ServerLevel level, BlockPos pos) {
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                    }
                }
            }.setColor(0xFF4937).setSize(2.0F),
            new StrokeSet(1, 3, 0, 0, 1, 1),
            new StrokeSet(2, 3, 0, 0, 0, 1, 1, 1),
            new StrokeSet(3, 3, 0, 0, 0, 0, 1, 1, 1, 1));

    /** Lumos — set a Glow Globe against the struck block face. */
    public static final SymbolEffect LUMOS = instance().add(
            new SymbolEffectProjectile(22, "witchery.pott.lumos") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (hit instanceof BlockHitResult bhr) {
                        BlockPos pos = faceAdjacent(bhr);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir() || state.canBeReplaced()) {
                            level.setBlock(pos, WitcheryBlocks.GLOWGLOBE.get().defaultBlockState(), 3);
                        }
                    }
                }
            }.setColor(0xFFFFFA).setSize(0.5F),
            new StrokeSet(1, 1, 1, 1, 2));

    /** Nox — extinguish bright blocks (including Glow Globes) around the caster. */
    public static final SymbolEffect NOX = instance().add(
            new SymbolEffect(26, "witchery.pott.nox", 50, false, false, null, 0) {
                @Override
                public void perform(ServerLevel level, ServerPlayer caster, int effectLevel) {
                    BlockPos origin = caster.blockPosition();
                    int radius = 10;
                    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dx = -radius; dx <= radius; dx++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                                BlockState state = level.getBlockState(cursor);
                                if (state.getLightEmission() > 11 && state.getDestroySpeed(level, cursor) >= 0.0F) {
                                    BlockPos immutable = cursor.immutable();
                                    Block.dropResources(state, level, immutable);
                                    level.removeBlock(immutable, false);
                                }
                            }
                        }
                    }
                }
            },
            new StrokeSet(1, 0, 0, 2, 1, 2, 0));

    /** Stupefy — stun the struck creature (heavy slowness). */
    public static final SymbolEffect STUPEFY = instance().add(
            new SymbolEffectProjectile(36, "witchery.pott.stupefy", 5, false, true, null, 0) {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity target
                            && !target.hasEffect(MobEffects.SLOWNESS)) {
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 6000, 9));
                    }
                }
            }.setColor(0x0004FF).setSize(1.5F),
            new StrokeSet(1, 2, 2, 0, 3, 0, 2),
            new StrokeSet(1, 2, 2, 2, 0, 3, 3, 0, 2, 2),
            new StrokeSet(2, 2, 2, 0, 0, 3, 0, 0, 2),
            new StrokeSet(2, 2, 2, 2, 0, 0, 3, 3, 0, 0, 2, 2));

    /** Morsmordre — conjures a Dark Mark: a floating skull-cloud that wards mobs for 5 minutes. */
    public static final SymbolEffect MORSMORDRE = instance().add(
            new SymbolEffectProjectile(41, "witchery.pott.morsmordre", 20, false, false, null, 0) {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    DarkMarkEntity mark = WitcheryEntities.DARK_MARK.get().create(level, EntitySpawnReason.MOB_SUMMONED);
                    if (mark != null) {
                        mark.snapTo(spell.getX(), spell.getY(), spell.getZ(), 0.0F, 0.0F);
                        mark.setPersistenceRequired();
                        level.addFreshEntity(mark);
                    }
                }
            }.setColor(0x000000).setSize(3.0F).setTimeToLive(8),
            // Glyph: up, up, left, right, right. Source used StrokeSet's no-level constructor here,
            // so the leading stroke is part of the sequence — level 1 is prepended (as the port's
            // StrokeSet is always level-first, like every other spell above).
            new StrokeSet(1, 0, 0, 3, 2, 2),
            new StrokeSet(1, 0, 0, 3, 3, 2, 2, 2, 2),
            new StrokeSet(1, 0, 0, 0, 3, 2, 2),
            new StrokeSet(1, 0, 0, 0, 3, 3, 2, 2, 2, 2),
            new StrokeSet(1, 0, 0, 0, 0, 3, 2, 2),
            new StrokeSet(1, 0, 0, 0, 0, 3, 3, 2, 2, 2, 2));

    // ── Utility spells (Phase A) ─────────────────────────────────────────────

    /** Aguamenti — conjure a water source against the struck block face (steams away in the Nether). */
    public static final SymbolEffect AGUAMENTI = instance().add(
            new SymbolEffectProjectile(2, "witchery.pott.aguamenti") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (level.dimension() == net.minecraft.world.level.Level.NETHER && spell.getEffectLevel() < 3) {
                        return; // only the strongest casting holds water in the Nether
                    }
                    BlockPos pos = hit instanceof BlockHitResult bhr ? faceAdjacent(bhr)
                            : (hit instanceof EntityHitResult ehr ? ehr.getEntity().blockPosition() : null);
                    if (pos != null && level.getBlockState(pos).canBeReplaced()) {
                        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    }
                }
            }.setColor(0x4C7BFF).setSize(0.5F),
            new StrokeSet(1, 0, 0, 2, 2, 1),
            new StrokeSet(2, 0, 0, 0, 2, 2, 1, 1),
            new StrokeSet(3, 0, 0, 0, 0, 2, 2, 1, 1, 1));

    /** Alohomora — force a door open, even a locked Rowan door. */
    public static final SymbolEffect ALOHOMORA = instance().add(
            new SymbolEffectProjectile(3, "witchery.pott.alohomora") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (hit instanceof BlockHitResult bhr
                            && level.getBlockState(bhr.getBlockPos()).getBlock() instanceof DoorBlock door) {
                        BlockState state = level.getBlockState(bhr.getBlockPos());
                        if (!door.isOpen(state)) {
                            door.setOpen(caster, level, state, bhr.getBlockPos(), true);
                        }
                    }
                }
            }.setColor(0x513726).setSize(0.5F),
            new StrokeSet(1, 2, 0, 2, 2, 1));

    /** Defodio — dig out a soft block and drop it. */
    public static final SymbolEffect DEFODIO = instance().add(
            new SymbolEffectProjectile(10, "witchery.pott.defodio", 3, false, false, null, 0) {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (!(hit instanceof BlockHitResult bhr)) {
                        return;
                    }
                    int rad = spell.getEffectLevel() - 1; // 0 / 1 / 2 → single, 3×3×3, 5×5×5
                    BlockPos center = bhr.getBlockPos();
                    for (BlockPos p : BlockPos.betweenClosed(center.offset(-rad, -rad, -rad), center.offset(rad, rad, rad))) {
                        BlockState state = level.getBlockState(p);
                        if (isDiggable(state) && state.getDestroySpeed(level, p) >= 0.0F) {
                            BlockPos pos = p.immutable();
                            Block.dropResources(state, level, pos);
                            level.removeBlock(pos, false);
                        }
                    }
                }
            }.setColor(0x3D2A1C).setSize(2.5F),
            new StrokeSet(1, 0, 0, 3, 1),
            new StrokeSet(2, 0, 0, 0, 3, 3, 1, 1, 2),
            new StrokeSet(3, 0, 0, 0, 0, 3, 3, 3, 1, 1, 1, 2, 2));

    /** Ennervate — strip Slowness, Weakness and Nausea from the struck creature. */
    public static final SymbolEffect ENNERVATE = instance().add(
            new SymbolEffectProjectile(12, "witchery.pott.ennervate", 1, false, true, null, 0) {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    double radius = spell.getEffectLevel() <= 1 ? 0.0 : (spell.getEffectLevel() == 2 ? 2.0 : 4.0);
                    applyEntityEffect(level, caster, hit, spell.position(), radius, (l, c, t) -> {
                        t.removeEffect(MobEffects.SLOWNESS);
                        t.removeEffect(MobEffects.WEAKNESS);
                        t.removeEffect(MobEffects.NAUSEA);
                    });
                }
            }.setColor(0xFF18BB).setSize(1.5F),
            new StrokeSet(1, 0, 3, 0, 2, 3, 0, 2),
            new StrokeSet(2, 0, 3, 3, 0, 2, 2, 3, 3, 0, 2, 2),
            new StrokeSet(3, 0, 3, 3, 3, 0, 2, 2, 2, 3, 3, 3, 0, 2, 2, 2));

    /** Attraho — drag nearby creatures toward the caster. */
    public static final SymbolEffect ATTRAHO = instance().add(
            new SymbolEffectProjectile(47, "witchery.pott.attraho") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (caster == null) {
                        return;
                    }
                    int lvl = spell.getEffectLevel();
                    double r = lvl <= 1 ? 4.0 : (lvl == 2 ? 6.0 : 9.0);
                    AABB box = spell.getBoundingBox().inflate(r);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                        if (e == caster || e.distanceToSqr(spell) > r * r) {
                            continue;
                        }
                        Vec3 dir = caster.position().subtract(e.position());
                        if (dir.length() > 0.8) {
                            Vec3 v = dir.normalize().scale(0.55);
                            e.setDeltaMovement(v.x, v.y * 0.5 + 0.12, v.z);
                            e.hurtMarked = true;
                        }
                    }
                }
            }.setColor(0x513726).setSize(1.0F),
            new StrokeSet(1, 0, 0, 0, 2, 2, 1, 3));

    /** Cave Inimicum — fortify terrain, turning soft blocks to stone around the strike. */
    public static final SymbolEffect CAVE_INIMICUM = instance().add(
            new SymbolEffectProjectile(5, "witchery.pott.caveinimicum") {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (!(hit instanceof BlockHitResult bhr)) {
                        return;
                    }
                    int lvl = spell.getEffectLevel();
                    int rad = lvl <= 1 ? 1 : (lvl == 2 ? 2 : 3);
                    BlockPos center = bhr.getBlockPos();
                    for (BlockPos p : BlockPos.betweenClosed(center.offset(-rad, -rad, -rad), center.offset(rad, rad, rad))) {
                        BlockState hardened = fortify(level.getBlockState(p));
                        if (hardened != null) {
                            level.setBlock(p.immutable(), hardened, 3);
                        }
                    }
                }
            }.setColor(0x8A8A8A).setSize(2.0F),
            new StrokeSet(1, 0, 3, 0, 0, 2),
            new StrokeSet(2, 0, 3, 3, 0, 0, 0, 2, 2),
            new StrokeSet(3, 0, 3, 3, 3, 0, 0, 0, 0, 2, 2, 2));

    /** Meteolojinx Recanto — clear the rain (very costly). */
    public static final SymbolEffect METEOLOJINX_RECANTO = instance().add(
            new SymbolEffect(23, "witchery.pott.meteolojinxrecanto", 100, false, false, null, 0) {
                @Override
                public void perform(ServerLevel level, ServerPlayer caster, int effectLevel) {
                    if (level.isRaining() || level.isThundering()) {
                        level.setWeatherParameters(6000, 0, false, false);
                    }
                }
            },
            new StrokeSet(1, 0, 0, 0, 2, 2, 1, 0, 2, 2, 1, 1));

    // ── Demon-soul spells (Phase B) ──────────────────────────────────────────
    // Curse spells with a knowledge key; the source learns them from Demon souls. The port has no
    // learning system, so any active infusion suffices (knowledge key kept for future gating).

    /** Ignianima — strike for demonic damage that grows as the caster's own health falls. */
    public static final SymbolEffect IGNIANIMA = instance().add(
            new SymbolEffectProjectile(39, "witchery.pott.ignianima", 2, true, false, "ignianima", 0) {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    float health = caster != null ? 20.0f * (caster.getHealth() / caster.getMaxHealth()) : 20.0f;
                    final float damage = health > 19.0f ? 2.0f
                            : health > 15.0f ? 3.0f
                            : health > 10.0f ? 5.0f
                            : 6.0f + (12.0f - health) / 2.0f;
                    applyEntityEffect(level, caster, hit, spell.position(), 1.5, (l, c, t) -> {
                        if (t != c) {
                            t.hurtServer(l, l.damageSources().indirectMagic(spell, c), damage);
                        }
                    });
                }
            }.setColor(0xFFCB20).setSize(3.0F),
            new StrokeSet(1, 3, 3, 0, 1, 1));

    /** Carnosa Diem — bleed the caster to refill a little infusion power. */
    public static final SymbolEffect CARNOSA_DIEM = instance().add(
            new SymbolEffect(40, "witchery.pott.carnosadiem", 1, true, false, "carnosadiem", 0) {
                @Override
                public void perform(ServerLevel level, ServerPlayer caster, int effectLevel) {
                    caster.hurtServer(level, level.damageSources().magic(), caster.getMaxHealth() * 0.1f);
                    WitcheryPlayerData data = WitcheryAttachments.get(caster);
                    data.setInfusionEnergy(Math.min(data.getInfusionEnergy() + 10, data.getInfusionMaxEnergy()),
                            data.getInfusionMaxEnergy());
                    level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, caster.getX(), caster.getEyeY(), caster.getZ(),
                            6, 0.3, 0.3, 0.3, 0.0);
                }
            },
            new StrokeSet(1, 2, 2, 0, 1, 1));

    /** Tormentum — cast the struck player into the Torment dimension (30-minute cooldown). */
    public static final SymbolEffect TORMENTUM = instance().add(
            new SymbolEffectProjectile(42, "witchery.pott.tormentum", 25, true, true, "tormentum", 36000) {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    applyEntityEffect(level, caster, hit, spell.position(), 2.0, (l, c, t) -> {
                        if (t instanceof ServerPlayer victim) {
                            TormentHelper.sendToTorment(victim, 0);
                        }
                    });
                }
            }.setColor(0x222222).setSize(4.0F),
            new StrokeSet(1, 1, 1, 3, 2, 2));

    // ── Unforgivable curses (Phase C) ────────────────────────────────────────
    // curse + no knowledge key → "unforgivable": castable only in creative until a Beast infusion
    // exists (see SymbolEffect.hasValidInfusion / isUnforgivable).

    /** Crucio — torment the struck creature with demonic pain. */
    public static final SymbolEffect CRUCIO = instance().add(
            new SymbolEffectProjectile(9, "witchery.pott.crucio", 5, true, false, null, 0) {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity t && t != caster) {
                        float dmg = t instanceof Player ? 4.0f + 4.0f * (spell.getEffectLevel() - 1) : 4.0f;
                        t.hurtServer(level, level.damageSources().indirectMagic(spell, caster), dmg);
                    }
                }
            }.setColor(0x66007F).setSize(2.0F),
            new StrokeSet(1, 1, 3, 1, 1, 2),
            new StrokeSet(1, 1, 3, 3, 1, 1, 2, 2),
            new StrokeSet(2, 1, 3, 1, 1, 1, 2),
            new StrokeSet(2, 1, 3, 3, 1, 1, 1, 2, 2),
            new StrokeSet(3, 1, 3, 3, 3, 1, 1, 1, 1, 2, 2, 2));

    /** Imperio — bend the struck creature's will, turning it on its nearest neighbour. */
    public static final SymbolEffect IMPERIO = instance().add(
            new SymbolEffectProjectile(20, "witchery.pott.imperio", 10, true, false, null, 0) {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (caster == null || !(hit instanceof EntityHitResult ehr) || !(ehr.getEntity() instanceof Mob mob)) {
                        return;
                    }
                    Mob nearest = null;
                    double best = Double.MAX_VALUE;
                    for (Mob other : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(12.0))) {
                        if (other == mob || other == caster) {
                            continue;
                        }
                        double d = other.distanceToSqr(mob);
                        if (d < best) {
                            best = d;
                            nearest = other;
                        }
                    }
                    if (nearest != null) {
                        mob.setTarget(nearest);
                        level.sendParticles(ParticleTypes.WITCH, mob.getX(), mob.getEyeY(), mob.getZ(),
                                12, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            }.setColor(0xA3F0FF).setSize(1.5F),
            new StrokeSet(1, 2, 1, 1, 1, 1));

    /** Avada Kedavra — the killing curse. Slays a struck player outright; fells lesser creatures. */
    public static final SymbolEffect AVADA_KEDAVRA = instance().add(
            new SymbolEffectProjectile(4, "witchery.pott.avadakedavra", 101, true, false, null, 0) {
                @Override
                public void onCollision(ServerLevel level, LivingEntity caster, HitResult hit, SpellEffectEntity spell) {
                    if (!(hit instanceof EntityHitResult ehr) || !(ehr.getEntity() instanceof LivingEntity t) || t == caster) {
                        return;
                    }
                    boolean creative = caster instanceof Player p && p.getAbilities().instabuild;
                    var source = level.damageSources().indirectMagic(spell, caster);
                    if (t instanceof Player || creative) {
                        t.hurtServer(level, source, 1000.0f);
                    } else if (t.getMaxHealth() <= 200.0f) {
                        t.hurtServer(level, source, 200.0f);
                    } else {
                        t.hurtServer(level, source, 25.0f);
                    }
                }
            }.setColor(0x33FF33).setSize(2.0F),
            new StrokeSet(1, 1, 2, 2, 0, 0, 3, 3, 3, 3, 1, 1, 2));

    /**
     * Protego — a warding shield on the caster. The source raises a light-barrier block (which the
     * port has no equivalent for), so this grants a short Resistance + Absorption ward instead.
     */
    public static final SymbolEffect PROTEGO = instance().add(
            new SymbolEffect(31, "witchery.pott.protego") {
                @Override
                public void perform(ServerLevel level, ServerPlayer caster, int effectLevel) {
                    caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1, false, true));
                    caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1, false, true));
                    level.sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getEyeY(), caster.getZ(),
                            24, 0.6, 0.6, 0.6, 0.5);
                }
            },
            new StrokeSet(1, 1, 1, 0, 0),
            new StrokeSet(1, 1, 1, 1, 0, 0, 0));

    private static boolean isDiggable(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.PODZOL) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.CLAY)
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW)
                || state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE);
    }

    private static BlockState fortify(BlockState state) {
        if (state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.CLAY)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(BlockTags.PLANKS)) {
            return Blocks.STONE.defaultBlockState();
        }
        if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE)) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        return null;
    }
}
