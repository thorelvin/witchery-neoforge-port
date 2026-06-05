package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.SpectreEntity;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/**
 * Stores the spirit-bound effect on a fetish block (Scarecrow / Trent Effigy / Witch's Ladder).
 *
 * effectType:
 *   0 = none
 *   1 = Voodoo Protection (passive — enhances carried poppets near this fetish)
 *   2 = Sentinel          (spawns spectres against intruders)
 *   3 = Shrieking         (redstone signal + sound when intruder detected)
 *   4 = Disorientation    (nausea on armed/armored intruders)
 *   5 = Ghost Walking     (prevents spirit-world players from waking up)
 *
 * alarmMode:
 *   0 = trigger when player NOT in whitelist
 *   1 = trigger when player IS in blacklist
 *   5 = OFF (default)
 */
public class FetishBlockEntity extends BlockEntity {

    // Effect type IDs matching original InfusedSpiritEffect
    public static final int EFFECT_NONE              = 0;
    public static final int EFFECT_VOODOO_PROTECTION = 1;
    public static final int EFFECT_SENTINEL          = 2;
    public static final int EFFECT_SHRIEKING         = 3;
    public static final int EFFECT_DISORIENTATION    = 4;
    public static final int EFFECT_GHOST_WALKING     = 5;

    public static final int MODE_PLAYER_WHITELIST = 0; // trigger if player not in list
    public static final int MODE_PLAYER_BLACKLIST = 1; // trigger if player in list
    public static final int MODE_OFF             = 5; // never trigger

    private int effectType  = EFFECT_NONE;
    private int alarmMode   = MODE_OFF;
    private Direction facing = Direction.NORTH;
    private final List<String> knownPlayers = new ArrayList<>();

    private static final String ITEM_EFFECT_TAG = "FetishEffect";
    private static final String ITEM_ALARM_MODE_TAG = "FetishAlarmMode";
    private static final String ITEM_KNOWN_PLAYERS_TAG = "FetishKnownPlayers";

    // Transient: not saved to disk
    private boolean lastRaiseAlarm = false;
    private long lastActivationTime = 0;
    private int tickCount = 0;

