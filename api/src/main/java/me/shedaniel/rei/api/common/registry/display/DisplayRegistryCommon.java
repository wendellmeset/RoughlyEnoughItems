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

package me.shedaniel.rei.api.common.registry.display;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.plugins.REIPlugin;
import me.shedaniel.rei.api.common.registry.Reloadable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface DisplayRegistryCommon<P extends REIPlugin<?>> extends Reloadable<P>, DisplayConsumer {
    /**
     * Returns the total display count
     *
     * @return the recipe count
     */
    int size();
    
    /**
     * Returns an unmodifiable map of displays visible to the player
     *
     * @return an unmodifiable map of displays
     */
    Map<CategoryIdentifier<?>, List<Display>> getAll();
    
    /**
     * Returns the list of displays visible to the player for a category
     *
     * @return the list of displays
     */
    default <D extends Display> List<D> get(CategoryIdentifier<D> categoryId) {
        return (List<D>) getAll().getOrDefault(categoryId, Collections.emptyList());
    }
    
    /**
     * Returns the origin of the display, this may be the recipe which the display was created from.
     *
     * @param display the display
     * @return the origin
     */
    @Nullable
    Object getDisplayOrigin(Display display);
}
