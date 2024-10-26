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

package me.shedaniel.rei.api.common.display;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.ApiStatus;

/**
 * The display serializer used for display serialization, useful for persistent displays across reloads,
 * and server-client communication.
 *
 * @see DisplaySerializerRegistry
 */
@ApiStatus.NonExtendable
public interface DisplaySerializer<D extends Display> {
    MapCodec<D> codec();
    
    StreamCodec<RegistryFriendlyByteBuf, D> streamCodec();
    
    /**
     * Returns whether the serialized output is persistent across differing reboots, thus enabling serialization support for saving to disk.
     *
     * @return whether the serialized output is persistent across differing reboots, thus enabling serialization support for saving to disk.
     */
    default boolean isPersistent() {
        return true;
    }
    
    static <D extends Display> DisplaySerializer<D> of(MapCodec<D> codec, StreamCodec<RegistryFriendlyByteBuf, D> streamCodec) {
        return of(codec, streamCodec, true);
    }
    
    static <D extends Display> DisplaySerializer<D> of(MapCodec<D> codec, StreamCodec<RegistryFriendlyByteBuf, D> streamCodec, boolean persistent) {
        return new DisplaySerializer<>() {
            @Override
            public MapCodec<D> codec() {
                return codec;
            }
            
            @Override
            public StreamCodec<RegistryFriendlyByteBuf, D> streamCodec() {
                return streamCodec;
            }
            
            @Override
            public boolean isPersistent() {
                return persistent;
            }
        };
    }
}
