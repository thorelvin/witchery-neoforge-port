package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.StatueHobgoblinPatronBlockEntity;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Statue of Hobgoblin Patron — nearby hobgoblins worship it instead of their natural shrine.
 * Right-click to collect worship power (stealthy prayer theft).
 * Each use has a chance to anger Gulg or Mog, who send avatars to attack the witch.
 */
public class StatueHobgoblinPatronBlock extends BaseEntityBlock {
    public static final MapCodec<StatueHobgoblinPatronBlock> CODEC = simpleCodec(StatueHobgoblinPatronBlock::new);

    private static final int SEARCH_RADIUS = 16;
    private static final float ANGER_CHANCE = 0.25f; // 25% chance each use angers a god

    public StatueHobgoblinPatronBlock(BlockBehaviour.Properties props) { super(props); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatueHobgoblinPatronBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()
                || !(level.getBlockEntity(pos) instanceof StatueHobgoblinPatronBlockEntity statue)) {
            return;
        }
        String owner = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getStringOr(StatueHobgoblinPatronBlockEntity.ITEM_OWNER_KEY, "");
        if (!owner.isEmpty()) {
            statue.setOwner(owner);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;

        // Count worshipping hobgoblins nearby
        AABB range = new AABB(pos).inflate(SEARCH_RADIUS);
        List<HobgoblinEntity> hobgoblins = sl.getEntitiesOfClass(HobgoblinEntity.class, range);

        if (hobgoblins.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.statue_patron.no_worshippers"), false);
            return InteractionResult.FAIL;
        }

        // Grant a brief power boost based on hobgoblin count
        int power = hobgoblins.size();
        player.displayClientMessage(Component.translatable("witchery.statue_patron.power_gained", power), false);

        // Chance to anger Gulg or Mog
        if (sl.getRandom().nextFloat() < ANGER_CHANCE) {
            boolean gulg = sl.getRandom().nextBoolean();
            player.displayClientMessage(Component.translatable(
                    gulg ? "witchery.statue_patron.gulg_angered" : "witchery.statue_patron.mog_angered"), false);
            // All nearby hobgoblins turn hostile to the player temporarily
            for (HobgoblinEntity hob : hobgoblins) {
                hob.setTarget(player);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof StatueHobgoblinPatronBlockEntity statue) {
            return List.of(statue.createDropStack(this));
        }
        return List.of(new ItemStack(this));
    }
}
