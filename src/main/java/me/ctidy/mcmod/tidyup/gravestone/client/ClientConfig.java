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

package me.ctidy.mcmod.tidyup.gravestone.client;

import com.electronwill.nightconfig.core.Config;
import me.ctidy.mcmod.tidyup.gravestone.Constants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * ClientConfig
 *
 * @author Tidy-Bear
 * @since 2025/1/4
 */
@OnlyIn(Dist.CLIENT)
public class ClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ClientConfig INSTANCE;

    // public final ForgeConfigSpec.BooleanValue aBool;

    static {
        Config.setInsertionOrderPreserved(true);
        Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    private static String nameTKey(final String key) {
        return Constants.MOD_ID + ".config." + key;
    }

    private static String sectionTKey(final String key) {
        return nameTKey("section." + key);
    }

    private static String commentTKey(final String key) {
        return nameTKey(key) + ".tooltip";
    }

    public ClientConfig(ForgeConfigSpec.Builder builder) {
        // builder.push("section_name").translation(sectionTKey("section_name"));

        // aBool = builder.comment("Describe the config entry.",
        //                 "[Default: true]")
        //         .translation(nameTKey("config_name"))
        //         .define("config_name", true);

        // builder.pop();
    }

}