    public FetishBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.FETISH.get(), pos, state);
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, FetishBlockEntity be) {
        if (++be.tickCount < 20) return;
        be.tickCount = 0;

        if (be.effectType == EFFECT_NONE || be.alarmMode == MODE_OFF) return;

        double radius = radiusForEffect(be.effectType);
        if (radius <= 0) return;

        AABB box = new AABB(pos).inflate(radius);
        List<Player> players = level.getEntitiesOfClass(Player.class, box);

        List<LivingEntity> triggeredEntities = new ArrayList<>();
        boolean raiseAlarm = false;

        for (Player player : players) {
            boolean inList = be.knownPlayers.contains(player.getName().getString())
                    || be.knownPlayers.contains(player.getGameProfile().name());
            boolean trigger = switch (be.alarmMode) {
                case MODE_PLAYER_WHITELIST -> !inList; // alarm if NOT in whitelist
                case MODE_PLAYER_BLACKLIST ->  inList; // alarm if IS in blacklist
                default -> false;
            };
            if (trigger) {
                raiseAlarm = true;
                triggeredEntities.add(player);
            }
        }

        long now = level.getGameTime();
        int cooldown = cooldownForEffect(be.effectType);
        boolean onCooldown = cooldown > 0 && now < be.lastActivationTime + cooldown;

        if (!onCooldown && raiseAlarm) {
            be.applyEffect(level, pos, triggeredEntities);
            be.lastActivationTime = now;
        }

        if (be.lastRaiseAlarm != raiseAlarm) {
            be.lastRaiseAlarm = raiseAlarm;
            if (be.effectType == EFFECT_SHRIEKING) {
                // Notify neighbours for redstone update
                level.updateNeighborsAt(pos, state.getBlock());
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    private void applyEffect(Level level, BlockPos pos, List<LivingEntity> targets) {
        switch (effectType) {
            case EFFECT_VOODOO_PROTECTION -> {
                // Passive — checked by poppet/curse code via isNearFetish(player, EFFECT_VOODOO_PROTECTION)
            }
            case EFFECT_SENTINEL -> applysentinel(level, pos, targets);
            case EFFECT_SHRIEKING -> applyShrieking(level, pos);
            case EFFECT_DISORIENTATION -> applyDisorientation(targets);
            case EFFECT_GHOST_WALKING -> applyGhostWalking(targets);
        }
    }

    private void applysentinel(Level level, BlockPos pos, List<LivingEntity> targets) {
        for (LivingEntity target : targets) {
            int count = targets.size() > 1 ? 1 : 2;
            for (int i = 0; i < count; i++) {
                SpectreEntity spectre = WitcheryEntities.SPECTRE.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (spectre != null) {
                    spectre.setPos(target.getX(), target.getY(), target.getZ());
                    spectre.finalizeSpawn((net.minecraft.server.level.ServerLevel) level,
                            ((net.minecraft.server.level.ServerLevel) level).getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
                    spectre.setTarget(target);
                    level.addFreshEntity(spectre);
                }
            }
        }
    }

    private void applyShrieking(Level level, BlockPos pos) {
        level.playSound(null,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.WITHER_AMBIENT,
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.5f, 1.4f);
    }

    private void applyDisorientation(List<LivingEntity> targets) {
        for (LivingEntity entity : targets) {
            if (!(entity instanceof Player player)) continue;
            // Only affect armed or armored players (matching original "threat" detection)
            boolean armed = !player.getMainHandItem().isEmpty();
            boolean armored = player.getArmorValue() > 0;
            if (armed || armored) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
        }
    }

    private void applyGhostWalking(List<LivingEntity> targets) {
        for (LivingEntity entity : targets) {
            if (!(entity instanceof Player player)) continue;
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            if (data.isInSpiritWorld()) {
                data.setGhostWalkerActive(true);
            }
        }
    }

    // ── Public helpers ────────────────────────────────────────────────────────

    public int getPowerLevel() {
        return effectType == EFFECT_SHRIEKING && lastRaiseAlarm ? 15 : 0;
    }

    public int getEffectType()  { return effectType; }
    public int getAlarmMode()   { return alarmMode; }

    public void setEffectType(int type) {
        this.effectType = type;
        setChanged();
    }

    public void toggleKnownPlayer(String name) {
        if (knownPlayers.contains(name)) knownPlayers.remove(name);
        else knownPlayers.add(name);
        setChanged();
    }

    public void cycleAlarmMode() {
        alarmMode = (alarmMode + 1) % 6;
        setChanged();
    }

    public String knownPlayersDisplay() {
        return knownPlayers.isEmpty() ? "-" : String.join(", ", knownPlayers);
    }

    public void loadFromItemData(CompoundTag tag) {
        effectType = tag.getIntOr(ITEM_EFFECT_TAG, tag.getIntOr("EffectTypeID", EFFECT_NONE));
        alarmMode = tag.getIntOr(ITEM_ALARM_MODE_TAG, tag.getIntOr("AlarmMode", MODE_OFF));
        knownPlayers.clear();
        readKnownPlayers(tag.getListOrEmpty(ITEM_KNOWN_PLAYERS_TAG));
        if (knownPlayers.isEmpty()) {
            readKnownPlayers(tag.getListOrEmpty("KnownPlayers"));
        }
        setChanged();
    }

    public ItemStack createDropStack(Block block) {
        ItemStack stack = new ItemStack(block);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, this::writeItemData);
        return stack;
    }

    private void writeItemData(CompoundTag tag) {
        tag.putInt(ITEM_EFFECT_TAG, effectType);
        tag.putInt(ITEM_ALARM_MODE_TAG, alarmMode);
        if (!knownPlayers.isEmpty()) {
            ListTag players = new ListTag();
            for (String name : knownPlayers) {
                players.add(StringTag.valueOf(name));
            }
            tag.put(ITEM_KNOWN_PLAYERS_TAG, players);
        }
    }

    private void readKnownPlayers(ListTag players) {
        for (int i = 0; i < players.size(); i++) {
            String name = players.getString(i).orElse("");
            if (name != null && !name.isBlank() && !knownPlayers.contains(name)) {
                knownPlayers.add(name);
            }
        }
    }

    /** Returns true if a player is within 16 blocks of a fetish with the given effect type. */
    public static boolean isNearFetish(Player player, int effectType) {
        Level level = player.level();
        net.minecraft.world.level.ChunkPos minC = new net.minecraft.world.level.ChunkPos(player.blockPosition().offset(-16, 0, -16));
        net.minecraft.world.level.ChunkPos maxC = new net.minecraft.world.level.ChunkPos(player.blockPosition().offset( 16, 0,  16));
        for (int cx = minC.x; cx <= maxC.x; cx++) {
            for (int cz = minC.z; cz <= maxC.z; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (var entry : level.getChunk(cx, cz).getBlockEntities().entrySet()) {
                    if (!(entry.getValue() instanceof FetishBlockEntity f)) continue;
                    if (f.getEffectType() != effectType) continue;
                    BlockPos fp = entry.getKey();
                    if (player.distanceToSqr(fp.getX() + 0.5, fp.getY() + 0.5, fp.getZ() + 0.5) <= 256.0) return true;
                }
            }
        }
        return false;
    }

    // ── Radius / cooldown tables ──────────────────────────────────────────────

    private static double radiusForEffect(int type) {
        return switch (type) {
            case EFFECT_VOODOO_PROTECTION -> 0; // passive
            case EFFECT_SENTINEL          -> 8;
            case EFFECT_SHRIEKING         -> 16;
            case EFFECT_DISORIENTATION    -> 8;
            case EFFECT_GHOST_WALKING     -> 8;
            default -> 0;
        };
    }

    private static int cooldownForEffect(int type) {
        return switch (type) {
            case EFFECT_SENTINEL       -> 600; // 30 seconds
            case EFFECT_DISORIENTATION -> 10;
            default -> 0;
        };
    }

    public Direction getFacing() { return facing; }
    public void setFacing(Direction d) { this.facing = d; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("EffectTypeID", effectType);
        tag.putInt("AlarmMode", alarmMode);
        tag.putString("Facing", facing.getName());
        tag.putLong("LastActivation", lastActivationTime);
        if (!knownPlayers.isEmpty()) {
            var playerListOut = tag.list("KnownPlayers", com.mojang.serialization.Codec.STRING);
            for (String name : knownPlayers) playerListOut.add(name);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        effectType        = tag.getIntOr("EffectTypeID", 0);
        alarmMode = tag.getIntOr("AlarmMode", MODE_OFF);
        lastActivationTime = tag.getLongOr("LastActivation", 0L);
        facing = Direction.byName(tag.getStringOr("Facing", "north"));
        if (facing == null || facing.getAxis() == Direction.Axis.Y) facing = Direction.NORTH;
        knownPlayers.clear();
        for (String name : tag.listOrEmpty("KnownPlayers", com.mojang.serialization.Codec.STRING)) {
            knownPlayers.add(name);
        }
    }

    // ── Network sync ──────────────────────────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("EffectTypeID", effectType);
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
