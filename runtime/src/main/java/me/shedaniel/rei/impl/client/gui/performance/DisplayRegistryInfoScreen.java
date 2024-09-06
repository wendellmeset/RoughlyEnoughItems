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

package me.shedaniel.rei.impl.client.gui.performance;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.shedaniel.clothconfig2.gui.widget.DynamicElementListWidget;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import me.shedaniel.rei.impl.client.gui.modules.Menu;
import me.shedaniel.rei.impl.client.gui.modules.entries.ToggleMenuEntry;
import me.shedaniel.rei.impl.client.gui.screen.ScreenWithMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class DisplayRegistryInfoScreen extends ScreenWithMenu {
    private Runnable onClose;
    
    public DisplayRegistryInfoScreen(Runnable onClose) {
        super(Component.translatable("text.rei.display_registry_analysis"));
        this.onClose = onClose;
    }
    
    private ListWidget list;
    private SortType sortType = SortType.ID;
    
    @Override
    public void init() {
        {
            Component backText = Component.literal("↩ ").append(Component.translatable("gui.back"));
            addRenderableWidget(new Button(4, 4, Minecraft.getInstance().font.width(backText) + 10, 20, backText, button -> {
                this.onClose.run();
                this.onClose = null;
            }));
        }
        {
            Component text = Component.translatable("text.rei.sort");
            Rectangle bounds = new Rectangle(this.width - 4 - Minecraft.getInstance().font.width(text) - 10, 4, Minecraft.getInstance().font.width(text) + 10, 20);
            addRenderableWidget(new Button(bounds.x, bounds.y, bounds.width, bounds.height, text, button -> {
                this.setMenu(new Menu(bounds, CollectionUtils.map(SortType.values(), type -> {
                    return ToggleMenuEntry.of(Component.translatable("text.rei.sort.by", type.name().toLowerCase(Locale.ROOT)), () -> false, o -> {
                        this.closeMenu();
                        this.sortType = type;
                        this.init(this.minecraft, this.width, this.height);
                    });
                }), false));
            }));
        }
        list = new ListWidget();
        list.addItem(new EntryImpl(Component.literal("Total Displays"), DisplayRegistry.getInstance().displaySize()));
        sort(DisplayRegistry.getInstance().getAll().entrySet().stream())
                .forEach(entry -> {
                    list.addItem(new EntryImpl(entry.getKey(), entry.getValue().size()));
                });
        addWidget(list);
    }
    
    private Stream<Map.Entry<CategoryIdentifier<?>, List<Display>>> sort(Stream<Map.Entry<CategoryIdentifier<?>, List<Display>>> stream) {
        return switch (sortType) {
            case COUNT -> stream.sorted(Comparator.<Map.Entry<CategoryIdentifier<?>, List<Display>>>comparingInt(value -> value.getValue().size()).reversed());
            case ID -> stream.sorted(Comparator.comparing(value -> value.getKey().toString()));
        };
    }
    
    @Override
    public void render(PoseStack poses, int mouseX, int mouseY, float delta) {
        renderDirtBackground(0);
        list.render(poses, mouseX, mouseY, delta);
        this.font.drawShadow(poses, this.title.getVisualOrderText(), this.width / 2.0F - this.font.width(this.title) / 2.0F, 12.0F, -1);
        super.render(poses, mouseX, mouseY, delta);
    }
    
    public static abstract class ListEntry extends DynamicElementListWidget.ElementEntry<ListEntry> {
    }
    
    private class ListWidget extends DynamicElementListWidget<ListEntry> {
        public ListWidget() {
            super(DisplayRegistryInfoScreen.this.minecraft, DisplayRegistryInfoScreen.this.width, DisplayRegistryInfoScreen.this.height, 30, DisplayRegistryInfoScreen.this.height, GuiComponent.BACKGROUND_LOCATION);
        }
        
        @Override
        public int getItemWidth() {
            return width;
        }
        
        @Override
        public int addItem(ListEntry item) {
            return super.addItem(item);
        }
        
        @Override
        protected int getScrollbarPosition() {
            return width - 6;
        }
    }
    
    public static class EntryImpl extends ListEntry {
        private final Component component;
        public final int count;
        
        public EntryImpl(CategoryIdentifier<?> identifier, int count) {
            this(Component.literal(identifier.getIdentifier().toString()), count);
        }
        
        public EntryImpl(Component component, int count) {
            this.component = component;
            this.count = count;
        }
        
        @Override
        public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getInstance().font.drawShadow(matrices, this.component.getVisualOrderText(), (float) x + 4, (float) (y + 6), -1);
            FormattedCharSequence rightText = Component.translatable("text.rei.display_registry_analysis.displays", count).getVisualOrderText();
            Minecraft.getInstance().font.drawShadow(matrices, rightText, (float) x + entryWidth - 6 - 8 - Minecraft.getInstance().font.width(rightText), (float) (y + 6), -1);
        }
        
        @Override
        public int getItemHeight() {
            return 24;
        }
        
        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }
        
        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.emptyList();
        }
    }
    
    private enum SortType {
        COUNT,
        ID
    }
}
