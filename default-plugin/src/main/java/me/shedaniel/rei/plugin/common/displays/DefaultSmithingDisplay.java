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

package me.shedaniel.rei.plugin.common.displays;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.*;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DefaultSmithingDisplay extends BasicDisplay {
    @Nullable
    private final SmithingRecipeType type;
    
    @ApiStatus.Experimental
    public DefaultSmithingDisplay(SmithingTransformRecipe recipe) {
        this(
                recipe,
                List.of(
                        EntryIngredients.ofIngredient(recipe.template),
                        EntryIngredients.ofIngredient(recipe.base),
                        EntryIngredients.ofIngredient(recipe.addition)
                )
        );
    }
    
    @Deprecated(forRemoval = true)
    public DefaultSmithingDisplay(SmithingTrimRecipe recipe) {
        this(
                recipe,
                List.of(
                        EntryIngredients.ofIngredient(recipe.template),
                        EntryIngredients.ofIngredient(recipe.base),
                        EntryIngredients.ofIngredient(recipe.addition)
                )
        );
    }
    
    @ApiStatus.Experimental
    public static List<DefaultSmithingDisplay> from(SmithingTrimRecipe recipe) {
        RegistryAccess registryAccess = BasicDisplay.registryAccess();
        List<DefaultSmithingDisplay> displays = new ArrayList<>();
        for (ItemStack templateItem : recipe.template.getItems()) {
            Holder.Reference<TrimPattern> trimPattern = TrimPatterns.getFromTemplate(registryAccess, templateItem)
                    .orElse(null);
            if (trimPattern == null) continue;
            
            for (ItemStack additionStack : recipe.addition.getItems()) {
                Holder.Reference<TrimMaterial> trimMaterial = TrimMaterials.getFromIngredient(registryAccess, additionStack)
                        .orElse(null);
                if (trimMaterial == null) continue;
                
                EntryIngredient baseIngredient = EntryIngredients.ofIngredient(recipe.base);
                EntryIngredient templateOutput = baseIngredient.isEmpty() ? EntryIngredient.empty()
                        : getTrimmingOutput(registryAccess, EntryStacks.of(templateItem), baseIngredient.get(0), EntryStacks.of(additionStack));
                
                displays.add(new DefaultSmithingDisplay(List.of(
                        EntryIngredients.of(templateItem),
                        baseIngredient,
                        EntryIngredients.of(additionStack)
                ), List.of(templateOutput), SmithingRecipeType.TRIM, Optional.ofNullable(recipe.getId())));
            }
        }
        return displays;
    }
    
    public DefaultSmithingDisplay(SmithingRecipe recipe, List<EntryIngredient> inputs) {
        this(
                inputs,
                List.of(EntryIngredients.of(recipe.getResultItem(BasicDisplay.registryAccess()))),
                Optional.ofNullable(recipe.getId())
        );
    }
    
    public DefaultSmithingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<ResourceLocation> location) {
        this(inputs, outputs, null, location);
    }
    
    @ApiStatus.Experimental
    public DefaultSmithingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, @Nullable SmithingRecipeType type, Optional<ResourceLocation> location) {
        super(inputs, outputs, location);
        this.type = type;
    }
    
    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return BuiltinPlugin.SMITHING;
    }
    
    @ApiStatus.Experimental
    @Nullable
    public SmithingRecipeType getType() {
        return type;
    }
    
    public static BasicDisplay.Serializer<DefaultSmithingDisplay> serializer() {
        return BasicDisplay.Serializer.of((input, output, id, tag) -> {
            SmithingRecipeType type = tag.contains("Type") ? SmithingRecipeType.valueOf(tag.getString("Type")) : null;
            return new DefaultSmithingDisplay(input, output, type, id);
        }, (display, tag) -> {
            if (display.type != null) tag.putString("Type", display.type.name());
        });
    }
    
    @ApiStatus.Experimental
    public enum SmithingRecipeType {
        TRIM,
        TRANSFORM
    }
    
    @ApiStatus.Experimental
    @ApiStatus.Internal
    public static EntryIngredient getTrimmingOutput(RegistryAccess registryAccess, EntryStack<?> template, EntryStack<?> base, EntryStack<?> addition) {
        if (template.getType() != VanillaEntryTypes.ITEM || base.getType() != VanillaEntryTypes.ITEM || addition.getType() != VanillaEntryTypes.ITEM) return EntryIngredient.empty();
        ItemStack templateItem = template.castValue();
        ItemStack baseItem = base.castValue();
        ItemStack additionItem = addition.castValue();
        Holder.Reference<TrimPattern> trimPattern = TrimPatterns.getFromTemplate(registryAccess, templateItem)
                .orElse(null);
        if (trimPattern == null) return EntryIngredient.empty();
        Holder.Reference<TrimMaterial> trimMaterial = TrimMaterials.getFromIngredient(registryAccess, additionItem)
                .orElse(null);
        if (trimMaterial == null) return EntryIngredient.empty();
        ArmorTrim armorTrim = new ArmorTrim(trimMaterial, trimPattern);
        Optional<ArmorTrim> trim = ArmorTrim.getTrim(registryAccess, baseItem);
        if (trim.isPresent() && trim.get().hasPatternAndMaterial(trimPattern, trimMaterial)) return EntryIngredient.empty();
        ItemStack newItem = baseItem.copy();
        newItem.setCount(1);
        if (ArmorTrim.setTrim(registryAccess, newItem, armorTrim)) {
            return EntryIngredients.of(newItem);
        } else return EntryIngredient.empty();
    }
}
