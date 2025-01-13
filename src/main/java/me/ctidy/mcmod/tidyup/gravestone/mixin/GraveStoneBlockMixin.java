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

package me.ctidy.mcmod.tidyup.gravestone.mixin;

import de.maxhenkel.gravestone.blocks.GraveStoneBlock;
import de.maxhenkel.gravestone.corelib.death.Death;
import me.ctidy.mcmod.tidyup.gravestone.duck.IWithExtensibleInventories;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

/**
 * GraveStoneBlockMixin
 *
 * @author Tidy-Bear
 * @since 2025/1/10
 */
@Mixin(value = GraveStoneBlock.class, remap = false)
public abstract class GraveStoneBlockMixin {

    @Redirect(method = "fillPlayerInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;addAll(Ljava/util/Collection;)Z"))
    private boolean restorePlayerInventory(NonNullList<ItemStack> itemsToInv, Collection<ItemStack> additionalItems, Player player, Death death) {
        ((IWithExtensibleInventories) death).getExtensibleInventories().restorePlayerInventory(itemsToInv, player, death);
        return itemsToInv.addAll(additionalItems);
    }

}
