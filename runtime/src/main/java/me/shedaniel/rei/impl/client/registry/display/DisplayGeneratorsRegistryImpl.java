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

package me.shedaniel.rei.impl.client.registry.display;

import me.shedaniel.rei.api.client.registry.display.DisplayGeneratorsRegistry;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.impl.common.InternalLogger;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public interface DisplayGeneratorsRegistryImpl extends DisplayGeneratorsRegistry {
    List<DynamicDisplayGenerator<?>> globalDisplayGenerators();
    
    Map<CategoryIdentifier<?>, List<DynamicDisplayGenerator<?>>> categoryDisplayGenerators();
    
    @Override
    default <A extends Display> void registerGlobalDisplayGenerator(DynamicDisplayGenerator<A> generator) {
        globalDisplayGenerators().add(generator);
        InternalLogger.getInstance().debug("Added global display generator: %s", generator);
    }
    
    @Override
    default <A extends Display> void registerDisplayGenerator(CategoryIdentifier<A> categoryId, DynamicDisplayGenerator<A> generator) {
        categoryDisplayGenerators().computeIfAbsent(categoryId, location -> new ArrayList<>())
                .add(generator);
        InternalLogger.getInstance().debug("Added display generator for category [%s]: %s", categoryId, generator);
    }
    
    @Override
    default List<DynamicDisplayGenerator<?>> getGlobalDisplayGenerators() {
        return Collections.unmodifiableList(globalDisplayGenerators());
    }
    
    @Override
    default Map<CategoryIdentifier<?>, List<DynamicDisplayGenerator<?>>> getCategoryDisplayGenerators() {
        return Collections.unmodifiableMap(categoryDisplayGenerators());
    }
}
