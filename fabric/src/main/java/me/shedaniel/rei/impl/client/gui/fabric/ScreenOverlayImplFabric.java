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

package me.shedaniel.rei.impl.client.gui.fabric;

import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.impl.ClientInternals;
import me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScreenOverlayImplFabric extends ScreenOverlayImpl {
    @Override
    public void renderTooltipInner(Screen screen, DrawContext graphics, Tooltip tooltip, int mouseX, int mouseY) {
        List<TooltipComponent> lines = tooltip.entries().stream()
                .flatMap(component -> {
                    if (component.isText()) {
                        List<StringVisitable> texts = MinecraftClient.getInstance().textRenderer.getTextHandler().wrapLines(component.getAsText(), 100000, Style.EMPTY);
                        Stream<OrderedText> sequenceStream = texts.isEmpty() ? Stream.of(component.getAsText().asOrderedText())
                                : texts.stream().map(Language.getInstance()::reorder);
                        return sequenceStream.map(TooltipComponent::of);
                    } else {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
        for (Tooltip.Entry entry : tooltip.entries()) {
            if (entry.isTooltipComponent()) {
                TooltipData component = entry.getAsTooltipComponent();
                
                if (component instanceof TooltipComponent client) {
                    lines.add(client);
                    continue;
                }
                
                try {
                    ClientInternals.getClientTooltipComponent(lines, component);
                } catch (Throwable exception) {
                    throw new IllegalArgumentException("Failed to add tooltip component! " + component + ", Class: " + (component == null ? null : component.getClass().getCanonicalName()), exception);
                }
            }
        }
        renderTooltipInner(graphics, lines, tooltip.getX(), tooltip.getY(), tooltip.getTooltipStyle());
    }
    
    public static void renderTooltipInner(DrawContext graphics, List<TooltipComponent> lines, int mouseX, int mouseY, @Nullable Identifier tooltipStyle) {
        if (lines.isEmpty()) {
            return;
        }
        graphics.getMatrices().push();
        graphics.drawTooltip(MinecraftClient.getInstance().textRenderer, lines, mouseX, mouseY, HoveredTooltipPositioner.INSTANCE, tooltipStyle);
        graphics.getMatrices().pop();
    }
}
