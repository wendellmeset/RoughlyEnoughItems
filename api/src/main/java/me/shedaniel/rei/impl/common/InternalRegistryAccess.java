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

package me.shedaniel.rei.impl.common;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.GameInstance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

@ApiStatus.Internal
public final class InternalRegistryAccess implements Supplier<RegistryAccess> {
    private static final InternalRegistryAccess INSTANCE = new InternalRegistryAccess();
    private WeakReference<RegistryAccess> registryAccess;
    private boolean warned;
    
    public static InternalRegistryAccess getInstance() {
        return INSTANCE;
    }
    
    @Override
    public RegistryAccess get() {
        RegistryAccess access = this.registryAccess == null ? null : this.registryAccess.get();
        if (access != null) {
            return access;
        }
        
        if (Platform.getEnvironment() == Env.CLIENT) {
            access = getFromClient();
        } else if (GameInstance.getServer() != null) {
            access = GameInstance.getServer().registryAccess();
        }
        
        if (access == null && !this.warned) {
            this.warned = true;
            
            new NullPointerException("Cannot get registry access!").printStackTrace();
            InternalLogger.getInstance().warn("Cannot get registry access!");
            return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        }
        
        return access;
    }
    
    @Environment(EnvType.CLIENT)
    private static RegistryAccess getFromClient() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.registryAccess();
        } else if (Minecraft.getInstance().getConnection() != null) {
            return Minecraft.getInstance().getConnection().registryAccess();
        } else if (Minecraft.getInstance().gameMode != null) {
            // Sometimes the packet is sent way too fast and is between the connection and the level, better safe than sorry
            return Minecraft.getInstance().gameMode.connection.registryAccess();
        }
        
        return null;
    }
}
