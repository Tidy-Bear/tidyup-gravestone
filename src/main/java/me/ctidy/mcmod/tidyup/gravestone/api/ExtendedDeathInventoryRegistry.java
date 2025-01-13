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

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ExtendedDeathInventoryRegistry
 * <br/>
 * Thread safe only when writing, i.e. registering. <br/>
 * Reading is not thread safe as it's assumed to call in the server thread only.<br/>
 *
 * @author Tidy-Bear
 * @since 2025/1/10
 */
public final class ExtendedDeathInventoryRegistry {

    public static final ExtendedDeathInventoryRegistry INSTANCE = new ExtendedDeathInventoryRegistry();

    private final Map<ResourceLocation, InventoryConstructor<?>> registry = new HashMap<>();
    // private final Map<ResourceLocation, InventoryConstructor<?>> registry = new ConcurrentHashMap<>();

    private final Map<ResourceLocation, InventoryConstructor<?>> registryView = Collections.unmodifiableMap(registry);

    private ExtendedDeathInventoryRegistry() { }

    public synchronized void register(ResourceLocation id, InventoryConstructor<?> constructor) {
        registry.putIfAbsent(id, constructor);
    }

    public InventoryConstructor<?> get(ResourceLocation id) {
        return registry.get(id);
    }

    public Map<ResourceLocation, InventoryConstructor<?>> getAll() {
        return registryView;
    }

    @FunctionalInterface
    public interface InventoryConstructor<I extends IExtendedDeathInventory> {
        I create(ResourceLocation id);
    }

}
