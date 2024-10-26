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

import me.shedaniel.rei.api.client.registry.display.reason.DisplayAdditionReason;
import me.shedaniel.rei.api.client.registry.display.reason.DisplayAdditionReasons;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.registry.display.DisplayConsumer;
import me.shedaniel.rei.impl.common.InternalLogger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

@ApiStatus.Internal
public interface DisplayConsumerImpl extends DisplayConsumer {
    List<DisplayFiller<?>> fillers();
    
    @Override
    default <D extends Display> void registerFillerWithReason(BiPredicate<?, DisplayAdditionReasons> predicate, BiFunction<?, DisplayAdditionReasons, @Nullable D> filler) {
        fillers().add(DisplayFiller.of((BiPredicate<Object, DisplayAdditionReasons>) predicate, (BiFunction<Object, DisplayAdditionReasons, D>) filler));
        InternalLogger.getInstance().debug("Added display filter: %s", filler);
    }
    
    @Override
    default <D extends Display> void registerDisplaysFillerWithReason(BiPredicate<?, DisplayAdditionReasons> predicate, BiFunction<?, DisplayAdditionReasons, @Nullable Collection<? extends D>> filler) {
        fillers().add(new DisplayFiller<>((BiPredicate<Object, DisplayAdditionReasons>) predicate, (BiFunction<Object, DisplayAdditionReasons, Collection<? extends D>>) filler));
        InternalLogger.getInstance().debug("Added display filter: %s", filler);
    }
    
    @Override
    default <T> Collection<Display> tryFillDisplay(T value, DisplayAdditionReason... reason) {
        if (value instanceof Display) return Collections.singleton((Display) value);
        List<Display> out = null;
        DisplayAdditionReasons reasons = reason.length == 0 ? DisplayAdditionReasons.Impl.EMPTY : new DisplayAdditionReasons.Impl(reason);
        for (DisplayFiller<?> filler : fillers()) {
            Collection<Display> displays = tryFillDisplayGenerics(filler, value, reasons);
            if (displays != null && !displays.isEmpty()) {
                if (out == null) out = new ArrayList<>();
                for (Display display : displays) {
                    if (display != null) out.add(display);
                }
            }
        }
        if (out != null) {
            return out;
        }
        return Collections.emptyList();
    }
    
    private <D extends Display> Collection<D> tryFillDisplayGenerics(DisplayFiller<? extends D> filler, Object value, DisplayAdditionReasons reasons) {
        try {
            if (filler.predicate().test(value, reasons)) {
                return (Collection<D>) filler.mappingFunction().apply(value, reasons);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to fill displays!", e);
        }
        
        return null;
    }
    
    record DisplayFiller<D extends Display>(
            BiPredicate<Object, DisplayAdditionReasons> predicate,
            
            BiFunction<Object, DisplayAdditionReasons, Collection<? extends D>> mappingFunction
    ) {
        public static <D extends Display> DisplayFiller<D> of(BiPredicate<Object, DisplayAdditionReasons> predicate, Function<Object, D> mappingFunction) {
            return new DisplayFiller<>(predicate, (o, r) -> Collections.singleton(mappingFunction.apply(o)));
        }
        
        public static <D extends Display> DisplayFiller<D> of(BiPredicate<Object, DisplayAdditionReasons> predicate, BiFunction<Object, DisplayAdditionReasons, D> mappingFunction) {
            return new DisplayFiller<>(predicate, (o, r) -> Collections.singleton(mappingFunction.apply(o, r)));
        }
        
        public static <D extends Display> DisplayFiller<D> ofMultiple(BiPredicate<Object, DisplayAdditionReasons> predicate, Function<Object, Collection<? extends D>> mappingFunction) {
            return new DisplayFiller<>(predicate, (o, r) -> mappingFunction.apply(o));
        }
    }
}
