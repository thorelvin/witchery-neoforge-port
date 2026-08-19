package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.MirrorDecoyEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MirrorDecoyRenderer extends HumanoidMobRenderer<MirrorDecoyEntity,
        MirrorDecoyRenderer.State, HumanoidModel<MirrorDecoyRenderer.State>> {
    public static class State extends HumanoidRenderState {
        private Identifier skin;
    }

    public MirrorDecoyRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        ArmorModelSet<HumanoidModel<State>> armor = ArmorModelSet.bake(
                ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new);
        addLayer(new HumanoidArmorLayer<>(this, armor, context.getEquipmentRenderer()));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MirrorDecoyEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = resolveSkin(entity.getOwnerUUID(), entity.getOwnerName());
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return state.skin;
    }

    private static Identifier resolveSkin(UUID uuid, String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Player player = uuid == null ? null : minecraft.level.getPlayerByUUID(uuid);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                return clientPlayer.getSkin().body().texturePath();
            }
        }
        if (uuid == null && name.isBlank()) return DefaultPlayerSkin.getDefaultTexture();
        UUID profileUuid = uuid != null ? uuid
                : UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return minecraft.getSkinManager().createLookup(new GameProfile(profileUuid,
                name.isBlank() ? null : name), false).get().body().texturePath();
    }
}
