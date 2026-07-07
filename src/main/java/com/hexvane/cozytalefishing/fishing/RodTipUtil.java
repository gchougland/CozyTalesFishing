package com.hexvane.cozytalefishing.fishing;

import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.universe.world.World;
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
        FishingModConfig config = FishingModConfig.get();

        Vector3d offset = new Vector3d();
        ProjectileComponent.computeStartOffset(
            true,
            config.getRodTipAttachVertical(),
            config.getRodTipAttachHorizontal(),
            ROD_SHAFT_LENGTH_BLOCKS * config.getRodTipShaftLengthScale(),
            yaw,
            pitch,
            offset
        );

        out.set(look.getPosition()).add(offset);
        out.y += config.getRodTipVerticalLift();
    }

    /**
     * Rod tip for an active fishing line, including a synthetic idle bob that approximates the held
     * rod animation. The server does not expose animated bone positions, so this uses a sine wave.
     */
    public static void getAnchoredRodTipPosition(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishingLineComponent line,
        @Nonnull Vector3d out
    ) {
        getRodTipPosition(playerRef, commandBuffer, out);
        applyRodIdleBob(out, playerRef, commandBuffer, line);
    }

    /**
     * Applies idle bob offset to an already-computed rod tip. No-op when amplitude is zero or the
     * line is not in an idle/reeling hold state.
     */
    public static void applyRodIdleBob(
        @Nonnull Vector3d tip,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishingLineComponent line
    ) {
        FishingModConfig config = FishingModConfig.get();
        float amplitude = config.getRodTipBobAmplitude();
        if (amplitude <= 0.0f || !usesIdleRodBob(line)) {
            return;
        }

        float bobPhaseRadians = resolveRodTipBobPhaseRadians(playerRef, commandBuffer);

        var look = TargetUtil.getLook(playerRef, commandBuffer);
        Vector3d forward = look.getDirection();
        if (forward.lengthSquared() < 1.0e-8) {
            return;
        }
        forward.normalize();

        Vector3d bobDirection = new Vector3d();
        float viewUpWeight = FishingLineMath.clamp(config.getRodTipBobViewUpWeight(), 0.0f, 1.0f);
        if (viewUpWeight > 0.0f) {
            Vector3d worldUp = new Vector3d(0.0, 1.0, 0.0);
            Vector3d right = new Vector3d(forward).cross(worldUp);
            if (right.lengthSquared() < 1.0e-8) {
                right.set(1.0, 0.0, 0.0);
            } else {
                right.normalize();
            }
            Vector3d viewUp = new Vector3d(right).cross(forward);
            if (viewUp.lengthSquared() >= 1.0e-8) {
                viewUp.normalize();
                bobDirection.set(viewUp).mul(viewUpWeight);
            }
        }
        bobDirection.add(0.0, 1.0 - viewUpWeight, 0.0);
        if (bobDirection.lengthSquared() < 1.0e-8) {
            bobDirection.set(0.0, 1.0, 0.0);
        } else {
            bobDirection.normalize();
        }

        float offset = (float) Math.sin(bobPhaseRadians + config.getRodTipBobPhaseOffset() * Math.PI * 2.0) * amplitude;
        tip.add(bobDirection.x * offset, bobDirection.y * offset, bobDirection.z * offset);
    }

    private static boolean usesIdleRodBob(@Nonnull FishingLineComponent line) {
        FishingLinePhase phase = line.getPhase();
        if (phase == FishingLinePhase.INACTIVE
            || phase == FishingLinePhase.RECALLING
            || phase == FishingLinePhase.CASTING) {
            return false;
        }
        return phase == FishingLinePhase.FLOATING
            || phase == FishingLinePhase.REELING
            || line.isReeling();
    }

    /** Phase accumulated while the rod is held; survives cast/recall so bob stays aligned with the client anim. */
    public static float resolveRodTipBobPhaseRadians(
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        FishingRodHoldComponent hold = commandBuffer.getComponent(playerRef, FishingRodHoldComponent.getComponentType());
        return hold != null ? hold.getRodTipBobPhaseRadians() : 0.0f;
    }

    /** Seeds a new hold component from server time (helps first equip align with a time-based client loop). */
    public static float computeWorldTimeBobPhaseRadians(
        @Nonnull World world,
        @Nonnull FishingModConfig config,
        float animSpeed
    ) {
        float seconds = world.getTick() * (world.getTickStepNanos() / 1_000_000_000.0f);
        return (float) (Math.PI * 2.0 * config.getRodTipBobFrequency() * animSpeed * seconds);
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
