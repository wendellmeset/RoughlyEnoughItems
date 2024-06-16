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

package me.shedaniel.rei.impl.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.impl.client.gui.InternalTextures;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.function.IntSupplier;

public class HoleWidget {
    // 32 for list background, 64 for header and footer
    @Deprecated(forRemoval = true)
    public static Widget create(Rectangle bounds, IntSupplier yOffset, int colorIntensity) {
        return Widgets.withBounds(
                Widgets.concat(
                        createBackground(bounds, yOffset, colorIntensity),
                        createInnerShadow(bounds)
                ),
                bounds
        );
    }
    
    @Deprecated(forRemoval = true)
    public static Widget create(Rectangle bounds, ResourceLocation backgroundLocation, IntSupplier yOffset, int colorIntensity) {
        return Widgets.withBounds(
                Widgets.concat(
                        createBackground(bounds, backgroundLocation, yOffset, colorIntensity),
                        createInnerShadow(bounds)
                ),
                bounds
        );
    }
    
    public static Widget create(Rectangle bounds) {
        return Widgets.withBounds(
                Widgets.concat(
                        createMenuBackground(bounds),
                        createListBorders(bounds)
                ),
                bounds
        );
    }
    
    @Deprecated(forRemoval = true)
    public static Widget createBackground(Rectangle bounds, IntSupplier yOffset, int colorIntensity) {
        return createBackground(bounds, InternalTextures.LEGACY_DIRT, yOffset, colorIntensity);
    }
    
    @Deprecated(forRemoval = true)
    public static Widget createBackground(Rectangle bounds, ResourceLocation backgroundLocation, IntSupplier yOffset, int colorIntensity) {
        return Widgets.withBounds(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
            Tesselator tesselator = Tesselator.getInstance();
            DynamicErrorFreeEntryListWidget.renderBackBackground(graphics, backgroundLocation, bounds.x, bounds.y, bounds.getMaxX(), bounds.getMaxY(), yOffset.getAsInt(), colorIntensity);
        }), bounds);
    }
    
    public static Widget createMenuBackground(Rectangle bounds) {
        return Widgets.withBounds(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
            RenderSystem.enableBlend();
            graphics.blit(ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png"), bounds.x, bounds.y, bounds.getMaxX(), bounds.getMaxY(), bounds.width, bounds.height, 32, 32);
            RenderSystem.disableBlend();
        }), bounds);
    }
    
    @Deprecated(forRemoval = true)
    public static Widget createInnerShadow(Rectangle bounds) {
        return Widgets.withBounds(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
            Tesselator tesselator = Tesselator.getInstance();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 771, 0, 1);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            Matrix4f matrix = graphics.pose().last().pose();
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.addVertex(matrix, bounds.x, bounds.y + 4, 0.0F).setUv(0, 1).setColor(0, 0, 0, 0);
            buffer.addVertex(matrix, bounds.getMaxX(), bounds.y + 4, 0.0F).setUv(1, 1).setColor(0, 0, 0, 0);
            buffer.addVertex(matrix, bounds.getMaxX(), bounds.y, 0.0F).setUv(1, 0).setColor(0, 0, 0, 255);
            buffer.addVertex(matrix, bounds.x, bounds.y, 0.0F).setUv(0, 0).setColor(0, 0, 0, 255);
            buffer.addVertex(matrix, bounds.x, bounds.getMaxY(), 0.0F).setUv(0, 1).setColor(0, 0, 0, 255);
            buffer.addVertex(matrix, bounds.getMaxX(), bounds.getMaxY(), 0.0F).setUv(1, 1).setColor(0, 0, 0, 255);
            buffer.addVertex(matrix, bounds.getMaxX(), bounds.getMaxY() - 4, 0.0F).setUv(1, 0).setColor(0, 0, 0, 0);
            buffer.addVertex(matrix, bounds.x, bounds.getMaxY() - 4, 0.0F).setUv(0, 0).setColor(0, 0, 0, 0);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.disableBlend();
        }), bounds);
    }
    
    public static Widget createListBorders(Rectangle bounds) {
        return Widgets.withBounds(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
            RenderSystem.enableBlend();
            graphics.blit(CreateWorldScreen.HEADER_SEPARATOR, bounds.x, bounds.y - 2, 0.0F, 0.0F, bounds.width, 2, 32, 2);
            graphics.blit(CreateWorldScreen.FOOTER_SEPARATOR, bounds.x, bounds.getMaxY(), 0.0F, 0.0F, bounds.width, 2, 32, 2);
            RenderSystem.disableBlend();
        }), bounds);
    }
}
