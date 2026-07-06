package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import javax.annotation.Nonnull;
import org.joml.Vector3d;

public final class RodTipUtil {
    private static final String ATTACH_NODE = "R-Attachment";
    private static final String TIP_NODE = "Tip";

    /** Shaft length from hand attach to {@code Tip} bone (blocks), parsed from the rod model. */
    private static final double ROD_SHAFT_LENGTH_BLOCKS = resolveRodShaftLengthBlocks();

    private RodTipUtil() {}

    /**
     * World-space rod tip from the model {@code Tip} bone distance applied along look direction.
     * Model +Y is along the rod shaft, not world up — rotating the raw bone delta by head yaw/pitch
     * incorrectly lifts the anchor into the sky when pitch is near zero.
     */
    public static void getRodTipPosition(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3d out
    ) {
        var look = TargetUtil.getLook(playerRef, commandBuffer);
        float yaw = look.getRotation().yaw();
        float pitch = look.getRotation().pitch();

        Vector3d offset = new Vector3d();
        ProjectileComponent.computeStartOffset(
            true,
            FishingConstants.ATTACH_VERTICAL_CENTER,
            FishingConstants.ATTACH_HORIZONTAL_CENTER,
            ROD_SHAFT_LENGTH_BLOCKS * FishingConstants.ROD_SHAFT_LENGTH_SCALE,
            yaw,
            pitch,
            offset
        );

        out.set(look.getPosition()).add(offset);
        out.y += FishingConstants.ROD_TIP_VERTICAL_LIFT;
    }

    public static void getCastDirection(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Vector3d out
    ) {
        var look = TargetUtil.getLook(playerRef, commandBuffer);
        out.set(look.getDirection());
        if (out.lengthSquared() < 1.0e-8) {
            TransformComponent transform = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
            if (transform != null) {
                PhysicsMath.vectorFromAngles(transform.getRotation().yaw(), transform.getRotation().pitch(), out);
            }
        }
        out.normalize();
    }

    private static double resolveRodShaftLengthBlocks() {
        Vector3d attach = BlockyModelNodeResolver.findNodePositionBlocks(FishingConstants.ROD_MODEL_PATH, ATTACH_NODE);
        if (attach == null) {
            attach = BlockyModelNodeResolver.findNodePositionBlocks(FishingConstants.ROD_MODEL_PATH, "Origin_Projectile");
        }

        Vector3d tip = BlockyModelNodeResolver.findNodePositionBlocks(FishingConstants.ROD_MODEL_PATH, TIP_NODE);
        if (attach != null && tip != null) {
            double length = attach.distance(tip);
            FishingDebugLog.info(
                "Rod shaft length (attach to Tip): %.3f blocks, delta=(%.3f, %.3f, %.3f)",
                length,
                tip.x - attach.x,
                tip.y - attach.y,
                tip.z - attach.z
            );
            return length;
        }

        FishingDebugLog.warn(
            "Rod tip nodes missing in %s (attach=%s tip=%s); using fallback shaft length",
            FishingConstants.ROD_MODEL_PATH,
            attach,
            tip
        );
        return 1.2;
    }
}
