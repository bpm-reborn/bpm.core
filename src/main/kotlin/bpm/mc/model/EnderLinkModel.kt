package bpm.mc.model


import bpm.Bpm
import bpm.mc.projectile.EnderLinkProjectile
import net.minecraft.client.model.HierarchicalModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.ResourceLocation

class EnderLinkModel(private val root: ModelPart) : HierarchicalModel<EnderLinkProjectile>() {
    private val innerCore: ModelPart = root.getChild("inner_core")
    private val outerCore: ModelPart = root.getChild("outer_core")
    private val ring: ModelPart = root.getChild("ring")

    companion object {

        val LAYER_LOCATION = ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Bpm.ID, "ender_link_projectile"),
            "main"
        )

        fun createBodyLayer(): LayerDefinition {
            val meshDefinition = MeshDefinition()
            val root = meshDefinition.root

            // Inner core (smallest)
            root.addOrReplaceChild(
                "inner_core",
                CubeListBuilder.create()
                    .addBox(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f),
                PartPose.ZERO
            )

            // Outer core (medium)
            root.addOrReplaceChild(
                "outer_core",
                CubeListBuilder.create()
                    .addBox(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f),
                PartPose.ZERO
            )

            // Outer ring (largest)
            root.addOrReplaceChild(
                "ring",
                CubeListBuilder.create()
                    .addBox(-2.0f, -2.0f, -0.5f, 4.0f, 4.0f, 1.0f),
                PartPose.ZERO
            )

            return LayerDefinition.create(meshDefinition, 16, 16)
        }
    }

    override fun root(): ModelPart = root

    override fun setupAnim(
        entity: EnderLinkProjectile,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        headYaw: Float,
        headPitch: Float
    ) {
        // Rotate inner core quickly
        innerCore.yRot = ageInTicks * 0.3f
        innerCore.xRot = ageInTicks * 0.2f

        // Rotate outer core slightly slower in opposite direction
        outerCore.yRot = -ageInTicks * 0.2f
        outerCore.xRot = -ageInTicks * 0.1f

        // Rotate ring on different axes
        ring.zRot = ageInTicks * 0.1f
    }
}