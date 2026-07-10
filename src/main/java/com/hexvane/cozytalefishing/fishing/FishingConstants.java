package com.hexvane.cozytalefishing.fishing;



/** Tunable fishing rod / line constants. */

public final class FishingConstants {

    public static final String BOBBER_PROJECTILE_CONFIG_ID = "Projectile_Config_CozyFishing_Bobber";

    public static final String STRING_SEGMENT_MODEL_ID = "CozyTalesFishing_StringSegment";

    public static final String BOBBER_MODEL_ID = "CozyTalesFishing_Bobber";

    public static final String SPLASH_PARTICLE_SYSTEM_ID = "CozyFishing_Bobber_Splash_System";

    public static final String RIPPLE_PARTICLE_SYSTEM_ID = "CozyFishing_Bobber_Ripple_System";

    /** Vanilla lava landing / splash particles (Block/Lava/Block_Land_Lava_Soft.particlesystem). */
    public static final String LAVA_SPLASH_PARTICLE_SYSTEM_ID = "Block_Land_Lava_Soft";

    /** Vanilla lava surface bubbles for subtle ripples while fighting in lava. */
    public static final String LAVA_RIPPLE_PARTICLE_SYSTEM_ID = "Block_Lava_Bubbles";

    public static final String WOODEN_ROD_ITEM_ID = FishingRodRegistry.WOODEN_ROD_ID;

    /** @deprecated Use {@link FishingRodRegistry#isFishingRod(String)} */
    @Deprecated
    public static final String ROD_ITEM_ID = WOODEN_ROD_ITEM_ID;

    /** @deprecated Use {@link FishingRodRegistry#isFishingRod(String)} */
    @Deprecated
    public static final String FISHING_ROD_ITEM_ID = WOODEN_ROD_ITEM_ID;

    public static final String FISHING_JOURNAL_ITEM_ID = "CozyFishing_Fishing_Journal";

    public static final String FISH_FINDER_ITEM_ID = "CozyFishing_Fish_Finder";

    public static final String ROD_MODEL_PATH = "Items/Fishing/CozyFishingRod.blockymodel";

    /**

     * Enough segments that rope node spacing stays below the fixed visual length of one segment prop

     * ({@link #BASE_SEGMENT_LENGTH} at unit scale). Prop entities do not reliably stretch at runtime.

     */

    public static final int SEGMENT_COUNT = 64;

    public static final int NODE_COUNT = SEGMENT_COUNT + 1;

    /** Fixed world-space length of one segment prop at unit scale (see model HitBox). */

    public static final float BASE_SEGMENT_LENGTH = 0.5f;

    /** Visible segment props per block of line length (2 = twice as dense as one prop per BASE_SEGMENT_LENGTH). */
    public static final int SEGMENT_VISUAL_DENSITY = 3;



    public static final float MAX_CHARGE_SECONDS = 2.0f;

    /** Minimum held charge before a release spawns the bobber. */

    public static final float MIN_CAST_CHARGE_SECONDS = 0.02f;

    public static final float MIN_CAST_BLOCKS = 4.0f;

    public static final float MAX_CAST_BLOCKS = 12.0f;

    /** Scales {@code Projectile_Config_CozyFishing_Bobber} LaunchForce by charge (0 = min, 1 = max). */
    public static final float MIN_CAST_FORCE_MULTIPLIER = 0.30f;

    public static final float MAX_CAST_FORCE_MULTIPLIER = 1.00f;



    public static final float GRAVITY = 6.0f;

    /** Reduced gravity while the bobber is floating so the line does not hang through terrain. */

    public static final float FLOATING_GRAVITY = 1.5f;

    public static final int CONSTRAINT_ITERATIONS = 10;

    public static final int CAST_WHIP_TICKS = 4;

    public static final float CAST_WHIP_IMPULSE = 0.35f;

    /** Rope rest length multiplier relative to tip-to-bobber distance (1 = taut). */
    public static final float ROPE_SLACK_FACTOR = 1.005f;

    /** Pulls floating line nodes toward a straight tip-to-bobber line (reeling uses a higher value). */
    public static final float FLOATING_ROPE_STRAIGHTEN = 0.05f;

    /** Slack while reeling so the line visually tightens toward the rod tip. */
    public static final float REEL_ROPE_SLACK_FACTOR = 1.0f;

    /** Minimum clearance above terrain for simulated line nodes. */

    public static final double GROUND_CLEARANCE = 0.1;



    public static final float BOB_AMPLITUDE = 0.06f;

    public static final float BOB_FREQUENCY = 2.2f;

    /** How far below the water surface the bobber sits while a fish is hooked or being reeled in. */
    public static final float FIGHT_BOBBER_SUBMERGE_OFFSET = 0.20f;

    public static final float GROUND_RECALL_DELAY_SECONDS = 1.5f;



    /**
     * Eye-to-hand attach offset from {@code Projectile_Config_Arrow_Base} SpawnOffset (shortbow FPS hold).
     * Combined with the rod model {@code Tip} bone offset in {@link RodTipUtil}.
     */
    public static final double ATTACH_HORIZONTAL_CENTER = 0.35;

    public static final double ATTACH_VERTICAL_CENTER = -0.25;

    /**
     * Scales parsed {@code Tip}−{@code R-Attachment} shaft length when projecting along look.
     * FPS hold keeps much of the rod behind the camera; 1.0 overshoots the visual tip.
     */
    public static final double ROD_SHAFT_LENGTH_SCALE = 0.70;

    /** Extra world-space Y added after shaft projection (raises anchor vs. lowering from scale trim). */
    public static final double ROD_TIP_VERTICAL_LIFT = 0.8;

    /** Pulls segment i&gt;0 slightly back along its edge so props overlap at rope nodes. */
    public static final float SEGMENT_JOINT_OVERLAP = 0.08f;

    public static final String REEL_IN_LOCAL_SOUND_EVENT_ID = "SFX_CozyFishing_Reel_In_Local";

    /** Below this tip-to-bobber distance, skip sag simulation and pin nodes in a straight line. */
    public static final float SHORT_LINE_STRAIGHTEN_BLOCKS = 4.0f;

    private FishingConstants() {}

}


