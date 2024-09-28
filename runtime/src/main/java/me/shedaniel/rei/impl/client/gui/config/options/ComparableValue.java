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

package me.shedaniel.rei.impl.client.gui.config.options;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.BiPredicate;

@ApiStatus.Internal
public final class ComparableValue<T> {
    private final T value;
    private final BiPredicate<T, Object> equals;
    
    private ComparableValue(T value, BiPredicate<T, Object> equals) {
        this.value = value;
        this.equals = equals;
    }
    
    public static <T> ComparableValue<T> of(T value, BiPredicate<T, Object> equals) {
        return new ComparableValue<>(value, equals);
    }
    
    public static ComparableValue<Float> ofFloat(float value) {
        return of(value, (a, b) -> b instanceof Float f && Math.abs(a - f) <= 0.001F);
    }
    
    public static ComparableValue<Double> ofDouble(double value) {
        return of(value, (a, b) -> b instanceof Double d && Math.abs(a - d) <= 0.001D);
    }
    
    public T value() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ComparableValue) {
            return equals.test(value, ((ComparableValue<?>) obj).value);
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
