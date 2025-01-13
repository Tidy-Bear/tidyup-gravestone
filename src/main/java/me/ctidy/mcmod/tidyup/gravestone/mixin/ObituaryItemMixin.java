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

import de.maxhenkel.gravestone.GraveUtils;
import de.maxhenkel.gravestone.corelib.death.Death;
import de.maxhenkel.gravestone.items.ObituaryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * ObituaryItemMixin
 *
 * @author Tidy-Bear
 * @since 2025/1/13
 */
@Mixin(value = ObituaryItem.class, remap = false)
public abstract class ObituaryItemMixin extends Item {

    public ObituaryItemMixin(Properties pProperties) {
        super(pProperties);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level p_41422_, List<Component> components, TooltipFlag hideFlag) {
        super.appendHoverText(itemStack, p_41422_, components, hideFlag);
        CompoundTag tag = itemStack.getTagElement("Death");
        if (tag == null) {
            return;
        }
        MutableComponent separator = Component.translatable("item.gravestone.obituary.desc.separator");
        String name = tag.getString("PlayerName");
        if (!name.isBlank()) {
            components.add(Component.translatable("item.gravestone.obituary.player_name.desc")
                    .withStyle(ChatFormatting.GRAY)
                    .append(separator)
                    .append(name));
        }
        if (tag.contains("DeadTimestamp", Tag.TAG_LONG)) {
            MutableComponent component = GraveUtils.getDate(tag.getLong("DeadTimestamp"));
            if (component != null) {
                components.add(Component.translatable("item.gravestone.obituary.time.desc")
                        .withStyle(ChatFormatting.GRAY)
                        .append(separator)
                        .append(component));
            }
        }
        if (tag.contains("ItemCount", Tag.TAG_INT)) {
            components.add(Component.translatable("item.gravestone.obituary.item_count.desc")
                    .withStyle(ChatFormatting.GRAY)
                    .append(separator)
                    .append(String.valueOf(tag.getInt("ItemCount"))));
        }
        if (hideFlag.isAdvanced() && tag.hasUUID("DeathID")) {
            components.add(Component.translatable("item.gravestone.obituary.id.desc")
                    .withStyle(ChatFormatting.GRAY)
                    .append(separator)
                    .append(tag.getUUID("DeathID").toString()));
        }
        components.add(Component.empty());
        components.add(Component.translatable("item.gravestone.obituary.restore.desc1")
                .withStyle(ChatFormatting.GRAY));
        components.add(Component.literal("  ").withStyle(ChatFormatting.BLUE)
                .append(Component.translatable("item.gravestone.obituary.restore.desc2")));
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "toStack", at = @At("RETURN"))
    private void enhanceTag(Death death, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack itemStack = cir.getReturnValue();
        CompoundTag tag = itemStack.getTagElement("Death");
        tag.putString("PlayerName", death.getPlayerName());
        tag.putLong("DeadTimestamp", death.getTimestamp());
        tag.putInt("ItemCount", (int) death.getAllItems().stream().filter(droppedItem -> !droppedItem.isEmpty()).count());
    }

}
