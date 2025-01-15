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

package me.ctidy.mcmod.tidyup.gravestone.api;

import de.maxhenkel.gravestone.corelib.death.Death;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

/**
 * IExtendedDeathInventory
 *
 * @author Tidy-Bear
 * @since 2025/1/6
 */
public interface IExtendedDeathInventory {

    /**
     * Gets the (type) id of the inventory.
     */
    ResourceLocation id();

    /**
     * Sets data when player dead.
     * @param player Dead player.
     * @param death Death info.
     */
    void fromDeath(Player player, Death death);

    /**
     * @apiNote Get data from its own child {@link CompoundTag} of the {@code parent} instead of reading data from
     *   {@code parent} directly.
     */
    void fromNBT(CompoundTag parent);

    /**
     * @apiNote Create its own {@link CompoundTag} as a child tag of the {@code parent} instead of writing data into
     *   {@code parent} directly.
     */
    void toNBT(CompoundTag parent);

    /**
     * Restores dropping items which will try to follow the item order before death. <br/>
     * @apiNote After calling this method, all items left in its own store will be added to player's inventory. <br/>
     * Thus, it's recommended to either add items to {@code itemsToInv} or properly restore it, and then clear ALL data
     *   from its own store. <br/>
     * <br/>
     * ATTENTION: Items trying to be restored but not cleared from its own inventory will finally be added to player's
     *   inventory instead of restoring. (See {@link ItemStack#split(int)})
     * @param itemsToInv Items to be added to player's inventory, used when some items cannot be placed on the slots.
     * @param player Dead player.
     * @param death Death info.
     */
    void restorePlayerInventory(NonNullList<ItemStack> itemsToInv, Player player, Death death);

    Stream<ItemStack> getAllItemsAsStream();

}
