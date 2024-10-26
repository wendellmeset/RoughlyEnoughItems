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

package me.shedaniel.rei.plugin.client.categories.cooking;

import com.google.common.collect.Lists;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.DisplayRenderer;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.SimpleDisplayRenderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.plugin.common.displays.cooking.CookingDisplay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public class DefaultCookingCategory implements DisplayCategory<CookingDisplay> {
    private CategoryIdentifier<? extends CookingDisplay> identifier;
    private EntryStack<?> logo;
    private String categoryName;
    private double defaultCookingTime;
    
    public DefaultCookingCategory(CategoryIdentifier<? extends CookingDisplay> identifier, EntryStack<?> logo, String categoryName, double defaultCookingTime) {
        this.identifier = identifier;
        this.logo = logo;
        this.categoryName = categoryName;
        this.defaultCookingTime = defaultCookingTime;
    }
    
    @Override
    public List<Widget> setupDisplay(CookingDisplay display, Rectangle bounds) {
        Point startPoint = new Point(bounds.getCenterX() - 41, bounds.y + 10);
        DecimalFormat df = new DecimalFormat("###.##");
        List<Widget> widgets = Lists.newArrayList();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createResultSlotBackground(new Point(startPoint.x + 61, startPoint.y + 9)));
        widgets.add(Widgets.createBurningFire(new Point(startPoint.x + 1, startPoint.y + 20))
                .animationDurationMS(10000));
        if (display.cookTime().isPresent() && display.xp().isPresent()) {
            widgets.add(Widgets.createLabel(new Point(bounds.x + bounds.width - 5, bounds.y + 5),
                    Component.translatable("category.rei.cooking.time&xp", df.format(display.xp().getAsDouble()), df.format(display.cookTime().getAsDouble() / 20d))).noShadow().rightAligned().color(0xFF404040, 0xFFBBBBBB));
        }
        widgets.add(Widgets.createArrow(new Point(startPoint.x + 24, startPoint.y + 8))
                .animationDurationTicks(display.cookTime().orElse(defaultCookingTime)));
        widgets.add(Widgets.createSlot(new Point(startPoint.x + 61, startPoint.y + 9))
                .entries(display.getOutputEntries().get(0))
                .disableBackground()
                .markOutput());
        widgets.add(Widgets.createSlot(new Point(startPoint.x + 1, startPoint.y + 1))
                .entries(display.getInputEntries().get(0))
                .markInput());
        return widgets;
    }
    
    @Override
    public DisplayRenderer getDisplayRenderer(CookingDisplay display) {
        return SimpleDisplayRenderer.from(Collections.singletonList(display.getInputEntries().get(0)), display.getOutputEntries());
    }
    
    @Override
    public int getDisplayHeight() {
        return 49;
    }
    
    @Override
    public CategoryIdentifier<? extends CookingDisplay> getCategoryIdentifier() {
        return identifier;
    }
    
    @Override
    public Renderer getIcon() {
        return logo;
    }
    
    @Override
    public Component getTitle() {
        return Component.translatable(categoryName);
    }
}
