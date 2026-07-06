package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class FishShadowSpawnStateComponent implements Component<EntityStore> {
    @Nonnull
    private static volatile ComponentType<EntityStore, FishShadowSpawnStateComponent> componentType;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(FishShadowSpawnStateComponent.class, FishShadowSpawnStateComponent::new);
    }

    @Nonnull
    public static ComponentType<EntityStore, FishShadowSpawnStateComponent> getComponentType() {
        ComponentType<EntityStore, FishShadowSpawnStateComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("FishShadowSpawnStateComponent not registered");
        }
        return type;
    }

    private float spawnTimer;
    private int staggerOffsetTicks;

    public float getSpawnTimer() {
        return spawnTimer;
    }

    public void setSpawnTimer(float spawnTimer) {
        this.spawnTimer = spawnTimer;
    }

    public int getStaggerOffsetTicks() {
        return staggerOffsetTicks;
    }

    public void setStaggerOffsetTicks(int staggerOffsetTicks) {
        this.staggerOffsetTicks = staggerOffsetTicks;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        FishShadowSpawnStateComponent copy = new FishShadowSpawnStateComponent();
        copy.spawnTimer = spawnTimer;
        copy.staggerOffsetTicks = staggerOffsetTicks;
        return copy;
    }
}
