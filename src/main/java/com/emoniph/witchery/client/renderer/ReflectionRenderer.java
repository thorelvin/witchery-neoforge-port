package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.ReflectionEntity;
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

public class ReflectionRenderer extends HumanoidMobRenderer<ReflectionEntity, ReflectionRenderer.ReflectionRenderState, HumanoidModel<ReflectionRenderer.ReflectionRenderState>> {

    public static class ReflectionRenderState extends HumanoidRenderState {
        public Identifier skinTexture = null;
    }

    public ReflectionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ReflectionRenderState createRenderState() {
        return new ReflectionRenderState();
    }

    @Override
    public void extractRenderState(ReflectionEntity entity, ReflectionRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skinTexture = resolveSkin(entity);
    }

    @Override
    public Identifier getTextureLocation(ReflectionRenderState state) {
        return state.skinTexture != null ? state.skinTexture : DefaultPlayerSkin.getDefaultTexture();
    }

    private static Identifier resolveSkin(ReflectionEntity entity) {
        UUID ownerUUID = entity.getOwnerUUID();
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

        // Player offline — use SkinManager (returns cached or default immediately)
        String name = entity.getOwnerName();
        GameProfile profile = new GameProfile(ownerUUID, name.isEmpty() ? null : name);
        return mc.getSkinManager().createLookup(profile, false).get().body().texturePath();
    }
}
