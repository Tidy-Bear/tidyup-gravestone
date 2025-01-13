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

package me.ctidy.mcmod.tidyup.gravestone.inv;

import de.maxhenkel.gravestone.corelib.death.Death;
import me.ctidy.mcmod.tidyup.gravestone.api.ExtendedDeathInventoryRegistry;
import me.ctidy.mcmod.tidyup.gravestone.api.IExtendedDeathInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ExtensibleDeathInventories
 *
 * @author Tidy-Bear
 * @since 2025/1/6
 */
public class ExtensibleDeathInventories {

    // public static final String TAG_KEY = "ExtensibleDeathInventories";

    @Unmodifiable
    private final Map<ResourceLocation, IExtendedDeathInventory> inventories;

    public ExtensibleDeathInventories() {
        inventories = ExtendedDeathInventoryRegistry.INSTANCE.getAll().entrySet()
                .stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().create(entry.getKey())));
    }

    public Collection<ResourceLocation> getIds() {
        return inventories.keySet();
    }

    public Collection<IExtendedDeathInventory> getInventories() {
        return inventories.values();
    }

    public IExtendedDeathInventory getInventory(ResourceLocation id) {
        return inventories.get(id);
    }

    /**
     * The map return is unmodifiable.
     */
    public Map<ResourceLocation, IExtendedDeathInventory> asMap() {
        return inventories;
    }

    public Stream<ItemStack> getAllItemsAsStream() {
        return inventories.values().stream().flatMap(IExtendedDeathInventory::getAllItemsAsStream);
    }

    public void fromDeath(Player player, Death death) {
        for (IExtendedDeathInventory inv : inventories.values()) {
            inv.fromDeath(player, death);
        }
    }

    public void restorePlayerInventory(NonNullList<ItemStack> itemsToInv, Player player, Death death) {
        for (IExtendedDeathInventory inv : inventories.values()) {
            inv.restorePlayerInventory(itemsToInv, player, death);
        }
    }

    public void fromNBT(CompoundTag parent) {
        // if (!parent.contains(TAG_KEY, Tag.TAG_COMPOUND)) {
        //     return;
        // }
        // CompoundTag root = parent.getCompound(TAG_KEY);
        for (IExtendedDeathInventory inv : inventories.values()) {
            inv.fromNBT(parent);
        }
    }

    public void toNBT(CompoundTag parent) {
        // CompoundTag root = new CompoundTag();
        for (IExtendedDeathInventory inv : inventories.values()) {
            inv.toNBT(parent);
        }
        // if (root.isEmpty()) {
        //     return;
        // }
        // parent.put(TAG_KEY, root);
    }

}
