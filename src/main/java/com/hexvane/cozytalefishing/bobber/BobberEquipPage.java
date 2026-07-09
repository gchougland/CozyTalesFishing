package com.hexvane.cozytalefishing.bobber;

import com.hexvane.cozytalefishing.journal.CozyInteractiveCustomUIPage;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

public final class BobberEquipPage extends CozyInteractiveCustomUIPage<BobberEquipPage.PageData> {
    private static final String SLOT_ROW = "#SlotRow";
    private static final String PICKER_LIST = "#PickerList";
    private static final String SLOT_TEMPLATE = "CozyTalesFishing/BobberSlotButton.ui";
    private static final String PICKER_TEMPLATE = "CozyTalesFishing/BobberPickerRow.ui";
    private static final String PICKER_EMPTY_TEMPLATE = "CozyTalesFishing/BobberPickerEmptyMessage.ui";

    private enum ViewMode {
        SLOTS,
        PICKER
    }

    private static final String SLOT_BUTTON = " #SlotButton";
    private static final String SLOT_ICON = SLOT_BUTTON + " #SlotIcon";
    private static final String SLOT_LABEL = SLOT_BUTTON + " #SlotLabel";
    private static final String SLOT_DURABILITY = SLOT_BUTTON + " #DurabilityLabel";
    private static final String SLOT_BROKEN = SLOT_BUTTON + " #BrokenLabel";

    private boolean templateAppended;
    @Nonnull
    private final ItemContainer rodContainer;
    private final short rodSlot;
    @Nonnull
    private ViewMode viewMode = ViewMode.SLOTS;
    private int pickerSlotIndex = -1;

    public BobberEquipPage(@Nonnull PlayerRef playerRef, @Nonnull ItemContext rodContext) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.rodContainer = rodContext.getContainer();
        this.rodSlot = rodContext.getSlot();
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        if (!templateAppended) {
            commandBuilder.append("CozyTalesFishing/BobberEquipPage.ui");
            templateAppended = true;
        }

        ItemStack rodStack = rodContainer.getItemStack(rodSlot);
        int slotCount = BobberLoadoutService.getSlotCount(rodStack.getItemId());
        boolean hasSlots = slotCount > 0;
        boolean showSlots = hasSlots && viewMode == ViewMode.SLOTS;
        boolean showPicker = hasSlots && viewMode == ViewMode.PICKER;

        commandBuilder.set("#PageTitle.TextSpans", Message.translation("server.cozytalefishing.bobber.title"));
        commandBuilder.set(
            "#Subtitle.TextSpans",
            Message.translation("server.cozytalefishing.bobber.subtitle").param("count", slotCount)
        );
        commandBuilder.set("#NoSlotsPanel.Visible", !hasSlots);
        commandBuilder.set("#SlotsPanel.Visible", showSlots);
        commandBuilder.set("#PickerPanel.Visible", showPicker);
        commandBuilder.set("#BackButton.Visible", showPicker);
        commandBuilder.set("#BackButton.TextSpans", Message.translation("server.cozytalefishing.bobber.back"));
        commandBuilder.set(
            "#NoSlotsMessage.TextSpans",
            Message.translation("server.cozytalefishing.bobber.no_slots")
        );

        if (showSlots) {
            buildSlots(commandBuilder, eventBuilder, rodStack, slotCount);
        }
        if (showPicker) {
            buildPicker(commandBuilder, eventBuilder, store, ref, rodStack);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if ("SelectSlot".equals(data.action)) {
            pickerSlotIndex = data.slotIndex;
            viewMode = ViewMode.PICKER;
        } else if ("BackToSlots".equals(data.action)) {
            viewMode = ViewMode.SLOTS;
            pickerSlotIndex = -1;
        } else if ("PickBobber".equals(data.action)) {
            handlePickBobber(ref, store, data.inventorySlot);
            viewMode = ViewMode.SLOTS;
            pickerSlotIndex = -1;
        } else if ("ClearSlot".equals(data.action)) {
            handleClearSlot(ref, store);
            viewMode = ViewMode.SLOTS;
            pickerSlotIndex = -1;
        }

        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        build(ref, commandBuilder, eventBuilder, store);
        sendUpdate(commandBuilder, eventBuilder, false);
    }

