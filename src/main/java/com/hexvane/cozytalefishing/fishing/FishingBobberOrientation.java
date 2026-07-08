package com.hexvane.cozytalefishing.fishing;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/** Keeps the bobber prop upright so the hook always points down. */
public final class FishingBobberOrientation {
    private static final Rotation3f UPRIGHT_ROTATION = new Rotation3f(0.0f, 0.0f, 0.0f);

    private FishingBobberOrientation() {}

    public static void applyUpright(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> bobberRef
    ) {
        TransformComponent transform = commandBuffer.getComponent(bobberRef, TransformComponent.getComponentType());
        if (transform != null) {
            transform.setRotation(UPRIGHT_ROTATION);
        }
        HeadRotation headRotation = commandBuffer.getComponent(bobberRef, HeadRotation.getComponentType());
        if (headRotation != null) {
            headRotation.setRotation(UPRIGHT_ROTATION);
        }
    }
}
