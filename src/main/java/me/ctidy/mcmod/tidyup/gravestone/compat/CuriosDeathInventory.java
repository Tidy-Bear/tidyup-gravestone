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
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        //noinspection NullableProblems
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
            return;
        }
        setBuriedCurios(buriedCurios);
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

        Map<String, ICurioStacksHandler> playerCurios = curiosInv.getCurios();
        List<BuriedCurio> curiosLeft = buriedCurios.stream().map(buriedCurio -> {
            // restore to the original slot first
            // yield the curio that cannot be equipped at the original slot but may be equipped at other slots
            // yield null if the curio succeed to be equipped, or it definitely cannot be equipped at any other slots
            // for the latter case, remember to add to itemsToInv manually

            if (buriedCurio.unequipable()) {
                // how wierd, but happened
                itemsToInv.add(buriedCurio.itemStack);
                return null;
            }

            Optional<ICurio> curioOptional = buriedCurio.asCurio().resolve();
            if (curioOptional.isEmpty()) {
                itemsToInv.add(buriedCurio.itemStack);
                return null;
            }
            ICurio curio = curioOptional.get();

            ICurioStacksHandler slotInv = playerCurios.get(buriedCurio.slotType);
            if (slotInv == null) {
                return buriedCurio;
            }

            NonNullList<Boolean> renderStates = slotInv.getRenders();
            SlotContext slotContext = new SlotContext(buriedCurio.slotType, player, buriedCurio.slot, false,
                    renderStates.size() > buriedCurio.slot && renderStates.get(buriedCurio.slot));
            if (!curio.canEquip(slotContext)) {
                return buriedCurio;
            }

            IDynamicStackHandler stackHandler = slotInv.getStacks();
            if (buriedCurio.slot < 0 || buriedCurio.slot >= stackHandler.getSlots()
                    || !stackHandler.isItemValid(buriedCurio.slot, buriedCurio.itemStack)) {
                return buriedCurio;
            }

            ItemStack presentItem = stackHandler.getStackInSlot(buriedCurio.slot);
            if (!presentItem.isEmpty()) {
                return buriedCurio;
            }

            stackHandler.setStackInSlot(buriedCurio.slot, buriedCurio.itemStack);
            curio.onEquip(slotContext, presentItem);
            return null;
        }).filter(Objects::nonNull).toList();
        // ensure iter all curios in the mapper above
        curiosLeft.forEach(buriedCurio -> {
            // restore to the rest available slots

            for (ICurioStacksHandler slotInv : playerCurios.values()) {
                IDynamicStackHandler stackHandler = slotInv.getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {

                    @SuppressWarnings("OptionalGetWithoutIsPresent")  // already check in the mapper above
                    ICurio curio = buriedCurio.asCurio().resolve().get();

                    NonNullList<Boolean> renderStates = slotInv.getRenders();
                    SlotContext slotContext = new SlotContext(slotInv.getIdentifier(), player, i, false,
                            renderStates.size() > i && renderStates.get(i));
                    if (!curio.canEquip(slotContext) || !stackHandler.isItemValid(i, buriedCurio.itemStack)) {
                        continue;
                    }

                    ItemStack presentItem = stackHandler.getStackInSlot(i);
                    if (presentItem.isEmpty()) {
                        stackHandler.setStackInSlot(i, buriedCurio.itemStack);
                        curio.onEquip(slotContext, presentItem);
                        return;
                    }
                }
            }

            itemsToInv.add(buriedCurio.itemStack);
        });
        setBuriedCurios(Collections.emptyList());
    }

    @Override
    public void fromNBT(CompoundTag parent) {
        String tagKey = id.toString();
        if (!parent.contains(tagKey, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag invRoot = parent.getCompound(tagKey);
        List<BuriedCurio> buriedCurios = invRoot.getAllKeys().stream().flatMap(slotType -> invRoot.getList(slotType, Tag.TAG_COMPOUND)
                .stream().map(rawTag -> {
                    CompoundTag tag = (CompoundTag) rawTag;
                    ItemStack itemStack = ItemStack.of(tag);
                    if (itemStack.isEmpty()) {
                        return null;
                    }
                    return new BuriedCurio(slotType, tag.getInt("Slot"), itemStack);
                }).filter(Objects::nonNull)
        ).sorted().toList();

        if (buriedCurios.isEmpty()) {
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
            if (buriedCurio.itemStack.isEmpty()) {
                continue;
            }

            ListTag listTag = invRoot.getList(buriedCurio.slotType, Tag.TAG_COMPOUND);
            if (listTag.isEmpty()) {
                invRoot.put(buriedCurio.slotType, listTag);
            }

            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", buriedCurio.slot);
            buriedCurio.itemStack.save(itemTag);
            listTag.add(itemTag);
        }
        if (!invRoot.isEmpty()) {
            parent.put(id.toString(), invRoot);
        }
    }

    @Override
    public Stream<ItemStack> getAllItemsAsStream() {
        if (buriedCurios.isEmpty()) {
            return Stream.empty();
        }
        return buriedCurios.stream().map(BuriedCurio::itemStack);
    }

    private void setBuriedCurios(List<BuriedCurio> buriedCurios) {
        this.buriedCurios = buriedCurios;
    }

    public record BuriedCurio(String slotType, int slot, ItemStack itemStack) implements Comparable<BuriedCurio> {

        @Override
        public int compareTo(@NotNull CuriosDeathInventory.BuriedCurio other) {
            int i = slotType.compareTo(other.slotType);
            if (i == 0) {
                return Integer.compare(slot, other.slot);
            }
            return i;
        }

        public LazyOptional<ICurio> asCurio() {
            return CuriosApi.getCurio(itemStack);
        }

        public boolean unequipable() {
            return itemStack.isEmpty() || itemStack.getEnchantmentLevel(Enchantments.BINDING_CURSE) > 0;
        }

    }

}
