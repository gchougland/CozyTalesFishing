package com.hexvane.cozytalefishing.journal;

import com.hexvane.cozytalefishing.fish.FishCatchRecordComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Journal visibility tier for a fish species entry. */
public enum JournalEntryState {
    UNDISCOVERED,
    HINTED,
    DISCOVERED;

    @Nonnull
    public static JournalEntryState fromRecords(@Nullable FishCatchRecordComponent records, @Nonnull String speciesId) {
        if (records == null) {
            return UNDISCOVERED;
        }
        if (records.isDiscovered(speciesId)) {
            return DISCOVERED;
        }
        if (records.isHinted(speciesId)) {
            return HINTED;
        }
        return UNDISCOVERED;
    }
}
