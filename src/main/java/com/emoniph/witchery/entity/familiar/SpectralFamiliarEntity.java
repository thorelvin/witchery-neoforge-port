package com.emoniph.witchery.entity.familiar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Spectral Familiar - a summoned bound familiar that can be fed ore-related
 * items to lead its owner toward nearby ore columns, closer to the original
 * Witchery "find shinies" role than the previous cat placeholder.
 */
public class SpectralFamiliarEntity extends FamiliarEntity {

    private static final List<SearchSpec> SEARCH_SPECS = List.of(
            new SearchSpec(Items.DIAMOND, BlockTags.DIAMOND_ORES, 14),
            new SearchSpec(Items.EMERALD, BlockTags.EMERALD_ORES, 14),
            new SearchSpec(Items.GOLD_INGOT, BlockTags.GOLD_ORES, 30),
            new SearchSpec(Items.IRON_INGOT, BlockTags.IRON_ORES, 64),
            new SearchSpec(Items.REDSTONE, BlockTags.REDSTONE_ORES, 16),
            new SearchSpec(Items.LAPIS_LAZULI, BlockTags.LAPIS_ORES, 30),
            new SearchSpec(Items.COAL, BlockTags.COAL_ORES, 64)
    );

    @Nullable
    private SearchSpec activeSearch;
    @Nullable
    private BlockPos searchPerch;
    private int searches;

    public SpectralFamiliarEntity(EntityType<? extends SpectralFamiliarEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new SpectralFamiliarFindOreGoal(this, 1.33));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.activeSearch == null || this.searchPerch != null || this.tickCount % 20 != 0) {
            return;
        }
        this.searchPerch = findSearchPerch(this.activeSearch);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        SearchSpec spec = getSearchSpec(held);
        if (spec != null && this.isOwnedBy(player)) {
            if (this.level().isClientSide()) {
                return InteractionResult.CONSUME;
            }
            beginOreSearch((ServerPlayer) player, hand, held, spec);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    public boolean hasPendingSearch() {
        return this.activeSearch != null && this.searchPerch != null;
    }

    @Nullable
    public BlockPos getSearchPerch() {
        return this.searchPerch;
    }

    public void clearSearch() {
        this.activeSearch = null;
        this.searchPerch = null;
    }

    public void finishSearch() {
        if (!(this.getOwner() instanceof ServerPlayer owner)) {
            clearSearch();
            return;
        }

        BlockPos pos = this.blockPosition();
        owner.displayClientMessage(Component.translatable(
                "witchery.familiar.foundsomething",
                Integer.toString(pos.getX()),
                Integer.toString(pos.getY()),
                Integer.toString(pos.getZ())), false);
        this.level().playSound(null, this.blockPosition(), SoundEvents.PIG_AMBIENT, SoundSource.NEUTRAL, 0.8F, 1.0F);
        this.setOrderedToSit(true);
        clearSearch();
    }

    private void beginOreSearch(ServerPlayer player, InteractionHand hand, ItemStack held, SearchSpec spec) {
        this.searches++;
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (held.isEmpty()) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        }

        if (shouldDisappearAfterSearch()) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
            this.discard();
            return;
        }

        this.activeSearch = spec;
        this.searchPerch = findSearchPerch(spec);
        this.setOrderedToSit(false);
        this.level().playSound(null, this.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.6F, 1.2F);
    }

    private boolean shouldDisappearAfterSearch() {
        if (this.searches <= 1) {
            return false;
        }

        double disappearanceChance = switch (Math.min(this.searches, 5)) {
            case 2 -> 0.60;
            case 3 -> 0.75;
            case 4 -> 0.85;
            default -> 0.95;
        };
        return this.random.nextDouble() < disappearanceChance;
    }

    @Nullable
    private SearchSpec getSearchSpec(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (SearchSpec spec : SEARCH_SPECS) {
            if (stack.is(spec.item())) {
                return spec;
            }
        }
        return null;
    }

    @Nullable
    private BlockPos findSearchPerch(SearchSpec spec) {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int y = 1; y < spec.maxDepth(); y++) {
            for (int x = origin.getX() - 4; x < origin.getX() + 4; x++) {
                for (int z = origin.getZ() - 4; z < origin.getZ() + 4; z++) {
                    BlockPos orePos = new BlockPos(x, y, z);
                    BlockState state = this.level().getBlockState(orePos);
                    if (!state.is(spec.oreTag())) {
                        continue;
                    }

                    int surfaceY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos perch = new BlockPos(x, surfaceY, z);
                    BlockState below = this.level().getBlockState(perch.below());
                    if (below.isAir()) {
                        continue;
                    }

                    double distance = this.distanceToSqr(perch.getX() + 0.5, perch.getY(), perch.getZ() + 0.5);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = perch;
                    }
                }
            }
        }

        return best;
    }

    private record SearchSpec(Item item, TagKey<Block> oreTag, int maxDepth) {
    }
}
