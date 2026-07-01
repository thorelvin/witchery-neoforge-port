package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.IllusionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class IllusionRenderers {
    private static final Identifier CREEPER_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png");
    private static final Identifier SPIDER_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/spider/spider.png");
    private static final Identifier ZOMBIE_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");

    private IllusionRenderers() {
    }

    private static boolean visibleToLocalPlayer(IllusionEntity entity) {
        Player player = Minecraft.getInstance().player;
        return player != null && entity.isVisibleTo(player);
    }

    public static final class Creeper extends MobRenderer<IllusionEntity.Creeper, CreeperRenderState, CreeperModel> {
        public Creeper(EntityRendererProvider.Context context) {
            super(context, new CreeperModel(context.bakeLayer(ModelLayers.CREEPER)), 0.0F);
        }

        @Override
        public boolean shouldRender(IllusionEntity.Creeper entity, Frustum frustum,
                                    double cameraX, double cameraY, double cameraZ) {
            return visibleToLocalPlayer(entity) && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
        }

        @Override
        public CreeperRenderState createRenderState() {
            return new CreeperRenderState();
        }

        @Override
        public void extractRenderState(IllusionEntity.Creeper entity, CreeperRenderState state, float partialTick) {
            super.extractRenderState(entity, state, partialTick);
            state.swelling = 0.0F;
            state.isPowered = false;
        }

        @Override
        public Identifier getTextureLocation(CreeperRenderState state) {
            return CREEPER_TEXTURE;
        }
    }

    public static final class Spider extends MobRenderer<IllusionEntity.Spider, LivingEntityRenderState, SpiderModel> {
        public Spider(EntityRendererProvider.Context context) {
            super(context, new SpiderModel(context.bakeLayer(ModelLayers.SPIDER)), 0.0F);
        }

        @Override
        public boolean shouldRender(IllusionEntity.Spider entity, Frustum frustum,
                                    double cameraX, double cameraY, double cameraZ) {
            return visibleToLocalPlayer(entity) && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
        }

        @Override
        protected float getFlipDegrees() {
            return 180.0F;
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return SPIDER_TEXTURE;
        }
    }

    public static final class Zombie extends MobRenderer<IllusionEntity.Zombie, ZombieRenderState, ZombieModel<ZombieRenderState>> {
        public Zombie(EntityRendererProvider.Context context) {
            super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.0F);
        }

        @Override
        public boolean shouldRender(IllusionEntity.Zombie entity, Frustum frustum,
                                    double cameraX, double cameraY, double cameraZ) {
            return visibleToLocalPlayer(entity) && super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
        }

        @Override
        public ZombieRenderState createRenderState() {
            return new ZombieRenderState();
        }

        @Override
        public Identifier getTextureLocation(ZombieRenderState state) {
            return ZOMBIE_TEXTURE;
        }
    }
}
