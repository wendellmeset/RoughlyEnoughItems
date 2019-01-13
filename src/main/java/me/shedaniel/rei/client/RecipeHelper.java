package me.shedaniel.rei.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import me.shedaniel.rei.RoughlyEnoughItemsCore;
import me.shedaniel.rei.api.IRecipeCategory;
import me.shedaniel.rei.api.IRecipeCategoryCraftable;
import me.shedaniel.rei.api.IRecipeDisplay;
import me.shedaniel.rei.api.IRecipePlugin;
import me.shedaniel.rei.listeners.RecipeSync;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

public class RecipeHelper implements RecipeSync {
    
    private static Map<Identifier, List<IRecipeDisplay>> recipeCategoryListMap;
    private static List<IRecipeCategory> categories;
    private static RecipeManager recipeManager;
    private static Map<Class, IRecipeCategoryCraftable> craftables;
    
    public RecipeHelper() {
        this.recipeCategoryListMap = Maps.newHashMap();
        this.categories = Lists.newArrayList();
        this.craftables = Maps.newHashMap();
    }
    
    public static List<ItemStack> findCraftableByItems(List<ItemStack> inventoryItems) {
        List<ItemStack> craftables = new ArrayList<>();
        for(List<IRecipeDisplay> value : recipeCategoryListMap.values())
            for(IRecipeDisplay recipeDisplay : value) {
                int slotsCraftable = 0;
                List<List<ItemStack>> requiredInput = (List<List<ItemStack>>) recipeDisplay.getRequiredItems();
                for(List<ItemStack> slot : requiredInput) {
                    if (slot.isEmpty()) {
                        slotsCraftable++;
                        continue;
                    }
                    boolean slotDone = false;
                    for(ItemStack possibleType : inventoryItems) {
                        for(ItemStack slotPossible : slot)
                            if (ItemStack.areEqualIgnoreTags(slotPossible, possibleType)) {
                                slotsCraftable++;
                                slotDone = true;
                                break;
                            }
                        if (slotDone)
                            break;
                    }
                }
                if (slotsCraftable == recipeDisplay.getRequiredItems().size())
                    craftables.addAll((List<ItemStack>) recipeDisplay.getOutput());
            }
        return craftables.stream().distinct().collect(Collectors.toList());
    }
    
    public static void registerCategory(IRecipeCategory category) {
        categories.add(0, category);
        recipeCategoryListMap.put(category.getIdentifier(), Lists.newArrayList());
    }
    
    public static void registerRecipe(Identifier categoryIdentifier, IRecipeDisplay display) {
        if (!recipeCategoryListMap.containsKey(categoryIdentifier))
            return;
        recipeCategoryListMap.get(categoryIdentifier).add(display);
    }
    
    public static Map<IRecipeCategory, List<IRecipeDisplay>> getRecipesFor(ItemStack stack) {
        Map<Identifier, List<IRecipeDisplay>> categoriesMap = new HashMap<>();
        categories.forEach(f -> categoriesMap.put(f.getIdentifier(), new LinkedList<>()));
        for(List<IRecipeDisplay> value : recipeCategoryListMap.values())
            for(IRecipeDisplay recipeDisplay : value)
                for(ItemStack outputStack : (List<ItemStack>) recipeDisplay.getOutput())
                    if (ItemStack.areEqualIgnoreTags(stack, outputStack))
                        categoriesMap.get(recipeDisplay.getRecipeCategory()).add(recipeDisplay);
        categoriesMap.keySet().removeIf(f -> categoriesMap.get(f).isEmpty());
        Map<IRecipeCategory, List<IRecipeDisplay>> recipeCategoryListMap = Maps.newHashMap();
        categories.forEach(category -> {
            if (categoriesMap.containsKey(category.getIdentifier()))
                recipeCategoryListMap.put(category, categoriesMap.get(category.getIdentifier()));
        });
        return recipeCategoryListMap;
    }
    
    public static RecipeManager getRecipeManager() {
        return recipeManager;
    }
    
    public static Map<IRecipeCategory, List<IRecipeDisplay>> getUsagesFor(ItemStack stack) {
        Map<Identifier, List<IRecipeDisplay>> categoriesMap = new HashMap<>();
        categories.forEach(f -> categoriesMap.put(f.getIdentifier(), new LinkedList<>()));
        for(List<IRecipeDisplay> value : recipeCategoryListMap.values())
            for(IRecipeDisplay recipeDisplay : value) {
                boolean found = false;
                for(List<ItemStack> input : (List<List<ItemStack>>) recipeDisplay.getInput()) {
                    for(ItemStack itemStack : input) {
                        if (ItemStack.areEqualIgnoreTags(itemStack, stack)) {
                            categoriesMap.get(recipeDisplay.getRecipeCategory()).add(recipeDisplay);
                            found = true;
                            break;
                        }
                    }
                    if (found)
                        break;
                }
            }
        categoriesMap.keySet().removeIf(f -> categoriesMap.get(f).isEmpty());
        Map<IRecipeCategory, List<IRecipeDisplay>> recipeCategoryListMap = Maps.newHashMap();
        categories.forEach(category -> {
            if (categoriesMap.containsKey(category.getIdentifier()))
                recipeCategoryListMap.put(category, categoriesMap.get(category.getIdentifier()));
        });
        return recipeCategoryListMap;
    }
    
    public static List<IRecipeCategory> getCategories() {
        return categories;
    }
    
    public static void registerCategoryCraftable(Class<? extends IRecipeDisplay> guiClass, IRecipeCategoryCraftable categoryCraftable) {
        craftables.put(guiClass, categoryCraftable);
    }
    
    public static void registerCategoryCraftable(Class<? extends IRecipeDisplay>[] guiClasses, IRecipeCategoryCraftable categoryCraftable) {
        for(Class<? extends IRecipeDisplay> guiClass : guiClasses) craftables.put(guiClass, categoryCraftable);
    }
    
    public static IRecipeCategoryCraftable getCategoryCraftable(IRecipeDisplay gui) {
        if (!craftables.containsKey(gui.getClass()))
            return null;
        return craftables.get(gui.getClass());
    }
    
    @Override
    public void recipesLoaded(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
        this.recipeCategoryListMap.clear();
        this.categories.clear();
        this.craftables.clear();
        RoughlyEnoughItemsCore.getListeners(IRecipePlugin.class).forEach(plugin -> {
            plugin.registerPluginCategories();
            plugin.registerRecipes();
            plugin.registerAutoCraftingGui();
        });
        Collections.reverse(categories);
        RoughlyEnoughItemsCore.LOGGER.info("Registered REI Categories: " + String.join(", ", categories.stream().map(category -> {
            return category.getCategoryName();
        }).collect(Collectors.toList())));
    }
    
}
