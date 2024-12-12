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

package me.shedaniel.rei.mixin.fabric;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import me.shedaniel.rei.impl.common.InternalLogger;
import me.shedaniel.rei.plugin.common.displays.tag.TagNodes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagEntry;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(TagGroupLoader.class)
public class MixinTagLoader<T> {
    @Shadow
    @Final
    private String directory;
    
    @Inject(method = "loadPendingTags", at = @At("HEAD"))
    private static <T> void loadPendingTags(ResourceManager resourceManager, Registry<T> registry, CallbackInfoReturnable<Optional<Registry.PendingTagLoad<T>>> cir) {
        RegistryKey<? extends Registry<T>> resourceKey = registry.getKey();
        TagNodes.TAG_DIR_MAP.put(RegistryKeys.getTagPath(resourceKey), resourceKey);
    }
    
    @Inject(method = "build(Ljava/util/Map;)Ljava/util/Map;", at = @At("HEAD"))
    private void load(Map<Identifier, TagGroupLoader.TrackedEntry> map, CallbackInfoReturnable<Map<Identifier, Collection<T>>> cir) {
        TagNodes.RAW_TAG_DATA_MAP.put(directory, new HashMap<>());
        TagNodes.CURRENT_TAG_DIR.set(directory);
    }
    
    @Inject(method = "build(Ljava/util/Map;)Ljava/util/Map;", at = @At("RETURN"))
    private void loadPost(Map<Identifier, TagGroupLoader.TrackedEntry> map, CallbackInfoReturnable<Map<Identifier, Collection<T>>> cir) {
        Map<TagNodes.CollectionWrapper<T>, Identifier> inverseMap = new HashMap<>(cir.getReturnValue().size());
        for (Map.Entry<Identifier, Collection<T>> entry : cir.getReturnValue().entrySet()) {
            inverseMap.put(new TagNodes.CollectionWrapper<>(entry.getValue()), entry.getKey());
        }
        RegistryKey<? extends Registry<?>> resourceKey = TagNodes.TAG_DIR_MAP.get(directory);
        if (resourceKey == null) return;
        TagNodes.TAG_DATA_MAP.put(resourceKey, new HashMap<>());
        Map<Identifier, TagNodes.TagData> tagDataMap = TagNodes.TAG_DATA_MAP.get(resourceKey);
        if (tagDataMap == null) return;
        Registry<T> registry = ((Registry<Registry<T>>) Registries.REGISTRIES).get((RegistryKey<Registry<T>>) resourceKey);
        Stopwatch stopwatch = Stopwatch.createStarted();
        
        Iterator<Map.Entry<TagNodes.CollectionWrapper<?>, TagNodes.RawTagData>> entryIterator = TagNodes.RAW_TAG_DATA_MAP.getOrDefault(directory, Reference2ObjectMaps.emptyMap())
                .entrySet().iterator();
        
        if (!entryIterator.hasNext()) return;
        
        while (entryIterator.hasNext()) {
            Map.Entry<TagNodes.CollectionWrapper<?>, TagNodes.RawTagData> entry = entryIterator.next();
            TagNodes.CollectionWrapper<?> tag = entry.getKey();
            entryIterator.remove();
            
            if (registry != null) {
                Identifier tagLoc = inverseMap.get(tag);
                
                if (tagLoc != null) {
                    TagNodes.RawTagData rawTagData = entry.getValue();
                    IntList elements = new IntArrayList();
                    for (Identifier element : rawTagData.otherElements()) {
                        T t = registry.get(element);
                        if (t != null) {
                            elements.add(registry.getRawId(t));
                        }
                    }
                    tagDataMap.put(tagLoc, new TagNodes.TagData(elements, rawTagData.otherTags()));
                }
            }
        }
        
        InternalLogger.getInstance().debug("Processed %d tags in %s for %s", tagDataMap.size(), stopwatch.stop(), resourceKey.getValue());
    }
    
    @Inject(method = "tryBuildTag", at = @At("RETURN"))
    private void load(TagEntry.ValueGetter<T> lookup, List<TagGroupLoader.TrackedEntry> entries, CallbackInfoReturnable<Either<Collection<TagGroupLoader.TrackedEntry>, Collection<T>>> cir) {
        Collection<T> tag = cir.getReturnValue().right().orElse(null);
        if (tag != null) {
            String currentTagDirectory = TagNodes.CURRENT_TAG_DIR.get();
            if (currentTagDirectory == null) return;
            RegistryKey<? extends Registry<?>> resourceKey = TagNodes.TAG_DIR_MAP.get(currentTagDirectory);
            if (resourceKey == null) return;
            Map<TagNodes.CollectionWrapper<?>, TagNodes.RawTagData> dataMap = TagNodes.RAW_TAG_DATA_MAP.get(currentTagDirectory);
            if (dataMap == null) return;
            List<Identifier> otherElements = new ArrayList<>();
            List<Identifier> otherTags = new ArrayList<>();
            
            for (TagGroupLoader.TrackedEntry builderEntry : entries) {
                TagEntry entry = builderEntry.entry();
                if (entry.tag) {
                    Collection<T> apply = lookup.tag(entry.id);
                    if (apply != null) {
                        otherTags.add(entry.id);
                    }
                } else {
                    T apply = lookup.direct(entry.id, entry.required);
                    if (apply != null) {
                        otherElements.add(entry.id);
                    }
                }
            }
            
            dataMap.put(new TagNodes.CollectionWrapper<>(tag), new TagNodes.RawTagData(CollectionUtils.distinctToList(otherElements), CollectionUtils.distinctToList(otherTags)));
        }
    }
}
