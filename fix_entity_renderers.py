import os

base = r"src\main\java\com\emoniph\witchery\client\renderer"

TEMPLATES = {
    "NightmareRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.NightmareModel;
import com.emoniph.witchery.entity.NightmareEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class NightmareRenderer extends MobRenderer<NightmareEntity, LivingEntityRenderState, NightmareModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/nightmare.png");
    public NightmareRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NightmareModel(ctx.bakeLayer(NightmareModel.LAYER_LOCATION)), 0.6f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
""",
    "SpiritRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.SpiritModel;
import com.emoniph.witchery.entity.SpiritEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class SpiritRenderer extends MobRenderer<SpiritEntity, LivingEntityRenderState, SpiritModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/spirit.png");
    public SpiritRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpiritModel(ctx.bakeLayer(SpiritModel.LAYER_LOCATION)), 0.25f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
    @Override
    public void submit(LivingEntityRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose(); ps.scale(0.5f, 0.5f, 0.5f);
        super.submit(state, ps, collector, camera);
        ps.popPose();
    }
}
""",
    "LordOfTormentRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LordOfTormentModel;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class LordOfTormentRenderer extends MobRenderer<LordOfTormentEntity, LivingEntityRenderState, LordOfTormentModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/lord_of_torment.png");
    public LordOfTormentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LordOfTormentModel(ctx.bakeLayer(LordOfTormentModel.LAYER_LOCATION)), 0.7f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
""",
    "FlameImpRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.FlameImpModel;
import com.emoniph.witchery.entity.FlameImpEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class FlameImpRenderer extends MobRenderer<FlameImpEntity, LivingEntityRenderState, FlameImpModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/imp.png");
    public FlameImpRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FlameImpModel(ctx.bakeLayer(FlameImpModel.LAYER_LOCATION)), 0.4f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
""",
    "DeathEntityRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.DeathEntityModel;
import com.emoniph.witchery.entity.DeathEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class DeathEntityRenderer extends MobRenderer<DeathEntity, LivingEntityRenderState, DeathEntityModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/death.png");
    public DeathEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DeathEntityModel(ctx.bakeLayer(DeathEntityModel.LAYER_LOCATION)), 0.5f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }
}
""",
    "CovenWitchRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.CovenWitchEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.resources.Identifier;

public class CovenWitchRenderer extends MobRenderer<CovenWitchEntity, WitchRenderState, WitchModel> {

    private static final Identifier[] SKINS = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch1.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch3.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch4.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch5.png"),
    };

    public CovenWitchRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchModel(ctx.bakeLayer(ModelLayers.WITCH)), 0.5F);
    }

    @Override public WitchRenderState createRenderState() { return new WitchRenderState(); }

    @Override
    public void extractRenderState(CovenWitchEntity entity, WitchRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
    }

    @Override
    public Identifier getTextureLocation(WitchRenderState state) {
        int skin = Math.abs(state.entityId) % SKINS.length;
        return SKINS[skin];
    }
}
""",
    "BinkyRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.BinkyEntity;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.resources.Identifier;

public class BinkyRenderer extends MobRenderer<BinkyEntity, EquineRenderState, HorseModel> {

    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/horse/horse_white.png");

    public BinkyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HorseModel(ctx.bakeLayer(ModelLayers.HORSE)), 0.75f);
    }

    @Override public EquineRenderState createRenderState() { return new EquineRenderState(); }

    @Override
    public Identifier getTextureLocation(EquineRenderState state) { return TEXTURE; }
}
""",
    "WitchHunterRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.WitchHunterModel;
import com.emoniph.witchery.entity.WitchHunterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class WitchHunterRenderer extends MobRenderer<WitchHunterEntity, LivingEntityRenderState, WitchHunterModel> {

    private static final Identifier[] SKINS = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter1.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/witchhunter3.png"),
    };

    public WitchHunterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchHunterModel(ctx.bakeLayer(WitchHunterModel.LAYER_LOCATION)), 0.5F);
    }

    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return SKINS[0];
    }
}
""",
    "HobgoblinRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.HobgoblinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class HobgoblinRenderer extends HumanoidMobRenderer<HobgoblinEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier[] TEXTURES = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin3.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin4.png"),
    };

    public HobgoblinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        int idx = (int)(Math.abs(state.id) % TEXTURES.length);
        return TEXTURES[idx];
    }

    @Override
    public void submit(HumanoidRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose(); ps.scale(0.75f, 0.75f, 0.75f);
        super.submit(state, ps, collector, camera);
        ps.popPose();
    }
}
""",
    "FlyingMonkeyRenderer": """package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.FlyingMonkeyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class FlyingMonkeyRenderer extends HumanoidMobRenderer<FlyingMonkeyEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/flying_monkey.png");

    public FlyingMonkeyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.4F);
    }

    @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) { return TEXTURE; }

    @Override
    public void submit(HumanoidRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose(); ps.scale(0.65f, 0.65f, 0.65f);
        super.submit(state, ps, collector, camera);
        ps.popPose();
    }
}
""",
}

for cls, content in TEMPLATES.items():
    path = os.path.join(base, cls + ".java")
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Rewrote {cls}")

print("Done")
