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

package me.shedaniel.rei.api.common.registry.display;

import me.shedaniel.rei.api.client.registry.display.reason.DisplayAdditionReason;
import me.shedaniel.rei.api.client.registry.display.reason.DisplayAdditionReasons;
import me.shedaniel.rei.api.common.display.Display;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public interface DisplayConsumer {
    /**
     * Registers a display.
     *
     * @param display the display
     */
    default void add(Display display) {
        add(display, null);
    }
    
    /**
     * Registers a display with an origin attached.
     *
     * @param display the display
     */
    boolean add(Display display, @Nullable Object origin);
    
    /**
     * Registers a display by the object provided, to be filled during {@link #tryFillDisplay(Object)}.
     *
     * @param object the object to be filled
     */
    default void add(Object object) {
        addWithReason(object, DisplayAdditionReason.NONE);
    }
    
    /**
     * Registers a display by the object provided, to be filled during {@link #tryFillDisplay(Object)}.
     *
     * @param object the object to be filled
     */
    @ApiStatus.Experimental
    default void addWithReason(Object object, DisplayAdditionReason... reasons) {
        if (object instanceof Display) {
            add((Display) object, null);
        } else {
            for (Display display : tryFillDisplay(object, reasons)) {
                add(display, object);
            }
        }
    }
    
    /**
     * Tries to fill displays from {@code T}.
     *
     * @param value the object
     * @param <T>   the type of object
     * @return the collection of displays
     */
    default <T> Collection<Display> tryFillDisplay(T value) {
        return tryFillDisplay(value, DisplayAdditionReason.NONE);
    }
    
    /**
     * Tries to fill displays from {@code T}.
     *
     * @param value the object
     * @param <T>   the type of object
     * @return the collection of displays
     */
    @ApiStatus.Experimental
    <T> Collection<Display> tryFillDisplay(T value, DisplayAdditionReason... reasons);
    
    default <T, D extends Display> FillerBuilder<T, D> beginFiller(Class<T> typeClass) {
        return new FillerBuilder<Object, D>(this, null, null)
                .filterClass(typeClass);
    }
    
    /**
     * Registers a display filler, to be filled during {@link #tryFillDisplay(Object)}.
     * <p>
     * Vanilla {@link Recipe} are by default filled, display filters
     * can be used to automatically generate displays for vanilla {@link Recipe}.
     *
     * @param predicate the predicate of the object
     * @param filler    the filler, taking an object and returning a {@code D}
     * @param <D>       the type of display
     */
    @ApiStatus.Internal
    <D extends Display> void registerFillerWithReason(BiPredicate<?, DisplayAdditionReasons> predicate, BiFunction<?, DisplayAdditionReasons, @Nullable D> filler);
    
    /**
     * Registers a display filler, to be filled during {@link #tryFillDisplay(Object)}.
     * <p>
     * Vanilla {@link Recipe} are by default filled, display filters
     * can be used to automatically generate displays for vanilla {@link Recipe}.
     *
     * @param predicate the predicate of the object
     * @param filler    the filler, taking an object and returning a {@code D}
     * @param <D>       the type of display
     * @since 8.4
     */
    @ApiStatus.Internal
    <D extends Display> void registerDisplaysFillerWithReason(BiPredicate<?, DisplayAdditionReasons> predicate, BiFunction<?, DisplayAdditionReasons, @Nullable Collection<? extends D>> filler);
    
    class FillerBuilder<T, D extends Display> {
        private final DisplayConsumer consumer;
        @Nullable
        private final BiPredicate<?, DisplayAdditionReasons> predicate;
        @Nullable
        private final BiFunction<?, DisplayAdditionReasons, Collection<D>> filler;
        
        private FillerBuilder(DisplayConsumer consumer, @Nullable BiPredicate<?, DisplayAdditionReasons> predicate, @Nullable BiFunction<?, DisplayAdditionReasons, Collection<D>> filler) {
            this.consumer = consumer;
            this.predicate = predicate;
            this.filler = filler;
        }
        
        public <T1> FillerBuilder<T1, D> filterClass(Class<T1> typeClass) {
            return new FillerBuilder<>(consumer, newPredicate(typeClass::isInstance), filler);
        }
        
        public FillerBuilder<T, D> filter(Predicate<T> predicate) {
            return new FillerBuilder<>(consumer, newPredicate(predicate), filler);
        }
        
        public FillerBuilder<T, D> filterWithReason(BiPredicate<T, DisplayAdditionReasons> predicate) {
            return new FillerBuilder<>(consumer, newPredicate(predicate), filler);
        }
        
        public void fill(Function<T, D> filler) {
            consumer.registerFillerWithReason((o, r) -> {
                return predicate == null || ((BiPredicate<Object, DisplayAdditionReasons>) predicate).test(o, r);
            }, (o, r) -> {
                return filler.apply((T) o);
            });
        }
        
        public void fillWithReason(BiFunction<T, DisplayAdditionReasons, D> filler) {
            consumer.registerFillerWithReason((o, r) -> {
                return predicate == null || ((BiPredicate<Object, DisplayAdditionReasons>) predicate).test(o, r);
            }, filler);
        }
        
        public void fillMultiple(Function<T, ? extends Collection<? extends D>> filler) {
            consumer.registerDisplaysFillerWithReason((o, r) -> {
                return predicate == null || ((BiPredicate<Object, DisplayAdditionReasons>) predicate).test(o, r);
            }, (o, r) -> {
                return filler.apply((T) o);
            });
        }
        
        public void fillMultipleWithReason(BiFunction<T, DisplayAdditionReasons, ? extends Collection<? extends D>> filler) {
            consumer.registerDisplaysFillerWithReason((o, r) -> {
                return predicate == null || ((BiPredicate<Object, DisplayAdditionReasons>) predicate).test(o, r);
            }, (o, r) -> {
                return filler.apply((T) o, r);
            });
        }
        
        private BiPredicate<?, DisplayAdditionReasons> newPredicate(Predicate<?> predicate) {
            if (this.predicate == null) return (o, r) -> ((Predicate<Object>) predicate).test(o);
            return ((BiPredicate<Object, DisplayAdditionReasons>) this.predicate).and((o, r) -> ((Predicate<Object>) predicate).test(o));
        }
        
        private BiPredicate<?, DisplayAdditionReasons> newPredicate(BiPredicate<?, DisplayAdditionReasons> predicate) {
            if (this.predicate == null) return predicate;
            return ((BiPredicate<Object, DisplayAdditionReasons>) this.predicate).and((BiPredicate<Object, DisplayAdditionReasons>) predicate);
        }
    }
    
    @FunctionalInterface
    interface FunctionWithId<T, R> {
        R apply(T t, Optional<RecipeDisplayId> id);
    }
    
    @FunctionalInterface
    interface BiFunctionWithId<T, U, R> {
        R apply(T t, U u, Optional<RecipeDisplayId> id);
    }
    
    @FunctionalInterface
    interface PredicateWithId<T> {
        boolean test(T t, Optional<RecipeDisplayId> id);
    }
    
    @FunctionalInterface
    interface BiPredicateWithId<T, U> {
        boolean test(T t, U u, Optional<RecipeDisplayId> id);
    }
    
    interface RecipeDisplayConsumer extends DisplayConsumer {
        default <T extends RecipeDisplay, D extends Display> RecipeFillerBuilder<T, D> beginRecipeFiller(Class<T> typeClass) {
            return new RecipeFillerBuilder<RecipeDisplay, D>(this, null, null)
                    .filterClass(typeClass);
        }
        
        class RecipeFillerBuilder<T extends RecipeDisplay, D extends Display> {
            private final DisplayConsumer consumer;
            @Nullable
            private final BiPredicate<?, DisplayAdditionReasons> predicate;
            @Nullable
            private final BiFunction<?, DisplayAdditionReasons, Collection<D>> filler;
            
            private RecipeFillerBuilder(DisplayConsumer consumer, @Nullable BiPredicate<?, DisplayAdditionReasons> predicate, @Nullable BiFunction<?, DisplayAdditionReasons, Collection<D>> filler) {
                this.consumer = consumer;
                this.predicate = predicate;
                this.filler = filler;
            }
            
            public <T1 extends RecipeDisplay> RecipeFillerBuilder<T1, D> filterClass(Class<T1> typeClass) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(typeClass::isInstance), filler);
            }
            
            public RecipeFillerBuilder<T, D> filter(Predicate<T> predicate) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(predicate), filler);
            }
            
            public RecipeFillerBuilder<T, D> filterWithReason(BiPredicate<T, DisplayAdditionReasons> predicate) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(predicate), filler);
            }
            
            public RecipeFillerBuilder<T, D> filter(PredicateWithId<T> predicate) {
                return new RecipeFillerBuilder<>(consumer, newPredicate((o, r) -> {
                    return predicate.test((T) o, Optional.ofNullable(r.get(DisplayAdditionReason.WithId.class))
                            .map(DisplayAdditionReason.WithId::id));
                }), filler);
            }
            
            public RecipeFillerBuilder<T, D> filterWithReason(BiPredicateWithId<T, DisplayAdditionReasons> predicate) {
                return new RecipeFillerBuilder<>(consumer, newPredicate((o, r) -> {
                    return predicate.test((T) o, r, Optional.ofNullable(r.get(DisplayAdditionReason.WithId.class))
                            .map(DisplayAdditionReason.WithId::id));
                }), filler);
            }
            
            public RecipeFillerBuilder<T, D> filterType(RecipeDisplay.Type<? super T> type) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(o -> o instanceof RecipeDisplay d && d.type() == type), filler);
            }
            
            public RecipeFillerBuilder<T, D> filterType(Predicate<RecipeDisplay.Type<? super T>> predicate) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(o -> o instanceof RecipeDisplay d && ((Predicate<Object>) (Predicate<?>) predicate).test(d.type())), filler);
            }
            
            public void fill(FunctionWithId<T, D> filler) {
                consumer.registerFillerWithReason((o, r) -> {
                    return predicate == null || ((BiPredicate<Object, DisplayAdditionReasons>) predicate).test(o, r);
                }, (o, r) -> {
                    return filler.apply((T) o, Optional.ofNullable(r.get(DisplayAdditionReason.WithId.class))
                            .map(DisplayAdditionReason.WithId::id));
                });
            }
            
            public void fillWithReason(BiFunctionWithId<T, DisplayAdditionReasons, D> filler) {
                consumer.registerFillerWithReason((o, r) -> {
                    return predicate == null || ((BiPredicate<Object, DisplayAdditionReasons>) predicate).test(o, r);
                }, (o, r) -> {
                    return filler.apply((T) o, r, Optional.ofNullable(r.get(DisplayAdditionReason.WithId.class))
                            .map(DisplayAdditionReason.WithId::id));
                });
            }
            
            public void fillMultiple(FunctionWithId<T, ? extends Collection<? extends D>> filler) {
                consumer.registerDisplaysFillerWithReason((o, r) -> {
                    return predicate == null || ((BiPredicate<Object, DisplayAdditionReasons>) predicate).test(o, r);
                }, (o, r) -> {
                    return filler.apply((T) o, Optional.ofNullable(r.get(DisplayAdditionReason.WithId.class))
                            .map(DisplayAdditionReason.WithId::id));
                });
            }
            
            public void fillMultipleWithReason(BiFunctionWithId<T, DisplayAdditionReasons, ? extends Collection<? extends D>> filler) {
                consumer.registerDisplaysFillerWithReason((o, r) -> {
                    return predicate == null || ((BiPredicate<Object, DisplayAdditionReasons>) predicate).test(o, r);
                }, (o, r) -> {
                    return filler.apply((T) o, r, Optional.ofNullable(r.get(DisplayAdditionReason.WithId.class))
                            .map(DisplayAdditionReason.WithId::id));
                });
            }
            
            private BiPredicate<?, DisplayAdditionReasons> newPredicate(Predicate<?> predicate) {
                if (this.predicate == null) return (o, r) -> ((Predicate<Object>) predicate).test(o);
                return ((BiPredicate<Object, DisplayAdditionReasons>) this.predicate).and((o, r) -> ((Predicate<Object>) predicate).test(o));
            }
            
            private BiPredicate<?, DisplayAdditionReasons> newPredicate(BiPredicate<?, DisplayAdditionReasons> predicate) {
                if (this.predicate == null) return predicate;
                return ((BiPredicate<Object, DisplayAdditionReasons>) this.predicate).and((BiPredicate<Object, DisplayAdditionReasons>) predicate);
            }
        }
    }
    
    interface RecipeManagerConsumer extends DisplayConsumer {
        default <T extends Recipe<?>, D extends Display> RecipeFillerBuilder<T, D> beginRecipeFiller(Class<T> typeClass) {
            return new RecipeFillerBuilder<Recipe<?>, D>(this, null, null)
                    .filterClass(typeClass);
        }
        
        class RecipeFillerBuilder<T extends Recipe<?>, D extends Display> {
            private final DisplayConsumer consumer;
            @Nullable
            private final BiPredicate<RecipeHolder<?>, DisplayAdditionReasons> predicate;
            @Nullable
            private final BiFunction<RecipeHolder<?>, DisplayAdditionReasons, Collection<D>> filler;
            
            private RecipeFillerBuilder(DisplayConsumer consumer, @Nullable BiPredicate<RecipeHolder<?>, DisplayAdditionReasons> predicate, @Nullable BiFunction<RecipeHolder<?>, DisplayAdditionReasons, Collection<D>> filler) {
                this.consumer = consumer;
                this.predicate = predicate;
                this.filler = filler;
            }
            
            public <T1 extends Recipe<?>> RecipeFillerBuilder<T1, D> filterClass(Class<T1> typeClass) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(o -> typeClass.isInstance(o.value())), filler);
            }
            
            public RecipeFillerBuilder<T, D> filter(Predicate<RecipeHolder<T>> predicate) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(predicate), filler);
            }
            
            public RecipeFillerBuilder<T, D> filterWithReason(BiPredicate<RecipeHolder<T>, DisplayAdditionReasons> predicate) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(predicate), filler);
            }
            
            public RecipeFillerBuilder<T, D> filterType(RecipeType<? super T> type) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(o -> o.value().getType() == type), filler);
            }
            
            public RecipeFillerBuilder<T, D> filterType(Predicate<RecipeType<? super T>> predicate) {
                return new RecipeFillerBuilder<>(consumer, newPredicate(o -> ((Predicate<Object>) (Predicate<?>) predicate).test(o.value().getType())), filler);
            }
            
            public void fill(Function<RecipeHolder<T>, D> filler) {
                consumer.registerFillerWithReason((o, r) -> {
                    if (o instanceof RecipeHolder<?> holder) {
                        return predicate == null || predicate.test(holder, r);
                    } else {
                        return false;
                    }
                }, (o, r) -> {
                    return filler.apply((RecipeHolder<T>) o);
                });
            }
            
            public void fillWithReason(BiFunction<RecipeHolder<T>, DisplayAdditionReasons, D> filler) {
                consumer.registerFillerWithReason((o, r) -> {
                    if (o instanceof RecipeHolder<?> holder) {
                        return predicate == null || predicate.test(holder, r);
                    } else {
                        return false;
                    }
                }, (o, r) -> {
                    return filler.apply((RecipeHolder<T>) o, r);
                });
            }
            
            public void fillMultiple(Function<RecipeHolder<T>, ? extends Collection<? extends D>> filler) {
                consumer.registerDisplaysFillerWithReason((o, r) -> {
                    if (o instanceof RecipeHolder<?> holder) {
                        return predicate == null || predicate.test(holder, r);
                    } else {
                        return false;
                    }
                }, (o, r) -> {
                    return filler.apply((RecipeHolder<T>) o);
                });
            }
            
            public void fillMultipleWithReason(BiFunction<RecipeHolder<T>, DisplayAdditionReasons, ? extends Collection<? extends D>> filler) {
                consumer.registerDisplaysFillerWithReason((o, r) -> {
                    if (o instanceof RecipeHolder<?> holder) {
                        return predicate == null || predicate.test(holder, r);
                    } else {
                        return false;
                    }
                }, (o, r) -> {
                    return filler.apply((RecipeHolder<T>) o, r);
                });
            }
            
            private BiPredicate<RecipeHolder<?>, DisplayAdditionReasons> newPredicate(Predicate<? extends RecipeHolder<?>> predicate) {
                if (this.predicate == null) return (o, r) -> ((Predicate<RecipeHolder<?>>) predicate).test(o);
                return this.predicate.and((o, r) -> ((Predicate<RecipeHolder<?>>) predicate).test(o));
            }
            
            private BiPredicate<RecipeHolder<?>, DisplayAdditionReasons> newPredicate(BiPredicate<? extends RecipeHolder<?>, DisplayAdditionReasons> predicate) {
                if (this.predicate == null) return (BiPredicate<RecipeHolder<?>, DisplayAdditionReasons>) predicate;
                return this.predicate.and((BiPredicate<RecipeHolder<?>, DisplayAdditionReasons>) predicate);
            }
        }
    }
}
