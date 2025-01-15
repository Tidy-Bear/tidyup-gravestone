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

import de.maxhenkel.gravestone.corelib.death.Death;
import me.ctidy.mcmod.tidyup.gravestone.duck.IWithExtensibleInventories;
import me.ctidy.mcmod.tidyup.gravestone.inv.ExtensibleDeathInventories;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * DeathMixin
 *
 * @author Tidy-Bear
 * @since 2025/1/6
 */
@Mixin(value = Death.class, remap = false)
public abstract class DeathMixin implements IWithExtensibleInventories {

    @Unique
    private ExtensibleDeathInventories inventories;

    @Override
    public ExtensibleDeathInventories getExtensibleInventories() {
        return inventories;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void createInventories(CallbackInfo ci) {
        inventories = new ExtensibleDeathInventories();
    }

    @Inject(method = "fromPlayer", at = @At("RETURN"))
    private static void fillInventoriesOnDead(Player player, CallbackInfoReturnable<Death> cir) {
        Death death = cir.getReturnValue();
        ((IWithExtensibleInventories) death).getExtensibleInventories().fromDeath(player, death);
    }

    @Inject(method = "fromNBT", at = @At("RETURN"))
    private static void fillInventoriesFromNBT(CompoundTag tag, CallbackInfoReturnable<Death> cir) {
        ((IWithExtensibleInventories) cir.getReturnValue()).getExtensibleInventories().fromNBT(tag);
    }

    @Inject(method = "toNBT(Z)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void dumpInventoriesToNBT(boolean withItems, CallbackInfoReturnable<CompoundTag> cir) {
        if (!withItems) {
            return;
        }
        inventories.toNBT(cir.getReturnValue());
    }

    @Redirect(method = "processDrops", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;addAll(Ljava/util/Collection;)Z"))
    private boolean pickRemainingDrops(NonNullList<ItemStack> additionalItems, Collection<ItemStack> drops) {
        inventories.getAllItemsAsStream().forEach(drops::remove);  // no need to call contains()
        return additionalItems.addAll(drops);
    }

    @Inject(method = "getAllItems", at = @At("RETURN"))
    private void getAllItems(CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        cir.getReturnValue().addAll(inventories.getAllItemsAsStream().toList());
    }

}
