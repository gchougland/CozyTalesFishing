package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FishShadowAnimations {
    private static final String SWIM_ANIMATION = "Swim";
    private static final String IDLE_ANIMATION = "Idle";

    private FishShadowAnimations() {}

    public static void prepareSpawn(@Nonnull Holder<EntityStore> holder, @Nonnull Model model) {
        prepareMovementAnimation(holder, model, false);
    }

    public static void prepareMovementAnimation(
        @Nonnull Holder<EntityStore> holder,
        @Nonnull Model model,
        boolean useIdleAnimation
    ) {
        String animation = resolveMovementAnimation(model, useIdleAnimation);
        if (animation == null) {
            return;
        }

        holder.ensureComponent(ActiveAnimationComponent.getComponentType());
        ActiveAnimationComponent active = holder.getComponent(ActiveAnimationComponent.getComponentType());
        if (active != null) {
            active.setPlayingAnimation(AnimationSlot.Movement, animation);
        }
    }

    public static void playSwim(@Nonnull Ref<EntityStore> shadowRef, @Nonnull ComponentAccessor<EntityStore> accessor) {
        playMovementAnimation(shadowRef, accessor, false);
    }

    public static void playMovementAnimation(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull ComponentAccessor<EntityStore> accessor,
        boolean useIdleAnimation
    ) {
        if (!entityRef.isValid()) {
            return;
        }

        var modelComponent = accessor.getComponent(entityRef, ModelComponent.getComponentType());
        if (modelComponent == null) {
            return;
        }

        String animation = resolveMovementAnimation(modelComponent.getModel(), useIdleAnimation);
        if (animation == null) {
            return;
        }

        AnimationUtils.playAnimation(entityRef, AnimationSlot.Movement, animation, true, accessor);
    }

    @Nullable
    private static String resolveMovementAnimation(@Nonnull Model model, boolean useIdleAnimation) {
        String preferred = useIdleAnimation ? IDLE_ANIMATION : SWIM_ANIMATION;
        if (model.getAnimationSetMap().containsKey(preferred)) {
            return preferred;
        }

        String fallback = useIdleAnimation ? SWIM_ANIMATION : IDLE_ANIMATION;
        if (model.getAnimationSetMap().containsKey(fallback)) {
            return fallback;
        }

        return null;
    }
}
