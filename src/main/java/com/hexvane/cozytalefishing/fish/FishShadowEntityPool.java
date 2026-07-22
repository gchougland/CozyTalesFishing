package com.hexvane.cozytalefishing.fish;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;
import org.joml.Vector3d;

public final class FishShadowEntityPool {
    private FishShadowEntityPool() {}

    @Nullable
    public static Ref<EntityStore> spawnShadow(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull FishSpeciesAsset species,
        @Nonnull WaterBodyType waterBodyType,
        @Nullable String columnFluidAssetId,
        @Nonnull Vector3d position,
        float yawRadians,
        float scale
    ) {
        FishShadowType shadowType = species.pickShadowTypeForSpawn(ThreadLocalRandom.current());
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(shadowType.getModelAssetId());
        if (modelAsset == null) {
            return null;
        }

        Model model = Model.createScaledModel(modelAsset, scale);
        Rotation3f rotation = new Rotation3f(0.0f, yawRadians, 0.0f);

        var holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(commandBuffer.getExternalData().takeNextNetworkId()));
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(new Vector3d(position), rotation));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
        holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE);
        holder.ensureComponent(UUIDComponent.getComponentType());

        FishShadowComponent shadow = new FishShadowComponent();
        shadow.setSpeciesId(species.getId());
        shadow.setShadowType(shadowType);
        shadow.setWaterBodyType(waterBodyType);
        shadow.setColumnFluidAssetId(columnFluidAssetId);
        shadow.setBaseScale(scale);
        shadow.setCurrentScale(scale);
        shadow.setWanderDirection(yawRadians);
        shadow.setWanderTimer(3.0f + (float) Math.random() * 5.0f);
        FishShadowAnimations.prepareSpawn(holder, model);
        holder.addComponent(FishShadowComponent.getComponentType(), shadow);

        Ref<EntityStore> shadowRef = commandBuffer.addEntity(holder, AddReason.SPAWN);
        if (shadowRef != null) {
            Ref<EntityStore> deferredRef = shadowRef;
            commandBuffer.run(store -> FishShadowAnimations.playSwim(deferredRef, store));
        }
        return shadowRef;
    }

    @Nullable
    public static Ref<EntityStore> spawnShadow(
        @Nonnull Store<EntityStore> store,
        @Nonnull FishSpeciesAsset species,
        @Nonnull WaterBodyType waterBodyType,
        @Nullable String columnFluidAssetId,
        @Nonnull Vector3d position,
        float yawRadians,
        float scale
    ) {
        FishShadowType shadowType = species.pickShadowTypeForSpawn(ThreadLocalRandom.current());
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(shadowType.getModelAssetId());
        if (modelAsset == null) {
            return null;
        }

        Model model = Model.createScaledModel(modelAsset, scale);
        Rotation3f rotation = new Rotation3f(0.0f, yawRadians, 0.0f);

        var holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(new Vector3d(position), rotation));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
        holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE);
        holder.ensureComponent(UUIDComponent.getComponentType());

        FishShadowComponent shadow = new FishShadowComponent();
        shadow.setSpeciesId(species.getId());
        shadow.setShadowType(shadowType);
        shadow.setWaterBodyType(waterBodyType);
        shadow.setColumnFluidAssetId(columnFluidAssetId);
        shadow.setBaseScale(scale);
        shadow.setCurrentScale(scale);
        shadow.setWanderDirection(yawRadians);
        shadow.setWanderTimer(3.0f + (float) Math.random() * 5.0f);
        FishShadowAnimations.prepareSpawn(holder, model);
        holder.addComponent(FishShadowComponent.getComponentType(), shadow);

        Ref<EntityStore> shadowRef = store.addEntity(holder, AddReason.SPAWN);
        if (shadowRef != null) {
            FishShadowAnimations.playSwim(shadowRef, store);
        }
        return shadowRef;
    }

    public static void updateTransform(
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull Ref<EntityStore> shadowRef,
        @Nonnull Vector3d position,
        float yawRadians,
        float scale
    ) {
        if (!shadowRef.isValid()) {
            return;
        }
        Rotation3f rotation = new Rotation3f(0.0f, yawRadians, 0.0f);
        TransformComponent transform = commandBuffer.getComponent(shadowRef, TransformComponent.getComponentType());
        if (transform != null) {
            transform.setPosition(position);
            transform.setRotation(rotation);
        }
        HeadRotation headRotation = commandBuffer.getComponent(shadowRef, HeadRotation.getComponentType());
        if (headRotation != null) {
            headRotation.setRotation(rotation);
        }
        PersistentModel persistentModel = commandBuffer.getComponent(shadowRef, PersistentModel.getComponentType());
        if (persistentModel != null) {
            Model.ModelReference reference = persistentModel.getModelReference();
            FishShadowComponent shadow = commandBuffer.getComponent(shadowRef, FishShadowComponent.getComponentType());
            String modelId =
                shadow != null
                    ? shadow.getShadowType().getModelAssetId()
                    : reference.getModelAssetId();
            persistentModel.setModelReference(new Model.ModelReference(modelId, scale, reference.getRandomAttachmentIds(), reference.isStaticModel()));
        }
        ModelComponent modelComponent = commandBuffer.getComponent(shadowRef, ModelComponent.getComponentType());
        if (modelComponent != null && persistentModel != null) {
            Model updated = persistentModel.getModelReference().toModel();
            if (updated != null) {
                commandBuffer.putComponent(shadowRef, ModelComponent.getComponentType(), new ModelComponent(updated));
            }
        } else if (modelComponent != null) {
            FishShadowComponent shadow = commandBuffer.getComponent(shadowRef, FishShadowComponent.getComponentType());
            if (shadow != null) {
                ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(shadow.getShadowType().getModelAssetId());
                if (modelAsset != null) {
                    commandBuffer.putComponent(
                        shadowRef,
                        ModelComponent.getComponentType(),
                        new ModelComponent(Model.createScaledModel(modelAsset, scale))
                    );
                }
            }
        }
        FishShadowComponent shadowState = commandBuffer.getComponent(shadowRef, FishShadowComponent.getComponentType());
        if (shadowState != null) {
            shadowState.setCurrentScale(scale);
        }
    }

    public static void despawn(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> shadowRef) {
        if (shadowRef.isValid()) {
            commandBuffer.removeEntity(shadowRef, RemoveReason.REMOVE);
        }
    }
}
