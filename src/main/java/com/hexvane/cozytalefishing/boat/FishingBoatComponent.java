package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Marks a rideable fishing boat entity and stores break-drop / physics state. */
public final class FishingBoatComponent implements Component<EntityStore> {
  public static final String DEFAULT_SOURCE_ITEM = "CozyFishing_Boat";

  public static final BuilderCodec<FishingBoatComponent> CODEC =
      BuilderCodec.builder(FishingBoatComponent.class, FishingBoatComponent::new)
          .append(new KeyedCodec<>("SourceItem", Codec.STRING), (o, v) -> o.sourceItem = v, o -> o.sourceItem)
          .add()
          .build();

  @Nullable
  private static volatile ComponentType<EntityStore, FishingBoatComponent> componentType;

  public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
    componentType = registry.registerComponent(FishingBoatComponent.class, "CozyFishingBoat", CODEC);
  }

  @Nonnull
  public static ComponentType<EntityStore, FishingBoatComponent> getComponentType() {
    ComponentType<EntityStore, FishingBoatComponent> type = componentType;
    if (type == null) {
      throw new IllegalStateException("FishingBoatComponent not registered");
    }
    return type;
  }

  private int numberOfHits;
  @Nullable private Instant lastHit;
  private String sourceItem = DEFAULT_SOURCE_ITEM;
  private float floatBobPhase;
  private double lastX = Double.NaN;
  private double lastY = Double.NaN;
  private double lastZ = Double.NaN;
  private double lastWaterX = Double.NaN;
  private double lastWaterZ = Double.NaN;
  private transient boolean suppressBlockPlacement;

  private FishingBoatComponent() {}

  public FishingBoatComponent(@Nullable String sourceItem) {
    if (sourceItem != null && !sourceItem.isEmpty()) {
      this.sourceItem = sourceItem;
    }
  }

  public int getNumberOfHits() {
    return numberOfHits;
  }

  public void setNumberOfHits(int numberOfHits) {
    this.numberOfHits = numberOfHits;
  }

  @Nullable
  public Instant getLastHit() {
    return lastHit;
  }

  public void setLastHit(@Nullable Instant lastHit) {
    this.lastHit = lastHit;
  }

  @Nonnull
  public String getSourceItem() {
    return sourceItem;
  }

  public float getFloatBobPhase() {
    return floatBobPhase;
  }

  public void setFloatBobPhase(float floatBobPhase) {
    this.floatBobPhase = floatBobPhase;
  }

  public double getLastX() {
    return lastX;
  }

  public void setLastX(double lastX) {
    this.lastX = lastX;
  }

  public double getLastY() {
    return lastY;
  }

  public void setLastY(double lastY) {
    this.lastY = lastY;
  }

  public double getLastZ() {
    return lastZ;
  }

  public void setLastZ(double lastZ) {
    this.lastZ = lastZ;
  }

  public double getLastWaterX() {
    return lastWaterX;
  }

  public void setLastWaterX(double lastWaterX) {
    this.lastWaterX = lastWaterX;
  }

  public double getLastWaterZ() {
    return lastWaterZ;
  }

  public void setLastWaterZ(double lastWaterZ) {
    this.lastWaterZ = lastWaterZ;
  }

  public boolean hasLastPosition() {
    return !Double.isNaN(lastX) && !Double.isNaN(lastY) && !Double.isNaN(lastZ);
  }

  public boolean hasLastWaterPosition() {
    return !Double.isNaN(lastWaterX) && !Double.isNaN(lastWaterZ);
  }

  public boolean isSuppressBlockPlacement() {
    return suppressBlockPlacement;
  }

  public void setSuppressBlockPlacement(boolean suppressBlockPlacement) {
    this.suppressBlockPlacement = suppressBlockPlacement;
  }

  @Nonnull
  @Override
  public Component<EntityStore> clone() {
    FishingBoatComponent copy = new FishingBoatComponent(sourceItem);
    copy.numberOfHits = numberOfHits;
    copy.lastHit = lastHit;
    copy.floatBobPhase = floatBobPhase;
    copy.lastX = lastX;
    copy.lastY = lastY;
    copy.lastZ = lastZ;
    copy.lastWaterX = lastWaterX;
    copy.lastWaterZ = lastWaterZ;
    return copy;
  }
}
