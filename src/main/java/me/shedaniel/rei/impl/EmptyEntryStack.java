/*
 * Copyright (c) 2018, 2019, 2020 shedaniel
 * Licensed under the MIT License (the "License").
 */

package me.shedaniel.rei.impl;

import me.shedaniel.math.api.Rectangle;
import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.gui.widget.QueuedTooltip;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@ApiStatus.Internal
public class EmptyEntryStack implements EntryStack {
    
    @Deprecated @ApiStatus.Internal public static final EntryStack EMPTY = new EmptyEntryStack();
    
    private EmptyEntryStack() {
    }
    
    @Override
    public Optional<Identifier> getIdentifier() {
        return Optional.empty();
    }
    
    @Override
    public Type getType() {
        return Type.EMPTY;
    }
    
    @Override
    public int getAmount() {
        return 0;
    }
    
    @Override
    public void setAmount(int amount) {
    
    }
    
    @Override
    public boolean isEmpty() {
        return true;
    }
    
    @Override
    public EntryStack copy() {
        return this;
    }
    
    @Override
    public Object getObject() {
        return null;
    }
    
    @Override
    public boolean equals(EntryStack stack, boolean ignoreTags, boolean ignoreAmount) {
        return stack.getType() == getType();
    }
    
    @Override
    public boolean equalsIgnoreTagsAndAmount(EntryStack stack) {
        return stack.getType() == getType();
    }
    
    @Override
    public boolean equalsIgnoreTags(EntryStack stack) {
        return stack.getType() == getType();
    }
    
    @Override
    public boolean equalsIgnoreAmount(EntryStack stack) {
        return stack.getType() == getType();
    }
    
    @Override
    public boolean equalsAll(EntryStack stack) {
        return stack.getType() == getType();
    }
    
    @Override
    public int getZ() {
        return 0;
    }
    
    @Override
    public void setZ(int z) {
    
    }
    
    @Override
    public <T> EntryStack setting(Settings<T> settings, T value) {
        return this;
    }
    
    @Override
    public <T> EntryStack removeSetting(Settings<T> settings) {
        return this;
    }
    
    @Override
    public EntryStack clearSettings() {
        return this;
    }
    
    @Override
    public <T> T get(Settings<T> settings) {
        return settings.getDefaultValue();
    }
    
    @Override
    @Nullable
    public QueuedTooltip getTooltip(int mouseX, int mouseY) {
        return null;
    }
    
    @Override
    public void render(Rectangle bounds, int mouseX, int mouseY, float delta) {
    
    }
    
    @Override
    public int hashCode() {
        return 0;
    }
}
