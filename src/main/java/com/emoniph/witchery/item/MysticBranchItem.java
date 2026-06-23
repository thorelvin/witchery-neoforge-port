package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.symbol.EffectRegistry;
import com.emoniph.witchery.symbol.SymbolEffect;
import com.emoniph.witchery.symbol.SymbolEffectProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Mystic Branch — Witchery's air-glyph casting wand.
 *
 * Hold use to channel, then "draw" a symbol by flicking the look direction (up / down / left /
 * right strokes); a recognised symbol is prepared, and releasing casts it, spending infusion
 * energy. Right-click an altar top to place it as a focus item.
 *
 * Source parity: {@code ItemMysticBranch} + the symbol/{@code EffectRegistry} system
 * (1.7.10-0.24.1). Client stroke recognition lives in {@code client.SymbolDrawState}; the casting
 * lifecycle is here.
 */
public class MysticBranchItem extends Item {

    private static final int USE_DURATION = 36000;

    public MysticBranchItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        String text = Component.translatable("item.witchery.mystic_branch.tip").getString();
        for (String line : text.split("\\n")) {
            tooltip.accept(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // Altar top → place as focus item; otherwise PASS so use() begins channelling.
        return AltarPlacedItemSupport.placeOnAltar(ctx);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            WitcheryAttachments.get(player).clearPreparedSpell();
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        int id = data.getPreparedSpellId();
        int spellLevel = Math.max(1, data.getPreparedSpellLevel());
        data.clearPreparedSpell();

        SymbolEffect effect = id < 0 ? null : EffectRegistry.instance().getById(id);
        if (effect == null) {
            fail(serverLevel, serverPlayer, "witchery.infuse.branch.unknownsymbol");
            return true;
        }

        boolean creative = player.getAbilities().instabuild;
        if (!effect.hasValidInfusion(creative, data.getInfusionType())) {
            fail(serverLevel, serverPlayer, "witchery.infuse.branch.infusionrequired");
            return true;
        }

        // Demonic glyphs must be learned from a Demon soul before they will answer.
        String knowledge = effect.getKnowledgeKey();
        if (!creative && knowledge != null
                && !com.emoniph.witchery.init.WitcheryAttachments.learned(player).keys.contains(knowledge)) {
            fail(serverLevel, serverPlayer, "witchery.infuse.branch.unknownknowledge");
            return true;
        }

        int cost = effect.getChargeCost(spellLevel);
        if (!creative && data.getInfusionEnergy() < cost) {
            fail(serverLevel, serverPlayer, "witchery.infuse.branch.nocharges");
            return true;
        }

        effect.perform(serverLevel, serverPlayer, spellLevel);
        success(serverLevel, serverPlayer, effect, spellLevel);
        serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS, 0.7F, 1.2F);

        if (!creative) {
            data.setInfusionEnergy(data.getInfusionEnergy() - cost, data.getInfusionMaxEnergy());
            sync(serverPlayer, data);
        }
        return true;
    }

    private static void fail(ServerLevel level, ServerPlayer player, String messageKey) {
        player.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.RED), true);
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.PLAYERS, 0.6F, 0.7F);
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getEyeY() - 0.1D, player.getZ(),
                8, 0.25D, 0.15D, 0.25D, 0.01D);
    }

    private static void success(ServerLevel level, ServerPlayer player, SymbolEffect effect, int spellLevel) {
        player.displayClientMessage(Component.translatable("witchery.infuse.branch.cast",
                Component.translatable(effect.getNameKey()), spellLevel).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        int color = effect instanceof SymbolEffectProjectile projectile ? projectile.getColor() : 0x9D4EDD;
        level.sendParticles(new DustParticleOptions(color, 0.85F),
                player.getX(), player.getEyeY() - 0.15D, player.getZ(),
                14, 0.35D, 0.25D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.WITCH,
                player.getX(), player.getEyeY() - 0.15D, player.getZ(),
                8, 0.28D, 0.2D, 0.28D, 0.01D);
    }

    private static void sync(ServerPlayer player, WitcheryPlayerData data) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }
}
