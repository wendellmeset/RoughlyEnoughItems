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

package me.shedaniel.rei.api.client.registry.display;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.visibility.DisplayVisibilityPredicate;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.registry.display.DisplayConsumer;
import me.shedaniel.rei.api.common.registry.display.DisplayRegistryCommon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

/**
 * Registry for registering displays for categories, this is called right after
 * {@link me.shedaniel.rei.api.client.registry.category.CategoryRegistry}.
 *
 * <p>Each display should have a category associated with it that's registered,
 * For any dynamic displays, you may want to look at {@link DynamicDisplayGenerator}.
 *
 * <p>Plugins may also determine the visibility of the displays dynamically via
 * {@link DisplayVisibilityPredicate}, these predicates are preferred comparing to
 * removing the displays from the registry.
 *
 * <p>Displays filler may be used for automatically registering displays from {@link Recipe},
 * these are filled after client recipe manager sync, and are invoked with one cycle.
 * Additionally, display filters allow other mods to easily register additional displays
 * for your mod.
 *
 * @see Display
 * @see DynamicDisplayGenerator
 * @see DisplayVisibilityPredicate
 * @see REIClientPlugin#registerDisplays(DisplayRegistry)
 */
@Environment(EnvType.CLIENT)
public interface DisplayRegistry extends DisplayRegistryCommon<REIClientPlugin>, DisplayGeneratorsRegistry, DisplayPredicatesRegistry, DisplayConsumer.RecipeDisplayConsumer {
    /**
     * @return the instance of {@link DisplayRegistry}
     */
    static DisplayRegistry getInstance() {
        return PluginManager.getClientInstance().get(DisplayRegistry.class);
    }
    
    /**
     * Returns an unmodifiable list of visibility predicates.
     *
     * @return an unmodifiable list of visibility predicates.
     */
    List<DisplayVisibilityPredicate> getVisibilityPredicates();
}
