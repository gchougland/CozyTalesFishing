package com.hexvane.cozytalefishing.treasure;

import com.hexvane.cozytalefishing.fish.FishCatchRecordComponent;
import com.hexvane.cozytalefishing.fish.FishSpeciesAsset;
import com.hexvane.cozytalefishing.fish.FishSpeciesDisplayNames;
import com.hexvane.cozytalefishing.fish.FishSpeciesRegistry;
import com.hypixel.hytale.server.core.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;

public final class MessageInBottleHintService {
    private MessageInBottleHintService() {}

    @Nonnull
    public static Message useBottle(@Nonnull FishCatchRecordComponent records) {
        List<FishSpeciesAsset> eligible = getEligibleHintSpecies(records);
        if (!eligible.isEmpty()) {
            FishSpeciesAsset species = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
            records.hint(species.getId());
            return Message.translation("server.cozytalefishing.bottle.hint_unlocked")
                .param("fish", FishSpeciesDisplayNames.resolve(species));
        }
        return rollFlavorMessage();
    }

    @Nonnull
    private static List<FishSpeciesAsset> getEligibleHintSpecies(@Nonnull FishCatchRecordComponent records) {
        List<FishSpeciesAsset> eligible = new ArrayList<>();
        for (FishSpeciesAsset species : FishSpeciesRegistry.getJournalSpecies()) {
            String speciesId = species.getId();
            if (!records.isDiscovered(speciesId) && !records.isHinted(speciesId)) {
                eligible.add(species);
            }
        }
        return eligible;
    }

    @Nonnull
    public static Message rollFlavorMessage() {
        int index = ThreadLocalRandom.current().nextInt(1, MessageInBottleConstants.FLAVOR_MESSAGE_COUNT + 1);
        return Message.translation(MessageInBottleConstants.FLAVOR_MESSAGE_KEY_PREFIX + index);
    }
}
