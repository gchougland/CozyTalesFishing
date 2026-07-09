package com.hexvane.cozytalefishing.bobber;

import com.hexvane.cozytalefishing.fish.FishingModConfig;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import java.util.List;
import javax.annotation.Nonnull;

public final class BobberEffects {
    public static final BobberEffects NONE = new BobberEffects(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0);

    private final float trashChanceBonus;
    private final float treasureChanceBonus;
    private final float sizeSkewExponent;
    private final float fleeSpeedMultiplier;
    private final float reelSpeedMultiplier;
    private final float visionRangeMultiplier;
    private final int activeBobberCount;

    public BobberEffects(
        float trashChanceBonus,
        float treasureChanceBonus,
        float sizeSkewExponent,
        float fleeSpeedMultiplier,
        float reelSpeedMultiplier,
        float visionRangeMultiplier,
        int activeBobberCount
    ) {
        this.trashChanceBonus = trashChanceBonus;
        this.treasureChanceBonus = treasureChanceBonus;
        this.sizeSkewExponent = sizeSkewExponent;
        this.fleeSpeedMultiplier = fleeSpeedMultiplier;
        this.reelSpeedMultiplier = reelSpeedMultiplier;
        this.visionRangeMultiplier = visionRangeMultiplier;
        this.activeBobberCount = activeBobberCount;
    }

    @Nonnull
    public static BobberEffects fromStacks(@Nonnull List<ItemStack> activeBobbers, @Nonnull FishingModConfig config) {
        if (activeBobbers.isEmpty()) {
            return NONE;
        }

        float trashBonus = 0.0f;
        float treasureBonus = 0.0f;
        float sizeExponent = config.getBobberDefaultSizeSkewExponent();
        float fleeMultiplier = 1.0f;
        float reelMultiplier = 1.0f;
        float visionMultiplier = 1.0f;

        for (ItemStack stack : activeBobbers) {
            BobberRegistry.BobberDefinition definition = BobberRegistry.get(stack.getItemId());
            if (definition == null) {
                continue;
            }
            switch (definition.type()) {
                case TRASH -> trashBonus += config.getBobberTrashChanceBonus();
                case TREASURE -> treasureBonus += config.getBobberTreasureChanceBonus();
                case QUALITY -> sizeExponent = Math.min(sizeExponent, config.getBobberQualitySizeSkewExponent());
                case TRAP -> {
                    fleeMultiplier *= config.getBobberTrapFleeSpeedMultiplier();
                    reelMultiplier *= config.getBobberTrapReelSpeedMultiplier();
                }
                case SPINNER -> visionMultiplier *= config.getBobberSpinnerVisionMultiplier();
                case DECORATED_SPINNER -> visionMultiplier *= config.getBobberDecoratedSpinnerVisionMultiplier();
            }
        }

        return new BobberEffects(
            trashBonus,
            treasureBonus,
            sizeExponent,
            fleeMultiplier,
            reelMultiplier,
            visionMultiplier,
            activeBobbers.size()
        );
    }

    @Nonnull
    public static BobberEffects fromRod(@Nonnull ItemStack rodStack) {
        return fromStacks(BobberLoadoutService.getActiveBobbers(rodStack), FishingModConfig.get());
    }

    public boolean isEmpty() {
        return activeBobberCount == 0;
    }

    public float getTrashChanceBonus() {
        return trashChanceBonus;
    }

    public float getTreasureChanceBonus() {
        return treasureChanceBonus;
    }

    public float getSizeSkewExponent() {
        return sizeSkewExponent;
    }

    public float getFleeSpeedMultiplier() {
        return fleeSpeedMultiplier;
    }

    public float getReelSpeedMultiplier() {
        return reelSpeedMultiplier;
    }

    public float getVisionRangeMultiplier() {
        return visionRangeMultiplier;
    }
}
