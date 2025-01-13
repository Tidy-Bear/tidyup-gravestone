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

package me.ctidy.mcmod.tidyup.gravestone.forge;

import me.ctidy.mcmod.tidyup.gravestone.Constants;
import me.ctidy.mcmod.tidyup.gravestone.api.ExtendedDeathInventoryRegistry;
import me.ctidy.mcmod.tidyup.gravestone.compat.CuriosDeathInventory;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

/**
 * ForgeMain
 *
 * @author Tidy-Bear
 * @since 2025/1/4
 */
@Mod(Constants.MOD_ID)
public class ForgeMain {

    public ForgeMain() {
        ModLoadingContext.get().registerDisplayTest(IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION);
        ExtendedDeathInventoryRegistry.INSTANCE.register(Constants.id("curios"), CuriosDeathInventory::new);
        // if (Dist.CLIENT == FMLEnvironment.dist) {
        //     ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        //     MinecraftForge.EVENT_BUS.addListener(this::registerClientCommands);
        // }
    }

}
