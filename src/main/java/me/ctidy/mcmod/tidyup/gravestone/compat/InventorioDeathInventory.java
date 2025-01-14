/*
 * Copyright (c) 2025, Tidy-Bear.
 *
 * This file is part of "Tidy UP - Grave Stone".
 *
 * "Tidy UP - Grave Stone" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * "Tidy UP - Grave Stone" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with "Tidy UP - Grave Stone".  If not, see <https://www.gnu.org/licenses/>.
 */

package me.ctidy.mcmod.tidyup.gravestone.compat;

import com.google.common.collect.ImmutableList;
import de.maxhenkel.gravestone.corelib.death.Death;
import de.rubixdev.inventorio.api.InventorioAPI;
import de.rubixdev.inventorio.api.ToolBeltSlotTemplate;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import de.rubixdev.inventorio.util.GeneralConstants;
import kotlin.ranges.IntRange;
import me.ctidy.mcmod.tidyup.gravestone.api.IExtendedDeathInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * InventorioDeathInventory
 *
 * @author Tidy-Bear
 * @since 2025/1/13
 */
public class InventorioDeathInventory implements IExtendedDeathInventory {

    private final ResourceLocation id;

    /**
     * Set to {@link Collections#emptyList()} if no need to keep data (e.g. restored to player's inventory).<br/>
     * Recommended to sort before set.<br/>
     * Unmodifiable, i.e. all data should be set at once.
     */
    @Unmodifiable
    private List<BuriedItem> buriedItems = Collections.emptyList();

    public InventorioDeathInventory(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void fromDeath(Player player, Death death) {
        PlayerInventoryAddon invAddon = InventorioAPI.getInventoryAddon(player);
        if (invAddon == null) {
            setBuriedItemsAsEmpty();
            return;
        }
        List<BuriedItem> buriedItems = Arrays.stream(InventoryArea.VALUES)
                .flatMap(invArea -> invArea.getBuriedItemsAsStream(invAddon))
                .sorted().toList();
        if (buriedItems.isEmpty()) {
            setBuriedItemsAsEmpty();
            return;
        }
        setBuriedItems(buriedItems);
    }

    @Override
    public void restorePlayerInventory(NonNullList<ItemStack> itemsToInv, Player player, Death death) {
        if (buriedItems.isEmpty()) {
            return;
        }

        PlayerInventoryAddon invAddon = InventorioAPI.getInventoryAddon(player);
        if (invAddon == null) {
            // itemsToInv.addAll(buriedItems.stream().map(BuriedItem::itemStack).toList());  // leave it to the upper
            return;
        }

        // restore to the original slot first
        // yield the item which is replaced with the buried one, or the buried item whose slot is now invalid
        // yield null if the curio succeed to be equipped, or it definitely cannot be equipped at any other slots
        // for the latter case, remember to add to itemsToInv manually
        List<BuriedItem> itemsLeft = buriedItems.stream()
                .map(buriedItem -> buriedItem.restore(itemsToInv, invAddon))
                .filter(Objects::nonNull).toList();

        // ensure restore all items first, and then resettle the left (items replaced + buried items each with invalid slot)
        itemsLeft.forEach(buriedItem -> buriedItem.resettle(itemsToInv, invAddon));

        setBuriedItemsAsEmpty();  // prevent the upper duplicated splitting item stack into additionalItems
    }

    @Override
    public void fromNBT(CompoundTag parent) {
        String tagKey = id.toString();
        if (!parent.contains(tagKey, Tag.TAG_COMPOUND)) {
            setBuriedItemsAsEmpty();
            return;
        }

        CompoundTag invRoot = parent.getCompound(tagKey);
        List<BuriedItem> buriedItems = invRoot.getAllKeys().stream().flatMap(area -> invRoot.getList(area, Tag.TAG_COMPOUND)
                .stream().map(rawTag -> {
                    CompoundTag tag = (CompoundTag) rawTag;
                    ItemStack itemStack = ItemStack.of(tag);
                    if (itemStack.isEmpty()) {
                        return null;
                    }
                    return new BuriedItem(InventoryArea.of(area), tag.getInt("Slot"), itemStack);
                }).filter(Objects::nonNull)
        ).sorted().toList();

        if (buriedItems.isEmpty()) {
            setBuriedItemsAsEmpty();
            return;
        }
        setBuriedItems(buriedItems);
    }

    @Override
    public void toNBT(CompoundTag parent) {
        if (buriedItems.isEmpty()) {
            return;
        }
        CompoundTag invRoot = new CompoundTag();
        for (BuriedItem buriedItem : buriedItems) {
            if (buriedItem.itemStack.isEmpty()) {
                continue;
            }

            ListTag listTag = invRoot.getList(buriedItem.area.tagName(), Tag.TAG_COMPOUND);
            if (listTag.isEmpty()) {
                invRoot.put(buriedItem.area.tagName(), listTag);
            }

            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", buriedItem.slot);
            buriedItem.itemStack.save(itemTag);
            listTag.add(itemTag);
        }
        if (!invRoot.isEmpty()) {
            parent.put(id.toString(), invRoot);
        }
    }

    @Override
    public Stream<ItemStack> getAllItemsAsStream() {
        if (buriedItems.isEmpty()) {
            return Stream.empty();
        }
        return buriedItems.stream().map(BuriedItem::itemStack);
    }

    private void setBuriedItemsAsEmpty() {
        this.buriedItems = Collections.emptyList();
    }

    private void setBuriedItems(List<BuriedItem> buriedItems) {
        this.buriedItems = buriedItems;
    }

    public enum InventoryArea {
        EMPTY(0, invAddon -> Collections.emptyList()) {
            @Override
            public boolean isItemValid(PlayerInventoryAddon invAddon, int slot, ItemStack itemStack) {
                return false;
            }
        },
        DEEP_POCKETS(GeneralConstants.INVENTORY_ADDON_DEEP_POCKETS_RANGE.getFirst(), invAddon -> invAddon.deepPockets) {
            @Override
            public boolean isItemValid(PlayerInventoryAddon invAddon, int slot, ItemStack itemStack) {
                IntRange range = invAddon.getAvailableDeepPocketsRange();
                return !range.isEmpty() && range.contains(slot);
            }
        },
        UTILITY_BELT(GeneralConstants.INVENTORY_ADDON_UTILITY_BELT_RANGE.getFirst(), invAddon -> invAddon.utilityBelt) {
            @Override
            public boolean isItemValid(PlayerInventoryAddon invAddon, int slot, ItemStack itemStack) {
                int size = invAddon.getAvailableUtilityBeltSize();
                return slot < size && slot >= 0;
            }
        },
        TOOL_BELT(GeneralConstants.INVENTORY_ADDON_TOOL_BELT_INDEX_OFFSET, invAddon -> invAddon.toolBelt) {
            @Override
            public boolean isItemValid(PlayerInventoryAddon invAddon, int slot, ItemStack itemStack) {
                return invAddon.findFittingToolBeltIndex(itemStack) == slot;
            }
        },;

        @Unmodifiable
        public static final InventoryArea[] VALUES = InventoryArea.values();

        public final int beginIndex;

        private final Function<PlayerInventoryAddon, List<ItemStack>> itemsGetter;

        public static InventoryArea of(String name) {
            name = name.toUpperCase();
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return EMPTY;
            }
        }

        InventoryArea(int beginIndex, Function<PlayerInventoryAddon, List<ItemStack>> itemsGetter) {
            this.beginIndex = beginIndex;
            this.itemsGetter = itemsGetter;
        }

        public String tagName() {
            return name().toLowerCase();
        }

        public List<ItemStack> getInventory(PlayerInventoryAddon invAddon) {
            return itemsGetter.apply(invAddon);
        }

        public Stream<BuriedItem> getBuriedItemsAsStream(PlayerInventoryAddon invAddon) {
            List<ItemStack> inv = getInventory(invAddon);
            return IntStream.range(0, inv.size())
                    .mapToObj(i -> {
                        ItemStack itemStack = inv.get(i);
                        return itemStack.isEmpty() ? null : new BuriedItem(this, i, itemStack);
                    }).filter(Objects::nonNull);
        }

        public abstract boolean isItemValid(PlayerInventoryAddon invAddon, int slot, ItemStack itemStack);

    }

