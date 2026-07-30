package com.emoniph.witchery.client.model;

import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public class SilverVatModel extends Model.Simple {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "silver_vat"), "main");
    public static final ModelLayerLocation LIQUID_LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("witchery", "silver_vat_liquid"), "main");

    private final List<ModelPart> bodyParts;
    private final List<ModelPart> westSpout;
    private final List<ModelPart> eastSpout;
    private final List<ModelPart> northSpout;
    private final List<ModelPart> southSpout;
    private final List<ModelPart> deposits;

    public SilverVatModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        bodyParts = List.of(
                root.getChild("base"),
                root.getChild("side_right"),
                root.getChild("side_front"),
                root.getChild("side_back"),
                root.getChild("side_left"));
        westSpout = List.of(root.getChild("spout_upper_west"), root.getChild("spout_lower_west"));
        eastSpout = List.of(root.getChild("spout_upper_east"), root.getChild("spout_lower_east"));
        northSpout = List.of(root.getChild("spout_upper_north"), root.getChild("spout_lower_north"));
        southSpout = List.of(root.getChild("spout_upper_south"), root.getChild("spout_lower_south"));
        deposits = List.of(
                root.getChild("silver_1"),
                root.getChild("silver_2"),
                root.getChild("silver_3"),
                root.getChild("silver_4"),
                root.getChild("silver_5"),
                root.getChild("silver_6"),
                root.getChild("silver_7"),
                root.getChild("silver_8"));
    }

    public Iterable<ModelPart> bodyParts() {
        return bodyParts;
    }

    public Iterable<ModelPart> spout(Direction direction) {
        return switch (direction) {
            case WEST -> westSpout;
            case EAST -> eastSpout;
            case NORTH -> northSpout;
            case SOUTH -> southSpout;
            default -> List.of();
        };
    }

    public List<ModelPart> deposits() {
        return deposits;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 19)
                        .addBox(0.0F, 0.0F, 0.0F, 12.0F, 1.0F, 12.0F),
                PartPose.offset(-6.0F, 23.0F, -6.0F));
        root.addOrReplaceChild("side_right",
                CubeListBuilder.create().texOffs(38, 10)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 8.0F, 12.0F),
                PartPose.offset(-7.0F, 16.0F, -6.0F));
        root.addOrReplaceChild("side_front",
                CubeListBuilder.create().texOffs(34, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 14.0F, 8.0F, 1.0F),
                PartPose.offset(-7.0F, 16.0F, -7.0F));
        root.addOrReplaceChild("side_back",
                CubeListBuilder.create().texOffs(34, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 14.0F, 8.0F, 1.0F),
                PartPose.offset(-7.0F, 16.0F, 6.0F));
        root.addOrReplaceChild("side_left",
                CubeListBuilder.create().texOffs(38, 10)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 8.0F, 12.0F),
                PartPose.offset(6.0F, 16.0F, -6.0F));

        root.addOrReplaceChild("spout_lower_west",
                CubeListBuilder.create().texOffs(15, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(4.2F, 16.0F, -0.5F));
        root.addOrReplaceChild("spout_upper_west",
                CubeListBuilder.create().texOffs(15, 3)
                        .addBox(0.0F, 0.0F, 0.0F, 4.0F, 2.0F, 3.0F),
                PartPose.offset(4.0F, 14.0F, -1.5F));
        root.addOrReplaceChild("spout_lower_east",
                CubeListBuilder.create().texOffs(15, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-5.2F, 16.0F, -0.5F));
        root.addOrReplaceChild("spout_upper_east",
                CubeListBuilder.create().texOffs(15, 3)
                        .addBox(0.0F, 0.0F, 0.0F, 4.0F, 2.0F, 3.0F),
                PartPose.offset(-8.0F, 14.0F, -1.5F));
        root.addOrReplaceChild("spout_lower_north",
                CubeListBuilder.create().texOffs(15, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-0.5F, 16.0F, -5.2F));
        root.addOrReplaceChild("spout_upper_north",
                CubeListBuilder.create().texOffs(15, 9)
                        .addBox(0.0F, 0.0F, 0.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(-1.5F, 14.0F, -8.0F));
        root.addOrReplaceChild("spout_lower_south",
                CubeListBuilder.create().texOffs(15, 0)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-0.5F, 16.0F, 4.2F));
        root.addOrReplaceChild("spout_upper_south",
                CubeListBuilder.create().texOffs(15, 9)
                        .addBox(0.0F, 0.0F, 0.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(-1.5F, 14.0F, 4.0F));

        root.addOrReplaceChild("silver_1",
                CubeListBuilder.create().texOffs(0, 3)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-2.2F, 19.3F, -3.9F));
        root.addOrReplaceChild("silver_2",
                CubeListBuilder.create().texOffs(0, 6)
                        .addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(1.6F, 19.0F, -2.1F));
        root.addOrReplaceChild("silver_3",
                CubeListBuilder.create().texOffs(0, 9)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(-3.8F, 19.1F, 3.1F));
        root.addOrReplaceChild("silver_4",
                CubeListBuilder.create().texOffs(0, 13)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-3.4F, 18.8F, 0.9F));
        root.addOrReplaceChild("silver_5",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(1.6F, 19.0F, -0.1F));
        root.addOrReplaceChild("silver_6",
                CubeListBuilder.create().texOffs(0, 19)
                        .addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(-4.6F, 19.1F, -1.6F));
        root.addOrReplaceChild("silver_7",
                CubeListBuilder.create().texOffs(0, 22)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-0.5F, 19.0F, 2.0F));
        root.addOrReplaceChild("silver_8",
                CubeListBuilder.create().texOffs(0, 25)
                        .addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-1.2F, 19.0F, -0.3F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createLiquidLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("liquid",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-8.0F, 0.0F, -8.0F, 16.0F, 0.0F, 16.0F),
                PartPose.offset(0.0F, 19.2F, 0.0F));
        return LayerDefinition.create(mesh, 64, 16);
    }
}
