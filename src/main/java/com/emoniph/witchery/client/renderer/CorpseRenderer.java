package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.CorpseEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Renders the Spirit World sleeping body with the owner player's own skin (source
 * {@code EntityCorpse} resolved the skin from the owner username). Falls back to the default
 * player skin when the owner is unknown or offline and uncached.
 */
public class CorpseRenderer extends HumanoidMobRenderer<CorpseEntity, CorpseRenderer.CorpseRenderState, HumanoidModel<CorpseRenderer.CorpseRenderState>> {

    public static class CorpseRenderState extends HumanoidRenderState {
        public Identifier skinTexture = null;
    }

    public CorpseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public CorpseRenderState createRenderState() {
        return new CorpseRenderState();
    }

    @Override
    public void extractRenderState(CorpseEntity entity, CorpseRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skinTexture = resolveSkin(entity);
    }

    @Override
    public Identifier getTextureLocation(CorpseRenderState state) {
        return state.skinTexture != null ? state.skinTexture : DefaultPlayerSkin.getDefaultTexture();
    }

    private static Identifier resolveSkin(CorpseEntity entity) {
        UUID ownerUUID = entity.getOwnerUUID().orElse(null);
        if (ownerUUID == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(ownerUUID);
            if (player instanceof AbstractClientPlayer acp) {
                return acp.getSkin().body().texturePath();
            }
        }

        // Owner offline — resolve through the skin cache (returns cached or default immediately).
        String name = entity.getOwnerName();
        GameProfile profile = new GameProfile(ownerUUID, name.isEmpty() ? null : name);
        return mc.getSkinManager().createLookup(profile, false).get().body().texturePath();
    }
}