    private void buildSlots(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull ItemStack rodStack,
        int slotCount
    ) {
        commandBuilder.clear(SLOT_ROW);
        ItemStack[] equipped = BobberLoadoutService.getEquippedSlots(rodStack);
        for (int i = 0; i < slotCount; i++) {
            String selector = SLOT_ROW + "[" + i + "]";
            String buttonSelector = selector + SLOT_BUTTON;
            commandBuilder.append(SLOT_ROW, SLOT_TEMPLATE);
            ItemStack bobber = i < equipped.length ? equipped[i] : null;
            boolean occupied = !ItemStack.isEmpty(bobber);
            if (occupied) {
                commandBuilder.set(selector + SLOT_ICON + ".ItemId", bobber.getItemId());
                commandBuilder.set(selector + SLOT_LABEL + ".TextSpans", bobber.getDisplayName());
                setBobberConditionLabels(commandBuilder, selector, bobber);
            } else {
                commandBuilder.clear(selector + SLOT_ICON);
                commandBuilder.set(
                    selector + SLOT_LABEL + ".TextSpans",
                    Message.translation("server.cozytalefishing.bobber.empty_slot").param("number", i + 1)
                );
                commandBuilder.set(selector + SLOT_DURABILITY + ".Text", "");
                commandBuilder.set(selector + SLOT_DURABILITY + ".Visible", true);
                commandBuilder.set(selector + SLOT_BROKEN + ".Visible", false);
            }

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                buttonSelector,
                EventData.of("Action", "SelectSlot").append("SlotIndex", Integer.toString(i)),
                false
            );
        }
    }

    private static void setBobberConditionLabels(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String slotSelector,
        @Nonnull ItemStack bobber
    ) {
        if (bobber.isBroken()) {
            commandBuilder.set(slotSelector + SLOT_DURABILITY + ".Visible", false);
            commandBuilder.set(slotSelector + SLOT_DURABILITY + ".Text", "");
            commandBuilder.set(slotSelector + SLOT_BROKEN + ".Visible", true);
            commandBuilder.set(
                slotSelector + SLOT_BROKEN + ".TextSpans",
                Message.translation("server.cozytalefishing.bobber.broken")
            );
            return;
        }

        commandBuilder.set(slotSelector + SLOT_BROKEN + ".Visible", false);
        commandBuilder.set(slotSelector + SLOT_DURABILITY + ".Visible", true);
        if (bobber.getMaxDurability() > 0.0) {
            int percent = (int) Math.round((bobber.getDurability() / bobber.getMaxDurability()) * 100.0);
            commandBuilder.set(
                slotSelector + SLOT_DURABILITY + ".TextSpans",
                Message.translation("server.cozytalefishing.bobber.durability_percent").param("percent", percent)
            );
        } else {
            commandBuilder.set(slotSelector + SLOT_DURABILITY + ".Text", "");
        }
    }

    private static void setPickerConditionLabels(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String rowSelector,
        @Nonnull ItemStack bobber
    ) {
        if (bobber.isBroken()) {
            commandBuilder.set(rowSelector + " #PickerDurability.Visible", false);
            commandBuilder.set(rowSelector + " #PickerDurability.Text", "");
            commandBuilder.set(rowSelector + " #PickerBroken.Visible", true);
            commandBuilder.set(
                rowSelector + " #PickerBroken.TextSpans",
                Message.translation("server.cozytalefishing.bobber.broken")
            );
            return;
        }

        commandBuilder.set(rowSelector + " #PickerBroken.Visible", false);
        commandBuilder.set(rowSelector + " #PickerDurability.Visible", true);
        if (bobber.getMaxDurability() > 0.0) {
            int percent = (int) Math.round((bobber.getDurability() / bobber.getMaxDurability()) * 100.0);
            commandBuilder.set(
                rowSelector + " #PickerDurability.TextSpans",
                Message.translation("server.cozytalefishing.bobber.durability_percent").param("percent", percent)
            );
        } else {
            commandBuilder.set(rowSelector + " #PickerDurability.Text", "");
        }
    }

    private void buildPicker(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull ItemStack rodStack
    ) {
        commandBuilder.set(
            "#PickerTitle.TextSpans",
            Message.translation("server.cozytalefishing.bobber.picker_title").param("number", pickerSlotIndex + 1)
        );
        commandBuilder.clear(PICKER_LIST);

        int row = 0;
        String clearSelector = PICKER_LIST + "[" + row + "]";
        commandBuilder.append(PICKER_LIST, PICKER_TEMPLATE);
        commandBuilder.clear(clearSelector + " #PickerIcon");
        commandBuilder.set(
            clearSelector + " #PickerName.TextSpans",
            Message.translation("server.cozytalefishing.bobber.clear_slot")
        );
        commandBuilder.set(clearSelector + " #PickerDurability.Visible", true);
        commandBuilder.set(clearSelector + " #PickerDurability.Text", "");
        commandBuilder.set(clearSelector + " #PickerBroken.Visible", false);
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            clearSelector,
            EventData.of("Action", "ClearSlot"),
            false
        );
        row++;

        ItemContainer inventory = InventoryComponent.getCombined(store, ref, InventoryComponent.EVERYTHING);
        List<Short> bobberSlots = new ArrayList<>();
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (!ItemStack.isEmpty(stack) && BobberRegistry.isBobber(stack.getItemId())) {
                bobberSlots.add(slot);
            }
        }

        for (short inventorySlot : bobberSlots) {
            ItemStack stack = inventory.getItemStack(inventorySlot);
            String selector = PICKER_LIST + "[" + row + "]";
            commandBuilder.append(PICKER_LIST, PICKER_TEMPLATE);
            commandBuilder.set(selector + " #PickerIcon.ItemId", stack.getItemId());
            commandBuilder.set(selector + " #PickerName.TextSpans", stack.getDisplayName());
            setPickerConditionLabels(commandBuilder, selector, stack);
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                EventData.of("Action", "PickBobber").append("InventorySlot", Short.toString(inventorySlot)),
                false
            );
            row++;
        }

        if (bobberSlots.isEmpty()) {
            String emptySelector = PICKER_LIST + "[" + row + "]";
            commandBuilder.append(PICKER_LIST, PICKER_EMPTY_TEMPLATE);
            commandBuilder.set(
                emptySelector + " #EmptyMessage.TextSpans",
                Message.translation("server.cozytalefishing.bobber.no_bobbers_in_inventory")
            );
        }

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BackButton",
            EventData.of("Action", "BackToSlots"),
            false
        );
    }

    private void handlePickBobber(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, short inventorySlot) {
        if (pickerSlotIndex < 0 || !ref.isValid()) {
            return;
        }
        ItemContainer inventory = InventoryComponent.getCombined(store, ref, InventoryComponent.EVERYTHING);
        ItemStack bobberStack = inventory.getItemStack(inventorySlot);
        if (ItemStack.isEmpty(bobberStack) || !BobberRegistry.isBobber(bobberStack.getItemId())) {
            return;
        }
        ItemStack rodStack = rodContainer.getItemStack(rodSlot);
        ItemContext rodContext = new ItemContext(rodContainer, rodSlot, rodStack);
        ItemContext bobberContext = new ItemContext(inventory, inventorySlot, bobberStack);
        BobberLoadoutService.equipBobber(store, ref, rodContext, pickerSlotIndex, bobberContext);
    }

    private void handleClearSlot(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (pickerSlotIndex < 0 || !ref.isValid()) {
            return;
        }
        ItemStack rodStack = rodContainer.getItemStack(rodSlot);
        ItemContext rodContext = new ItemContext(rodContainer, rodSlot, rodStack);
        ItemContainer inventory = InventoryComponent.getCombined(store, ref, InventoryComponent.EVERYTHING);
        BobberLoadoutService.clearSlot(store, ref, rodContext, pickerSlotIndex, inventory);
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC =
            BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .add()
                .append(
                    new KeyedCodec<>("SlotIndex", Codec.STRING),
                    (d, v) -> d.slotIndex = v == null || v.isBlank() ? -1 : Integer.parseInt(v),
                    d -> d.slotIndex < 0 ? null : Integer.toString(d.slotIndex)
                )
                .add()
                .append(
                    new KeyedCodec<>("InventorySlot", Codec.STRING),
                    (d, v) -> d.inventorySlot = v == null || v.isBlank() ? -1 : Short.parseShort(v),
                    d -> d.inventorySlot < 0 ? null : Short.toString(d.inventorySlot)
                )
                .add()
                .build();

        private String action;
        private int slotIndex = -1;
        private short inventorySlot = -1;
    }
}
