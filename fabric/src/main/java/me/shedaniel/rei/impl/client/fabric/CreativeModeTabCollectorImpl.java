/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020, 2021, 2022, 2023 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.impl.client.fabric;

import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.impl.common.InternalLogger;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import java.util.*;

public class CreativeModeTabCollectorImpl {
    public static Map<ItemGroup, Collection<ItemStack>> collectTabs() {
        Map<ItemGroup, Collection<ItemStack>> map = new LinkedHashMap<>();
        FeatureSet featureFlags = FeatureFlags.FEATURE_MANAGER.getFeatureSet();
        ItemGroup.DisplayContext parameters = new ItemGroup.DisplayContext(featureFlags, true, BasicDisplay.registryAccess());
        
        for (ItemGroup tab : ItemGroups.getGroups()) {
            if (tab.getType() != ItemGroup.Type.HOTBAR && tab.getType() != ItemGroup.Type.INVENTORY) {
                try {
                    ItemGroup.EntriesImpl builder = new ItemGroup.EntriesImpl(tab, featureFlags);
                    RegistryKey<ItemGroup> resourceKey = Registries.ITEM_GROUP
                            .getKey(tab)
                            .orElseThrow(() -> new IllegalStateException("Unregistered creative tab: " + tab));
                    tab.entryCollector.accept(parameters, builder);
                    map.put(tab, postFabricEvents(tab, parameters, resourceKey, builder.parentTabStacks));
                } catch (Throwable throwable) {
                    InternalLogger.getInstance().error("Failed to collect creative tab: " + tab, throwable);
                }
            }
        }
        
        return map;
    }
    
    @SuppressWarnings("UnstableApiUsage")
    private static Collection<ItemStack> postFabricEvents(ItemGroup tab, ItemGroup.DisplayContext parameters, RegistryKey<ItemGroup> resourceKey, Collection<ItemStack> tabContents) {
        try {
            // Sorry!
            FabricItemGroupEntries entries = new FabricItemGroupEntries(parameters, new LinkedList<>(tabContents), new LinkedList<>());
            ItemGroupEvents.modifyEntriesEvent(resourceKey).invoker().modifyEntries(entries);
            ItemGroupEvents.MODIFY_ENTRIES_ALL.invoker().modifyEntries(tab, entries);
            return entries.getDisplayStacks();
        } catch (Throwable throwable) {
            InternalLogger.getInstance().error("Failed to collect fabric's creative tab: " + tab, throwable);
            return tabContents;
        }
    }
}