    @VisibleForTesting
    public record BuriedItem(InventoryArea area, int slot, ItemStack itemStack) implements Comparable<BuriedItem> {

        @Override
        public int compareTo(@NotNull InventorioDeathInventory.BuriedItem other) {
            int i = area.compareTo(other.area);
            if (i == 0) {
                return Integer.compare(slot, other.slot);
            }
            return i;
        }

        public boolean unequipable() {
            return itemStack.isEmpty() || itemStack.getEnchantmentLevel(Enchantments.BINDING_CURSE) > 0;
        }

        public @Nullable BuriedItem restore(NonNullList<ItemStack> itemsToInv, PlayerInventoryAddon invAddon) {
            if (unequipable()) {
                // definitely cannot be equipped at any other slots
                itemsToInv.add(itemStack);
                return null;
            }
            if (!area.isItemValid(invAddon, slot, itemStack)) {
                return this;
            }
            List<ItemStack> inv = area.getInventory(invAddon);
            ItemStack presentItem = inv.get(slot);
            inv.set(slot, itemStack);
            return presentItem.isEmpty() ? null : new BuriedItem(area, slot, presentItem);
        }

        public void resettle(NonNullList<ItemStack> itemsToInv, PlayerInventoryAddon invAddon) {
            List<ItemStack> inv = area.getInventory(invAddon);
            if (area == InventoryArea.TOOL_BELT) {
                ImmutableList<ToolBeltSlotTemplate> templates = PlayerInventoryAddon.getToolBeltTemplates();
                for (int i = 0, size = templates.size(); i < size; i++) {
                    ToolBeltSlotTemplate template = templates.get(i);
                    if (template.test(itemStack, invAddon) && inv.get(i).isEmpty()) {
                        inv.set(i, itemStack);
                        return;
                    }
                }
                itemsToInv.add(itemStack);
                return;
            }
            for (int i = 0, size = inv.size(); i < size; i++) {
                if (inv.get(i).isEmpty()) {
                    inv.set(i, itemStack);
                    return;
                }
            }
            itemsToInv.add(itemStack);
        }
    }

}
