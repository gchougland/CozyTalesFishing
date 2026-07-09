package com.hexvane.cozytalefishing.bobber;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BobberVisualService {
    private BobberVisualService() {}

    @Nullable
    public static String resolveProjectileModelId(@Nullable ItemStack primaryBobber) {
        if (primaryBobber == null || ItemStack.isEmpty(primaryBobber)) {
            return BobberConstants.DEFAULT_PROJECTILE_MODEL_ID;
        }
        BobberRegistry.BobberDefinition definition = BobberRegistry.get(primaryBobber.getItemId());
        if (definition == null) {
            return BobberConstants.DEFAULT_PROJECTILE_MODEL_ID;
        }
        return definition.projectileModelId();
    }

    public static void applyToEntity(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> bobberRef,
        @Nullable ItemStack primaryBobber
    ) {
        String modelAssetId = resolveProjectileModelId(primaryBobber);
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        if (modelAsset == null) {
            modelAsset = ModelAsset.getAssetMap().getAsset(BobberConstants.DEFAULT_PROJECTILE_MODEL_ID);
        }
        if (modelAsset == null) {
            return;
        }

        Model model = Model.createUnitScaleModel(modelAsset);
        store.putComponent(bobberRef, ModelComponent.getComponentType(), new ModelComponent(model));
        store.putComponent(bobberRef, PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
    }
}
