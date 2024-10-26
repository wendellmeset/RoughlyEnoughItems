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
import me.shedaniel.rei.plugin.common.displays.crafting.CraftingDisplay;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;

import java.util.List;
import java.util.Optional;

public abstract class ClientsidedCraftingDisplay extends BasicDisplay implements CraftingDisplay, ClientsidedRecipeBookDisplay {
    private final Optional<RecipeDisplayId> id;
    
    public ClientsidedCraftingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<RecipeDisplayId> id) {
        super(inputs, outputs, Optional.empty());
        this.id = id;
    }
    
    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return BuiltinPlugin.CRAFTING;
    }
    
    @Override
    public Optional<RecipeDisplayId> recipeDisplayId() {
        return id;
    }
    
    public static class Shaped extends ClientsidedCraftingDisplay {
        public static final DisplaySerializer<Shaped> SERIALIZER = DisplaySerializer.of(
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(Shaped::getInputEntries),
                        EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(Shaped::getOutputEntries),
                        Codec.INT.xmap(RecipeDisplayId::new, RecipeDisplayId::index).optionalFieldOf("id").forGetter(Shaped::recipeDisplayId),
                        Codec.INT.fieldOf("width").forGetter(Shaped::getWidth),
                        Codec.INT.fieldOf("height").forGetter(Shaped::getHeight)
                ).apply(instance, Shaped::new)),
                StreamCodec.composite(
                        EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
                        Shaped::getInputEntries,
                        EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
                        Shaped::getOutputEntries,
                        ByteBufCodecs.optional(ByteBufCodecs.INT.map(RecipeDisplayId::new, RecipeDisplayId::index)),
                        Shaped::recipeDisplayId,
                        ByteBufCodecs.INT,
                        Shaped::getWidth,
                        ByteBufCodecs.INT,
                        Shaped::getHeight,
                        Shaped::new
                ), false);
        
        private final int width;
        private final int height;
        
        public Shaped(ShapedCraftingRecipeDisplay recipe, Optional<RecipeDisplayId> id) {
            super(EntryIngredients.ofSlotDisplays(recipe.ingredients()),
                    List.of(EntryIngredients.ofSlotDisplay(recipe.result())),
                    id);
            this.width = recipe.width();
            this.height = recipe.height();
        }
        
        public Shaped(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<RecipeDisplayId> id, int width, int height) {
            super(inputs, outputs, id);
            this.width = width;
            this.height = height;
        }
        
        @Override
        public boolean isShapeless() {
            return false;
        }
        
        @Override
        public int getWidth() {
            return width;
        }
        
        @Override
        public int getHeight() {
            return height;
        }
        
        @Override
        public DisplaySerializer<? extends Display> getSerializer() {
            return SERIALIZER;
        }
    }
    
    public static class Shapeless extends ClientsidedCraftingDisplay {
        public static final DisplaySerializer<Shapeless> SERIALIZER = DisplaySerializer.of(
                RecordCodecBuilder.mapCodec(instance -> instance.group(
                        EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(Shapeless::getInputEntries),
                        EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(Shapeless::getOutputEntries),
                        Codec.INT.xmap(RecipeDisplayId::new, RecipeDisplayId::index).optionalFieldOf("id").forGetter(Shapeless::recipeDisplayId)
                ).apply(instance, Shapeless::new)),
                StreamCodec.composite(
                        EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
                        Shapeless::getInputEntries,
                        EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
                        Shapeless::getOutputEntries,
                        ByteBufCodecs.optional(ByteBufCodecs.INT.map(RecipeDisplayId::new, RecipeDisplayId::index)),
                        Shapeless::recipeDisplayId,
                        Shapeless::new
                ), false);
        
        public Shapeless(ShapelessCraftingRecipeDisplay recipe, Optional<RecipeDisplayId> id) {
            super(EntryIngredients.ofSlotDisplays(recipe.ingredients()),
                    List.of(EntryIngredients.ofSlotDisplay(recipe.result())),
                    id);
        }
        
        public Shapeless(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<RecipeDisplayId> id) {
            super(inputs, outputs, id);
        }
        
        @Override
        public boolean isShapeless() {
            return true;
        }
        
        @Override
        public int getWidth() {
            return getInputEntries().size() > 4 ? 3 : 2;
        }
        
        @Override
        public int getHeight() {
            return getInputEntries().size() > 4 ? 3 : 2;
        }
        
        @Override
        public int getInputWidth(int craftingWidth, int craftingHeight) {
            return craftingWidth * craftingHeight <= getInputEntries().size() ? craftingWidth : Math.min(getInputEntries().size(), 3);
        }
        
        @Override
        public int getInputHeight(int craftingWidth, int craftingHeight) {
            return (int) Math.ceil(getInputEntries().size() / (double) getInputWidth(craftingWidth, craftingHeight));
        }
        
        @Override
        public DisplaySerializer<? extends Display> getSerializer() {
            return SERIALIZER;
        }
    }
}
