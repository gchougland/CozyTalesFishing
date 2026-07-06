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

public final class FishShadowAnimations {
    private static final String SWIM_ANIMATION = "Swim";

    private FishShadowAnimations() {}

    public static void prepareSpawn(@Nonnull Holder<EntityStore> holder, @Nonnull Model model) {
        holder.ensureComponent(ActiveAnimationComponent.getComponentType());
        if (!model.getAnimationSetMap().containsKey(SWIM_ANIMATION)) {
            return;
        }
        ActiveAnimationComponent active = holder.getComponent(ActiveAnimationComponent.getComponentType());
        if (active != null) {
            active.setPlayingAnimation(AnimationSlot.Movement, SWIM_ANIMATION);
        }
    }

    public static void playSwim(@Nonnull Ref<EntityStore> shadowRef, @Nonnull ComponentAccessor<EntityStore> accessor) {
        if (!shadowRef.isValid()) {
            return;
        }
        var modelComponent = accessor.getComponent(shadowRef, ModelComponent.getComponentType());
        if (modelComponent == null || !modelComponent.getModel().getAnimationSetMap().containsKey(SWIM_ANIMATION)) {
            return;
        }
        AnimationUtils.playAnimation(shadowRef, AnimationSlot.Movement, SWIM_ANIMATION, true, accessor);
    }
}
