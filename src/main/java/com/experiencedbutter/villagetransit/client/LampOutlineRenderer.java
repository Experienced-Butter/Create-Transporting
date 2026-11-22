package com.experiencedbutter.villagetransit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws the two blocks a platform lamp is bound to for five seconds after its screen was opened. */
@EventBusSubscriber(modid = "create_village_transit", value = Dist.CLIENT)
public final class LampOutlineRenderer {

    private LampOutlineRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!LampOutline.visible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().equals(LampOutline.dimension)) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var buffer = minecraft.renderBuffers().bufferSource();
        var lines = buffer.getBuffer(RenderType.lines());
        if (LampOutline.village != null) {
            LevelRenderer.renderLineBox(pose, lines, box(LampOutline.village), 0.3f, 1.0f, 0.5f, 1.0f);
        }
        if (LampOutline.station != null) {
            LevelRenderer.renderLineBox(pose, lines, box(LampOutline.station), 1.0f, 0.8f, 0.25f, 1.0f);
        }
        LevelRenderer.renderLineBox(pose, lines, box(LampOutline.lamp), 0.55f, 0.8f, 1.0f, 1.0f);
        buffer.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static AABB box(net.minecraft.core.BlockPos pos) {
        return new AABB(pos).inflate(0.004);
    }
}
