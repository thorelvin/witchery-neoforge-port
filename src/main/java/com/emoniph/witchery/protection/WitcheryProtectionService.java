package com.emoniph.witchery.protection;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.ai.NpcCommandKind;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.HobgoblinEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class WitcheryProtectionService {
    private static final List<WitcheryProtectionAdapter> ADAPTERS = List.of(
            new OpenPacProtectionAdapter(),
            new FtbChunksProtectionAdapter(),
            new CommonProtectionApiAdapter(),
            new VanillaProtectionAdapter()
    );

    private WitcheryProtectionService() {
    }

    public static boolean canCommandNpc(ServerPlayer player, LivingEntity npc, NpcCommandKind command) {
        if (!WitcheryConfig.PROTECT_NPC_COMMANDS.get()) {
            return true;
        }
        if (player == null || npc == null || command == null) {
            return false;
        }
        if (isAdmin(player)) {
            return true;
        }
        if (npc instanceof CovenWitchEntity witch) {
            return !witch.isRecruited() || witch.isRecruitedBy(player);
        }
        if (npc instanceof HobgoblinEntity hobgoblin) {
            return hobgoblin.canAcceptCommandFrom(player);
        }
        return false;
    }

    public static boolean canModifyBlock(@Nullable ServerPlayer actor, ServerLevel level, BlockPos pos,
                                         WitcheryProtectionAction action) {
        return canModifyBlock(actor, level, pos, action, true);
    }

    /**
     * Checks a cross-dimension terrain change while the acting player remains in the source level.
     * Claim adapters still receive the real destination level and position; only the ordinary
     * same-level actor guard is relaxed.
     */
    public static boolean canModifyRemoteBlock(@Nullable ServerPlayer actor, ServerLevel level, BlockPos pos,
                                               WitcheryProtectionAction action) {
        return canModifyBlock(actor, level, pos, action, false);
    }

    private static boolean canModifyBlock(@Nullable ServerPlayer actor, ServerLevel level, BlockPos pos,
                                          WitcheryProtectionAction action, boolean requireActorInLevel) {
        if (!WitcheryConfig.PROTECT_TERRAIN_MAGIC.get()) {
            return true;
        }
        if (level == null || pos == null || action == null) {
            return false;
        }
        if (actor == null || !actor.isAlive() || (requireActorInLevel && actor.level() != level)) {
            return false;
        }
        if (isAdmin(actor)) {
            return true;
        }
        WitcheryProtectionAdapter.Decision merged = WitcheryProtectionAdapter.Decision.PASS;
        for (WitcheryProtectionAdapter adapter : ADAPTERS) {
            if (!adapter.isAvailable()) {
                continue;
            }
            WitcheryProtectionAdapter.Decision decision = safeCanModifyBlock(adapter, actor, level, pos, action);
            if (decision == WitcheryProtectionAdapter.Decision.DENY) {
                return false;
            }
            merged = mergeDecision(merged, decision);
        }
        return merged == WitcheryProtectionAdapter.Decision.ALLOW;
    }

    public static boolean canModifyArea(@Nullable ServerPlayer actor, ServerLevel level, Iterable<BlockPos> positions,
                                        WitcheryProtectionAction action, WitcheryAreaMode mode) {
        if (!WitcheryConfig.PROTECT_TERRAIN_MAGIC.get()) {
            return true;
        }
        if (positions == null) {
            return false;
        }
        if (mode == WitcheryAreaMode.SKIP) {
            return true;
        }
        for (BlockPos pos : positions) {
            if (!canModifyBlock(actor, level, pos, action)) {
                return false;
            }
        }
        return true;
    }

    public static WitcheryAreaMode configuredAreaMode() {
        return WitcheryAreaMode.parse(WitcheryConfig.TERRAIN_MAGIC_PROTECTED_AREA_MODE.get(), WitcheryAreaMode.SKIP);
    }

    public static WitcheryAreaMode configuredDestructiveCoreMode() {
        return WitcheryAreaMode.parse(WitcheryConfig.TERRAIN_MAGIC_DESTRUCTIVE_CORE_MODE.get(), WitcheryAreaMode.FAIL);
    }

    public static boolean isAdmin(ServerPlayer player) {
        return player != null && player.createCommandSourceStack().permissions().hasPermission(
                new net.minecraft.server.permissions.Permission.HasCommandLevel(
                        net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS));
    }

    static WitcheryProtectionAdapter.Decision mergeDecision(WitcheryProtectionAdapter.Decision current,
                                                            WitcheryProtectionAdapter.Decision next) {
        if (next == WitcheryProtectionAdapter.Decision.DENY) {
            return WitcheryProtectionAdapter.Decision.DENY;
        }
        if (next == WitcheryProtectionAdapter.Decision.ALLOW) {
            return WitcheryProtectionAdapter.Decision.ALLOW;
        }
        return current == null ? WitcheryProtectionAdapter.Decision.PASS : current;
    }

    static WitcheryProtectionAdapter.Decision safeCanModifyBlock(WitcheryProtectionAdapter adapter,
                                                                 @Nullable ServerPlayer actor,
                                                                 ServerLevel level,
                                                                 BlockPos pos,
                                                                 WitcheryProtectionAction action) {
        try {
            return adapter.canModifyBlock(actor, level, pos, action);
        } catch (RuntimeException ex) {
            WitcheryMod.LOGGER.warn("Witchery protection adapter '{}' failed; denying this protected action",
                    adapter.id(), ex);
            return WitcheryProtectionAdapter.Decision.DENY;
        }
    }

    private static final class VanillaProtectionAdapter implements WitcheryProtectionAdapter {
        @Override
        public String id() {
            return "vanilla";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public Decision canModifyBlock(@Nullable ServerPlayer actor, ServerLevel level, BlockPos pos,
                                       WitcheryProtectionAction action) {
            if (actor == null) {
                return Decision.DENY;
            }
            return level.mayInteract(actor, pos) ? Decision.ALLOW : Decision.DENY;
        }
    }
}
