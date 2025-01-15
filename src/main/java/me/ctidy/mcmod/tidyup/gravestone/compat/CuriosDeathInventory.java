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

import de.maxhenkel.gravestone.corelib.death.Death;
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
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * CuriosDeathInventory
 *
 * @author Tidy-Bear
 * @since 2025/1/6
 */
public class CuriosDeathInventory implements IExtendedDeathInventory {

    private final ResourceLocation id;

    /**
     * Set to {@link Collections#emptyList()} if no need to keep data (e.g. restored to player's inventory).<br/>
     * Recommended to sort before set.<br/>
     * Unmodifiable, i.e. all data should be set at once.
     */
    @Unmodifiable
    private List<BuriedCurio> buriedCurios = Collections.emptyList();

    public CuriosDeathInventory(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void fromDeath(Player player, Death death) {
        List<BuriedCurio> buriedCurios = CuriosApi.getCuriosInventory(player)
                .map(ICuriosItemHandler::getCurios)
                .stream().flatMap(curiosInv -> curiosInv.values().stream())
                .flatMap(slotInv -> {
                    String slotType = slotInv.getIdentifier();
                    IDynamicStackHandler itemHandler = slotInv.getStacks();

                    return IntStream.range(0, itemHandler.getSlots()).mapToObj(slot -> {
                        ItemStack itemStack = itemHandler.getStackInSlot(slot);
                        if (itemStack.isEmpty()) {
                            return null;
                        }
                        return new BuriedCurio(slotType, slot, itemStack);
                    }).filter(Objects::nonNull);
                }).sorted().toList();

        if (buriedCurios.isEmpty()) {
            setBuriedCuriosAsEmpty();
            return;
        }
        setBuriedCurios(buriedCurios);
    }

    @Override
    public void fromNBT(CompoundTag parent) {
        String tagKey = id.toString();
        if (!parent.contains(tagKey, Tag.TAG_COMPOUND)) {
            setBuriedCuriosAsEmpty();
            return;
        }

        CompoundTag invRoot = parent.getCompound(tagKey);
        List<BuriedCurio> buriedCurios = invRoot.getAllKeys().stream().flatMap(slotType -> invRoot.getList(slotType, Tag.TAG_COMPOUND)
                .stream().map(rawTag -> BuriedCurio.fromNBT(slotType, (CompoundTag) rawTag)).filter(Objects::nonNull)
        ).sorted().toList();

        if (buriedCurios.isEmpty()) {
            setBuriedCuriosAsEmpty();
            return;
        }
        setBuriedCurios(buriedCurios);
    }

    @Override
    public void toNBT(CompoundTag parent) {
        if (buriedCurios.isEmpty()) {
            return;
        }
        CompoundTag invRoot = new CompoundTag();
        for (BuriedCurio buriedCurio : buriedCurios) {
            buriedCurio.toNBT(invRoot);
        }
        if (!invRoot.isEmpty()) {
            parent.put(id.toString(), invRoot);
        }
    }

    @Override
    public void restorePlayerInventory(NonNullList<ItemStack> itemsToInv, Player player, Death death) {
        if (buriedCurios.isEmpty()) {
            return;
        }

        Optional<ICuriosItemHandler> curiosInvOptional = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosInvOptional.isEmpty()) {
            return;
        }
        ICuriosItemHandler curiosInv = curiosInvOptional.get();

        List<BuriedCurio> curiosLeft = buriedCurios.stream()
                .map(buriedCurio -> buriedCurio.restore(itemsToInv, curiosInv))
                .filter(Objects::nonNull).toList();
        // ensure restore all curios first, and then resettle the left (curios replaced + buried curios failed to restore)
        curiosLeft.forEach(buriedCurio -> buriedCurio.resettle(itemsToInv, curiosInv));

        setBuriedCuriosAsEmpty();  // prevent the upper duplicated splitting item stack into additionalItems
    }

    @Override
    public Stream<ItemStack> getAllItemsAsStream() {
        if (buriedCurios.isEmpty()) {
            return Stream.empty();
        }
        return buriedCurios.stream().map(BuriedCurio::itemStack);
    }

    private void setBuriedCuriosAsEmpty() {
        this.buriedCurios = Collections.emptyList();
    }

    private void setBuriedCurios(List<BuriedCurio> buriedCurios) {
        this.buriedCurios = buriedCurios;
    }

    @VisibleForTesting
    public record BuriedCurio(String slotType, int slot, ItemStack itemStack) implements Comparable<BuriedCurio> {

        public static BuriedCurio fromNBT(String slotType, CompoundTag itemTag) {
            ItemStack itemStack = ItemStack.of(itemTag);
            if (itemStack.isEmpty()) {
                return null;
            }
            return new BuriedCurio(slotType, itemTag.getInt("Slot"), itemStack);
        }

