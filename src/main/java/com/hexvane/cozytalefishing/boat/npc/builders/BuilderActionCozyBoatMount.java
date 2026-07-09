package com.hexvane.cozytalefishing.boat.npc.builders;

import com.google.gson.JsonElement;
import com.hexvane.cozytalefishing.boat.npc.ActionCozyBoatMount;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.FloatHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import javax.annotation.Nonnull;

public final class BuilderActionCozyBoatMount extends BuilderActionBase {
  private final FloatHolder anchorX = new FloatHolder();
  private final FloatHolder anchorY = new FloatHolder();
  private final FloatHolder anchorZ = new FloatHolder();
  private final StringHolder movementConfig = new StringHolder();

  @Nonnull
  @Override
  public String getShortDescription() {
    return "Enable the player to mount a fishing boat";
  }

  @Nonnull
  @Override
  public String getLongDescription() {
    return getShortDescription();
  }

  @Nonnull
  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Stable;
  }

  public float getAnchorX(@Nonnull BuilderSupport support) {
    return anchorX.get(support.getExecutionContext());
  }

  public float getAnchorY(@Nonnull BuilderSupport support) {
    return anchorY.get(support.getExecutionContext());
  }

  public float getAnchorZ(@Nonnull BuilderSupport support) {
    return anchorZ.get(support.getExecutionContext());
  }

  public String getMovementConfig(@Nonnull BuilderSupport support) {
    return movementConfig.get(support.getExecutionContext());
  }

  @Nonnull
  @Override
  public ActionCozyBoatMount build(@Nonnull BuilderSupport builderSupport) {
    return new ActionCozyBoatMount(this, builderSupport);
  }

  @Override
  public Builder<Action> readConfig(@Nonnull JsonElement data) {
    requireFloat(data, "AnchorX", anchorX, null, BuilderDescriptorState.Stable, "The X anchor pos", null);
    requireFloat(data, "AnchorY", anchorY, null, BuilderDescriptorState.Stable, "The Y anchor pos", null);
    requireFloat(data, "AnchorZ", anchorZ, null, BuilderDescriptorState.Stable, "The Z anchor pos", null);
    requireString(
        data,
        "MovementConfig",
        movementConfig,
        null,
        BuilderDescriptorState.Stable,
        "The MovementConfig to use for this mount",
        null
    );
    return super.readConfig(data);
  }
}
