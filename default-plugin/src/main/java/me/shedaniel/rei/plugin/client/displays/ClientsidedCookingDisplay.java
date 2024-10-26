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

package me.shedaniel.rei.plugin.client.displays;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.cooking.CookingDisplay;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public abstract class ClientsidedCookingDisplay extends BasicDisplay implements CookingDisplay, ClientsidedRecipeBookDisplay {
    private final Optional<RecipeDisplayId> id;
    
    public ClientsidedCookingDisplay(FurnaceRecipeDisplay recipe, Optional<RecipeDisplayId> id) {
        this(List.of(EntryIngredients.ofSlotDisplay(recipe.ingredient())),
                List.of(EntryIngredients.ofSlotDisplay(recipe.result())),
                id);
    }
    
    public ClientsidedCookingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<RecipeDisplayId> id) {
        super(inputs, outputs, Optional.empty());
        this.id = id;
    }
    
    @Override
    public OptionalDouble xp() {
        return OptionalDouble.empty();
    }
    
    @Override
    public OptionalDouble cookTime() {
        return OptionalDouble.empty();
    }
    
    @Override
    public Optional<RecipeDisplayId> recipeDisplayId() {
        return id;
    }
    
    protected static <D extends ClientsidedCookingDisplay> DisplaySerializer<D> serializer(Constructor<D> constructor) {
        return DisplaySerializer.of(
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(D::getInputEntries),
                        EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(D::getOutputEntries),
                        Codec.INT.xmap(RecipeDisplayId::new, RecipeDisplayId::index).optionalFieldOf("id").forGetter(D::recipeDisplayId)
                ).apply(instance, constructor::create)),
                StreamCodec.composite(
                        EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
                        D::getInputEntries,
                        EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
                        D::getOutputEntries,
                        ByteBufCodecs.optional(ByteBufCodecs.INT.map(RecipeDisplayId::new, RecipeDisplayId::index)),
                        D::recipeDisplayId,
                        constructor::create
                ), false);
    }
    
    protected interface Constructor<T extends ClientsidedCookingDisplay> {
        T create(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<RecipeDisplayId> id);
    }
    
    public static class Smelting extends ClientsidedCookingDisplay {
        public static DisplaySerializer<Smelting> SERIALIZER = serializer(Smelting::new);
        
        public Smelting(FurnaceRecipeDisplay recipe, Optional<RecipeDisplayId> id) {
            super(recipe, id);
        }
        
        public Smelting(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<RecipeDisplayId> location) {
            super(inputs, outputs, location);
        }
        
        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return BuiltinPlugin.SMELTING;
        }
        
        @Override
        public DisplaySerializer<? extends Display> getSerializer() {
            return SERIALIZER;
        }
    }
    
    public static class Blasting extends ClientsidedCookingDisplay {
        public static DisplaySerializer<Blasting> SERIALIZER = serializer(Blasting::new);
        
        public Blasting(FurnaceRecipeDisplay recipe, Optional<RecipeDisplayId> id) {
            super(recipe, id);
        }
        
        public Blasting(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<RecipeDisplayId> location) {
            super(inputs, outputs, location);
        }
        
        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return BuiltinPlugin.BLASTING;
        }
        
        @Override
        public DisplaySerializer<? extends Display> getSerializer() {
            return SERIALIZER;
        }
    }
    
    public static class Smoking extends ClientsidedCookingDisplay {
        public static DisplaySerializer<Blasting> SERIALIZER = serializer(Blasting::new);
        
        public Smoking(FurnaceRecipeDisplay recipe, Optional<RecipeDisplayId> id) {
            super(recipe, id);
        }
        
        public Smoking(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<RecipeDisplayId> location) {
            super(inputs, outputs, location);
        }
        
        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return BuiltinPlugin.SMOKING;
        }
        
        @Override
        public DisplaySerializer<? extends Display> getSerializer() {
            return SERIALIZER;
        }
    }
}