        @Override
        public int compareTo(@NotNull CuriosDeathInventory.BuriedCurio other) {
            int i = slotType.compareTo(other.slotType);
            if (i == 0) {
                return Integer.compare(slot, other.slot);
            }
            return i;
        }

        public void toNBT(CompoundTag parent) {
            if (itemStack.isEmpty()) {
                return;
            }

            ListTag listTag = parent.getList(slotType, Tag.TAG_COMPOUND);
            if (listTag.isEmpty()) {
                parent.put(slotType, listTag);
            }

            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", slot);
            itemStack.save(itemTag);
            listTag.add(itemTag);
        }

        public boolean shouldNotBeEquipped() {
            return itemStack.getEnchantmentLevel(Enchantments.BINDING_CURSE) > 0;
        }

        /**
         * Restore to the original slot as far as possible.<br/>
         * @return Case 1: {@code null} if the buried curio: <ul>
         *         <li>succeed to be equipped (the target slot is empty);</li>
         *         <li>definitely cannot be equipped at any other slots. (will be automatically added into {@code itemsToInv})</li></ul>
         *         Case 2: A newly-created object with the {@code itemStack} replaced by the buried one. <br/>
         *         Case 3: The object itself that cannot be equipped at the target slot for some reason. <br/>
         */
        public @Nullable BuriedCurio restore(NonNullList<ItemStack> itemsToInv, ICuriosItemHandler curiosInv) {
            if (itemStack.isEmpty()) {
                return null;
            }
            if (shouldNotBeEquipped()) {
                // how weird, but happened
                // definitely cannot be equipped at any other slots
                itemsToInv.add(itemStack);
                return null;
            }

            // involved in DynamicStackHandler.isItemValid
            // var curioOptional = asCurio().resolve();
            // if (curioOptional.isEmpty()) {
            //     itemsToInv.add(itemStack);
            //     return null;
            // }
            // var curio = curioOptional.get();

            var slotInvOptional = curiosInv.getStacksHandler(slotType);
            if (slotInvOptional.isEmpty()) {
                return this;
            }
            var slotInv = slotInvOptional.get();

            // involved in DynamicStackHandler.isItemValid
            // NonNullList<Boolean> renderStates = slotInv.getRenders();
            // SlotContext slotContext = new SlotContext(slotType, curiosInv.getWearer(), slot, false,
            //         renderStates.size() > slot && renderStates.get(slot));
            // if (!curio.canEquip(slotContext)) {
            //     return this;
            // }

            IDynamicStackHandler stackHandler = slotInv.getStacks();
            if (slot < 0 || slot >= stackHandler.getSlots()
                    || !stackHandler.isItemValid(slot, itemStack)) {
                return this;
            }

            ItemStack presentItem = stackHandler.getStackInSlot(slot);
            if (presentItem.isEmpty()) {
                stackHandler.setStackInSlot(slot, itemStack);
                // curio.onEquip(slotContext, itemStack);  // called by Curios itself when the change is detected during ticking
                return null;
            } else if (stackHandler.extractItem(slot, itemStack.getMaxStackSize(), true).getCount()
                    == itemStack.getCount()) {
                stackHandler.setStackInSlot(slot, itemStack);
                // curio.onEquip(slotContext, itemStack);  // called by Curios itself when the change is detected during ticking
                return new BuriedCurio(slotType, slot, presentItem);
            }
            return this;  // passed all checks, but failed to extract the present curio from the target slot
        }

        public void resettle(NonNullList<ItemStack> itemsToInv, ICuriosItemHandler curiosInv) {
            for (ICurioStacksHandler slotInv : curiosInv.getCurios().values()) {
                IDynamicStackHandler stackHandler = slotInv.getStacks();
                for (int i = 0, size = stackHandler.getSlots(); i < size; i++) {

                    // involved in DynamicStackHandler.isItemValid
                    // @SuppressWarnings("OptionalGetWithoutIsPresent")  // already check in the mapper above
                    // ICurio curio = buriedCurio.asCurio().resolve().get();
                    //
                    // NonNullList<Boolean> renderStates = slotInv.getRenders();
                    // SlotContext slotContext = new SlotContext(slotInv.getIdentifier(), player, i, false,
                    //         renderStates.size() > i && renderStates.get(i));
                    // if (!curio.canEquip(slotContext) || !stackHandler.isItemValid(i, buriedCurio.itemStack)) {
                    //     continue;
                    // }

                    if (!stackHandler.isItemValid(i, itemStack)) {
                        continue;
                    }

                    ItemStack presentItem = stackHandler.getStackInSlot(i);
                    if (presentItem.isEmpty()) {
                        stackHandler.setStackInSlot(i, itemStack);
                        // curio.onEquip(slotContext, itemStack);  // called by Curios itself when the change is detected during ticking
                        return;
                    }
                }
            }

            itemsToInv.add(itemStack);
        }

    }

}
