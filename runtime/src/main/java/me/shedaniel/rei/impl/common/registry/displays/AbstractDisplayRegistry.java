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

package me.shedaniel.rei.impl.common.registry.displays;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.plugins.REIPlugin;
import me.shedaniel.rei.api.common.registry.display.DisplayRegistryCommon;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractDisplayRegistry<P extends REIPlugin<?>, H extends DisplaysHolder> implements DisplayRegistryCommon<P>, DisplayConsumerImpl {
    private final Supplier<H> holderFactory;
    private H holder;
    private final List<DisplayFiller<?>> fillers = new ArrayList<>();
    
    protected AbstractDisplayRegistry(Supplier<H> holderFactory) {
        this.holderFactory = holderFactory;
        this.holder = this.holderFactory.get();
    }
    
    @Override
    public int size() {
        return this.holder.size();
    }
    
    @Override
    public void startReload() {
        this.holder = this.holderFactory.get();
        this.fillers.clear();
    }
    
    @Override
    public boolean add(Display display, @Nullable Object origin) {
        this.holder.add(display, origin);
        return true;
    }
    
    @Override
    public Map<CategoryIdentifier<?>, List<Display>> getAll() {
        return this.holder.getUnmodifiable();
    }
    
    @Override
    public List<DisplayFiller<?>> fillers() {
        return fillers;
    }
    
    public H holder() {
        return holder;
    }
    
    @Override
    @Nullable
    public Object getDisplayOrigin(Display display) {
        return this.holder.getDisplayOrigin(display);
    }
}
