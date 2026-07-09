package com.hexvane.cozytalefishing.boat;

import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.builtin.adventure.camera.asset.viewbobbing.ViewBobbing;
import com.hypixel.hytale.protocol.MovementType;
import com.hypixel.hytale.protocol.UpdateType;
import com.hypixel.hytale.protocol.packets.assets.UpdateViewBobbing;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Disables horse-style camera bob while riding a fishing boat, without affecting other mounts. */
public final class FishingBoatMountViewBobbingHelper {
  private FishingBoatMountViewBobbingHelper() {}

  public static void disableForPlayer(@Nonnull PlayerRef playerRef) {
    sendMountProfiles(playerRef, getNoneProfile());
  }

  public static void restoreForPlayer(@Nonnull PlayerRef playerRef) {
    var assetMap = viewBobbingAssetMap();
    if (assetMap == null) {
      return;
    }

    var mounting = assetMap.getAsset(MovementType.Mounting);
    var sprintMounting = assetMap.getAsset(MovementType.SprintMounting);
    if (mounting == null || sprintMounting == null) {
      return;
    }

    var profiles = new EnumMap<MovementType, com.hypixel.hytale.protocol.ViewBobbing>(MovementType.class);
    profiles.put(MovementType.Mounting, mounting.toPacket());
    profiles.put(MovementType.SprintMounting, sprintMounting.toPacket());
    sendProfiles(playerRef, profiles);
  }

  @Nullable
  private static com.hypixel.hytale.protocol.ViewBobbing getNoneProfile() {
    var assetMap = viewBobbingAssetMap();
    if (assetMap == null) {
      return null;
    }

    var none = assetMap.getAsset(MovementType.None);
    return none != null ? none.toPacket() : null;
  }

  private static void sendMountProfiles(
      @Nonnull PlayerRef playerRef,
      @Nullable com.hypixel.hytale.protocol.ViewBobbing profile
  ) {
    if (profile == null) {
      return;
    }

    var profiles = new EnumMap<MovementType, com.hypixel.hytale.protocol.ViewBobbing>(MovementType.class);
    profiles.put(MovementType.Mounting, profile);
    profiles.put(MovementType.SprintMounting, profile);
    sendProfiles(playerRef, profiles);
  }

  private static void sendProfiles(
      @Nonnull PlayerRef playerRef,
      @Nonnull Map<MovementType, com.hypixel.hytale.protocol.ViewBobbing> profiles
  ) {
    var packet = new UpdateViewBobbing();
    packet.type = UpdateType.AddOrUpdate;
    packet.profiles = new EnumMap<>(MovementType.class);
    packet.profiles.putAll(profiles);
    playerRef.getPacketHandler().write(packet);
  }

  @Nullable
  private static AssetMap<MovementType, ViewBobbing> viewBobbingAssetMap() {
    AssetStore<?, ?, ?> store = AssetRegistry.getAssetStore(ViewBobbing.class);
    if (store == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    AssetMap<MovementType, ViewBobbing> assetMap =
        (AssetMap<MovementType, ViewBobbing>) store.getAssetMap();
    return assetMap;
  }
}
