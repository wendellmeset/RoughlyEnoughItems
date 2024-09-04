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

package me.shedaniel.rei.impl.common.util;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.GameInstance;
import me.shedaniel.rei.impl.common.InternalLogger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

@ApiStatus.Internal
public final class InstanceHelper {
    private static final InstanceHelper INSTANCE = new InstanceHelper();
    private WeakReference<RegistryAccess> registryAccessRef;
    private WeakReference<RecipeManager> recipeManagerRef;
    private boolean warnedRegistryAccess;
    private boolean warnedRecipeManager;
    
    public static InstanceHelper getInstance() {
        return INSTANCE;
    }
    
    public RegistryAccess registryAccess() {
        RegistryAccess access = this.registryAccessRef == null ? null : this.registryAccessRef.get();
        if (access != null) {
            return access;
        }
        
        if (Platform.getEnvironment() == Env.CLIENT) {
            access = registryAccessFromClient();
        } else if (GameInstance.getServer() != null) {
            access = GameInstance.getServer().registryAccess();
        }
        
        if (access == null && !this.warnedRegistryAccess) {
            this.warnedRegistryAccess = true;
            
            InternalLogger.getInstance().throwException(new IllegalStateException("Cannot get registry access!"));
            return RegistryAccess.fromRegistryOfRegistries(Registry.REGISTRY);
        }
        
        return access;
    }
    
    public RecipeManager recipeManager() {
        RecipeManager manager = this.recipeManagerRef == null ? null : this.recipeManagerRef.get();
        if (manager != null) {
            return manager;
        }
        
        if (Platform.getEnvironment() == Env.CLIENT) {
            manager = recipeManagerFromClient();
        } else if (GameInstance.getServer() != null) {
            manager = GameInstance.getServer().getRecipeManager();
        }
        
        if (manager == null && !this.warnedRegistryAccess) {
            this.warnedRegistryAccess = true;
            
            throw new IllegalStateException("Cannot get recipe manager!");
        }
        
        return manager;
    }
    
    @Environment(EnvType.CLIENT)
    @Nullable
    public static ClientPacketListener connectionFromClient() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.connection;
        } else if (Minecraft.getInstance().getConnection() != null) {
            return Minecraft.getInstance().getConnection();
        } else if (Minecraft.getInstance().gameMode != null) {
            // Sometimes the packet is sent way too fast and is between the connection and the level, better safe than sorry
            return Minecraft.getInstance().gameMode.connection;
        }
        
        return null;
    }
    
    @Environment(EnvType.CLIENT)
    private static RegistryAccess registryAccessFromClient() {
        ClientPacketListener connection = connectionFromClient();
        if (connection == null) return null;
        return connection.registryAccess();
    }
    
    @Environment(EnvType.CLIENT)
    private static RecipeManager recipeManagerFromClient() {
        ClientPacketListener connection = connectionFromClient();
        if (connection == null) return null;
        return connection.getRecipeManager();
    }
}
