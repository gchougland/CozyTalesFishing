package com.hexvane.cozytalefishing.journal;

import com.hexvane.cozytalefishing.fish.FishCatchRecordComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Journal visibility tier for a fish species entry. */
public enum JournalEntryState {
    UNDISCOVERED,
    HINTED,
    DISCOVERED;

    /** Lower values appear first in the journal species grid. */
    public int journalSortOrder() {
        return switch (this) {
            case DISCOVERED -> 0;
            case HINTED -> 1;
            case UNDISCOVERED -> 2;
        };
    }

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
