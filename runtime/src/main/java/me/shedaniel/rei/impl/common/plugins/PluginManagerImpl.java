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

package me.shedaniel.rei.impl.common.plugins;

import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.mojang.datafixers.util.Pair;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.GameInstance;
import me.shedaniel.rei.RoughlyEnoughItemsCore;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.plugins.PluginView;
import me.shedaniel.rei.api.common.plugins.REIPlugin;
import me.shedaniel.rei.api.common.plugins.REIPluginProvider;
import me.shedaniel.rei.api.common.registry.ReloadStage;
import me.shedaniel.rei.api.common.registry.Reloadable;
import me.shedaniel.rei.impl.common.InternalLogger;
import me.shedaniel.rei.impl.common.logging.performance.PerformanceLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

@ApiStatus.Internal
public class PluginManagerImpl<P extends REIPlugin<?>> implements PluginManager<P>, PluginView<P> {
    private final List<Reloadable<P>> reloadables = new ArrayList<>();
    private final Map<Class<? extends Reloadable<P>>, Reloadable<? super P>> cache = new ConcurrentHashMap<>();
    private final Class<P> pluginClass;
    private final UnaryOperator<PluginView<P>> view;
    @Nullable
    private ReloadStage reloading = null;
    private final List<ReloadStage> observedStages = new ArrayList<>();
    private final List<REIPluginProvider<P>> plugins = new ArrayList<>();
    private final Stopwatch reloadStopwatch = Stopwatch.createUnstarted();
    private boolean forcedMainThread;
    private final Stopwatch forceMainThreadStopwatch = Stopwatch.createUnstarted();
    
    @SafeVarargs
    public PluginManagerImpl(Class<P> pluginClass, UnaryOperator<PluginView<P>> view, Reloadable<? extends P>... reloadables) {
        this.pluginClass = pluginClass;
        this.view = view;
        for (Reloadable<? extends P> reloadable : reloadables) {
            registerReloadable(reloadable);
        }
    }
    
    @Override
    public void registerReloadable(Reloadable<? extends P> reloadable) {
        this.reloadables.add((Reloadable<P>) reloadable);
    }
    
    @Override
    public boolean isReloading() {
        return reloading != null;
    }
    
    @Override
    public <T extends Reloadable<? super P>> T get(Class<T> reloadableClass) {
        Reloadable<? super P> reloadable = this.cache.get(reloadableClass);
        if (reloadable != null) return (T) reloadable;
        
        for (Reloadable<P> r : reloadables) {
            if (reloadableClass.isInstance(r)) {
                this.cache.put((Class<? extends Reloadable<P>>) reloadableClass, r);
                return (T) r;
            }
        }
        throw new IllegalArgumentException("Unknown reloadable type! " + reloadableClass.getName());
    }
    
    @Override
    public List<Reloadable<P>> getReloadables() {
        return Collections.unmodifiableList(reloadables);
    }
    
    @Override
    public PluginView<P> view() {
        return view.apply(this);
    }
    
    @Override
    public void registerPlugin(REIPluginProvider<? extends P> plugin) {
        plugins.add((REIPluginProvider<P>) plugin);
        InternalLogger.getInstance().info("Registered plugin provider %s for %s", plugin.getPluginProviderName(), name(pluginClass));
    }
    
    @Override
    public List<REIPluginProvider<P>> getPluginProviders() {
        return Collections.unmodifiableList(plugins);
    }
    
    @Override
    public FluentIterable<P> getPlugins() {
        return FluentIterable.concat(Iterables.transform(plugins, REIPluginProvider::provide));
    }
    
    private record PluginWrapper<P extends REIPlugin<?>>(P plugin, REIPluginProvider<P> provider) {
        public double getPriority() {
            return plugin.getPriority();
        }
        
        public String getPluginProviderName() {
            String providerName = provider.getPluginProviderName();
            
            if (!provider.provide().isEmpty()) {
                String pluginName = plugin.getPluginProviderName();
                
                if (!providerName.equals(pluginName)) {
                    providerName = pluginName + " of " + providerName;
                }
            }
            
            return providerName;
        }
    }
    
    @SuppressWarnings("RedundantTypeArguments")
    public FluentIterable<PluginWrapper<P>> getPluginWrapped() {
        return FluentIterable.<PluginWrapper<P>>concat(Iterables.<REIPluginProvider<P>, Iterable<PluginWrapper<P>>>transform(plugins, input -> Iterables.<P, PluginWrapper<P>>transform(input.provide(),
                plugin -> new PluginWrapper<>(plugin, input))));
    }
    
    private class SectionClosable implements Closeable {
        private final PluginReloadContext context;
        private final String section;
        private final Stopwatch stopwatch;
        
        public SectionClosable(PluginReloadContext context, String section) {
            this.context = context;
            this.section = section;
            this.stopwatch = Stopwatch.createStarted();
            InternalLogger.getInstance().trace("[" + name(pluginClass) + " " + context.stage() + "] Reloading Section: \"%s\"", section);
        }
        
        @Override
        public void close() {
            this.stopwatch.stop();
            InternalLogger.getInstance().trace("[" + name(pluginClass) + " " + context.stage() + "] Reloading Section: \"%s\" done in %s", this.section, this.stopwatch);
            this.stopwatch.reset();
            try {
                context.interruptionContext().checkInterrupted();
            } catch (InterruptedException exception) {
                ExceptionUtils.rethrow(exception);
            }
        }
    }
    
    private SectionClosable section(PluginReloadContext context, String section) {
        return new SectionClosable(context, section);
    }
    
    @FunctionalInterface
    private interface SectionPluginSink {
        void accept(boolean respectMainThread, Runnable task);
    }
    
    private void pluginSection(PluginReloadContext context, String sectionName, List<PluginWrapper<P>> list, @Nullable Reloadable<?> reloadable, BiConsumer<PluginWrapper<P>, SectionPluginSink> consumer) throws InterruptedException {
        for (PluginWrapper<P> wrapper : list) {
            try (SectionClosable section = section(context, sectionName + wrapper.getPluginProviderName() + "/")) {
                consumer.accept(wrapper, (respectMainThread, runnable) -> {
                    if (!respectMainThread || reloadable == null || !wrapper.plugin.shouldBeForcefullyDoneOnMainThread(reloadable)) {
                        runnable.run();
                    } else {
                        try {
                            forcedMainThread = true;
                            forceMainThreadStopwatch.start();
                            InternalLogger.getInstance().warn("Forcing plugin " + wrapper.getPluginProviderName() + " to run on the main thread for " + sectionName + "! This is extremely dangerous, and have large performance implications.");
                            if (Platform.getEnvironment() == Env.CLIENT) {
                                EnvExecutor.runInEnv(Env.CLIENT, () -> () -> queueExecutionClient(runnable));
                            } else {
                                queueExecution(runnable);
                            }
                        } finally {
                            forceMainThreadStopwatch.stop();
                        }
                    }
                });
            } catch (Throwable throwable) {
                if (throwable instanceof InterruptedException) throw (InterruptedException) throwable;
                InternalLogger.getInstance().error(wrapper.getPluginProviderName() + " plugin failed to " + sectionName + "!", throwable);
            }
        }
    }
    
    private void queueExecution(Runnable runnable) {
        MinecraftServer server = GameInstance.getServer();
        if (server != null) {
            server.executeBlocking(runnable);
        }
    }
    
    private void queueExecutionClient(Runnable runnable) {
        Minecraft.getInstance().executeBlocking(runnable);
    }
    
    @Override
    public void pre(PluginReloadContext context0) throws InterruptedException {
        this.reloading = context0.stage();
        PluginReloadContext context = PluginReloadContext.of(context0.stage(), context0.interruptionContext().withJob(() -> this.reloading = null));
        
        List<PluginWrapper<P>> plugins = new ArrayList<>(getPluginWrapped().toList());
        plugins.sort(Comparator.comparingDouble(PluginWrapper<P>::getPriority).reversed());
        Collections.reverse(plugins);
        InternalLogger.getInstance().debug("========================================");
        InternalLogger.getInstance().debug(name(pluginClass) + " starting pre-reload for " + context.stage() + ".");
        InternalLogger.getInstance().debug("Reloadables (%d):".formatted(reloadables.size()));
        for (Reloadable<P> reloadable : reloadables) {
            InternalLogger.getInstance().debug(" - " + name(reloadable.getClass()));
        }
        InternalLogger.getInstance().debug("Plugins (%d):".formatted(plugins.size()));
        for (PluginWrapper<P> plugin : plugins) {
            InternalLogger.getInstance().debug(" - (%.2f) ".formatted(plugin.getPriority()) + plugin.getPluginProviderName());
        }
        InternalLogger.getInstance().debug("========================================");
        this.forcedMainThread = false;
        this.forceMainThreadStopwatch.reset();
        this.reloadStopwatch.reset().start();
        this.observedStages.clear();
        this.observedStages.add(context.stage());
        try (SectionClosable preRegister = section(context, "pre-register/");
             PerformanceLogger.Plugin perfLogger = RoughlyEnoughItemsCore.PERFORMANCE_LOGGER.stage("Pre Registration")) {
            pluginSection(context, "pre-register/", plugins, null, (plugin, sink) -> {
                try (PerformanceLogger.Plugin.Inner inner = perfLogger.plugin(new Pair<>(plugin.provider, plugin.plugin))) {
                    sink.accept(false, () -> {
                        ((REIPlugin<P>) plugin.plugin).preStage(this, context.stage());
                    });
                }
            });
        } catch (InterruptedException exception) {
            throw exception;
        } catch (Throwable throwable) {
            InternalLogger.getInstance().throwException(new RuntimeException("Failed to run pre registration in stage [" + context.stage() + "]"));
        }
        try (SectionClosable preStageAll = section(context, "pre-stage/");
             PerformanceLogger.Plugin perfLogger = RoughlyEnoughItemsCore.PERFORMANCE_LOGGER.stage("Pre Stage " + context.stage().name())) {
            for (Reloadable<P> reloadable : reloadables) {
                Class<?> reloadableClass = reloadable.getClass();
                try (SectionClosable preStage = section(context, "pre-stage/" + name(reloadableClass) + "/");
                     PerformanceLogger.Plugin.Inner inner = perfLogger.stage(name(reloadableClass))) {
                    reloadable.preStage(context.stage());
                } catch (Throwable throwable) {
                    if (throwable instanceof InterruptedException) throw (InterruptedException) throwable;
                    InternalLogger.getInstance().error("Failed to run pre registration task for reloadable [" + name(reloadableClass) + "] in stage [" + context.stage() + "]", throwable);
                }
            }
        }
        this.reloading = null;
        this.reloadStopwatch.stop();
        InternalLogger.getInstance().debug("========================================");
        InternalLogger.getInstance().debug(name(pluginClass) + " finished pre-reload for " + context.stage() + " in " + reloadStopwatch + ".");
        InternalLogger.getInstance().debug("========================================");
    }
    
    @Override
    public void post(PluginReloadContext context0) throws InterruptedException {
        this.reloading = context0.stage();
        PluginReloadContext context = PluginReloadContext.of(context0.stage(), context0.interruptionContext().withJob(() -> this.reloading = null));
        
        List<PluginWrapper<P>> plugins = new ArrayList<>(getPluginWrapped().toList());
        plugins.sort(Comparator.comparingDouble(PluginWrapper<P>::getPriority).reversed());
        Collections.reverse(plugins);
        InternalLogger.getInstance().debug("========================================");
        InternalLogger.getInstance().debug(name(pluginClass) + " starting post-reload for " + context.stage() + ".");
        InternalLogger.getInstance().debug("Reloadables (%d):".formatted(reloadables.size()));
        for (Reloadable<P> reloadable : reloadables) {
            InternalLogger.getInstance().debug(" - " + name(reloadable.getClass()));
        }
        InternalLogger.getInstance().debug("Plugins (%d):".formatted(plugins.size()));
        for (PluginWrapper<P> plugin : plugins) {
            InternalLogger.getInstance().debug(" - (%.2f) ".formatted(plugin.getPriority()) + plugin.getPluginProviderName());
        }
        InternalLogger.getInstance().debug("========================================");
        this.reloadStopwatch.start();
        Stopwatch postStopwatch = Stopwatch.createStarted();
        try (SectionClosable postRegister = section(context, "post-register/");
             PerformanceLogger.Plugin perfLogger = RoughlyEnoughItemsCore.PERFORMANCE_LOGGER.stage("Post Registration")) {
            pluginSection(context, "post-register/", plugins, null, (plugin, sink) -> {
                try (PerformanceLogger.Plugin.Inner inner = perfLogger.plugin(new Pair<>(plugin.provider, plugin.plugin))) {
                    sink.accept(false, () -> {
                        ((REIPlugin<P>) plugin.plugin).postStage(this, context.stage());
                    });
                }
            });
        } catch (Throwable throwable) {
            if (throwable instanceof InterruptedException) throw (InterruptedException) throwable;
            InternalLogger.getInstance().throwException(new RuntimeException("Failed to run post registration in stage [" + context.stage() + "]"));
        }
        try (SectionClosable postStageAll = section(context, "post-stage/");
             PerformanceLogger.Plugin perfLogger = RoughlyEnoughItemsCore.PERFORMANCE_LOGGER.stage("Pre Stage " + context.stage().name())) {
            for (Reloadable<P> reloadable : reloadables) {
                Class<?> reloadableClass = reloadable.getClass();
                try (SectionClosable postStage = section(context, "post-stage/" + name(reloadableClass) + "/");
                     PerformanceLogger.Plugin.Inner inner = perfLogger.stage(name(reloadableClass))) {
                    reloadable.postStage(context.stage());
                } catch (Throwable throwable) {
                    if (throwable instanceof InterruptedException) throw (InterruptedException) throwable;
                    InternalLogger.getInstance().error("Failed to run post registration task for reloadable [" + name(reloadableClass) + "] in stage [" + context.stage() + "]", throwable);
                }
            }
        }
        this.reloading = null;
        this.reloadStopwatch.stop();
        postStopwatch.stop();
        InternalLogger.getInstance().debug("========================================");
        InternalLogger.getInstance().info(name(pluginClass) + " finished post-reload for " + context.stage() + " in " + postStopwatch + ", totaling " + reloadStopwatch + ".");
        if (forcedMainThread) {
            InternalLogger.getInstance().warn("Forcing plugins to run on main thread took " + forceMainThreadStopwatch);
        }
        InternalLogger.getInstance().debug("========================================");
    }
    
    private static String name(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        if (simpleName.isEmpty()) return clazz.getName();
        return simpleName;
    }
    
    @Override
    public void startReload(ReloadStage stage) {
        try {
            reload(PluginReloadContext.of(stage, ReloadInterruptionContext.ofNever()));
        } catch (InterruptedException e) {
            ExceptionUtils.rethrow(e);
        }
    }
    
    @Override
    public void reload(PluginReloadContext context0) throws InterruptedException {
        try {
            this.reloadStopwatch.start();
            Stopwatch reloadingStopwatch = Stopwatch.createStarted();
            this.reloading = context0.stage();
            PluginReloadContext context = PluginReloadContext.of(context0.stage(), context0.interruptionContext().withJob(() -> this.reloading = null));
            
            // Sort Plugins
            List<PluginWrapper<P>> plugins = new ArrayList<>(getPluginWrapped().toList());
            plugins.sort(Comparator.comparingDouble(PluginWrapper<P>::getPriority).reversed());
            Collections.reverse(plugins);
            
            // Pre Reload
            String line = new String[]{"*", "=", "#", "@", "%", "~", "O", "-", "+"}[new Random().nextInt(9)].repeat(40);
            InternalLogger.getInstance().info(line);
            InternalLogger.getInstance().info(name(pluginClass) + " starting main-reload for " + context.stage() + ".");
            InternalLogger.getInstance().debug("Reloadables (%d):".formatted(reloadables.size()));
            for (Reloadable<P> reloadable : reloadables) {
                InternalLogger.getInstance().debug(" - " + name(reloadable.getClass()));
            }
            InternalLogger.getInstance().info("Plugins (%d):".formatted(plugins.size()));
            for (PluginWrapper<P> plugin : plugins) {
                InternalLogger.getInstance().info(" - (%.2f) ".formatted(plugin.getPriority()) + plugin.getPluginProviderName());
            }
            InternalLogger.getInstance().info(line);
            
            try (SectionClosable startReloadAll = section(context, "start-reload/");
                 PerformanceLogger.Plugin perfLogger = RoughlyEnoughItemsCore.PERFORMANCE_LOGGER.stage("Reload Initialization")) {
                for (Reloadable<P> reloadable : reloadables) {
                    Class<?> reloadableClass = reloadable.getClass();
                    try (SectionClosable startReload = section(context, "start-reload/" + name(reloadableClass) + "/");
                         PerformanceLogger.Plugin.Inner inner = perfLogger.stage(name(reloadableClass))) {
                        reloadable.startReload(context.stage());
                    } catch (Throwable throwable) {
                        if (throwable instanceof InterruptedException) throw (InterruptedException) throwable;
                        InternalLogger.getInstance().error("Failed to run start-reload task for reloadable [" + name(reloadableClass) + "] in stage [" + context.stage() + "]", throwable);
                    }
                }
            }
            
            // Reload
            InternalLogger.getInstance().debug("========================================");
            InternalLogger.getInstance().debug(name(pluginClass) + " started main-reload for " + context.stage() + ".");
            InternalLogger.getInstance().debug("========================================");
            
            for (Reloadable<P> reloadable : getReloadables()) {
                Class<?> reloadableClass = reloadable.getClass();
                try (SectionClosable reloadablePlugin = section(context, "reloadable-plugin/" + name(reloadableClass) + "/");
                     PerformanceLogger.Plugin perfLogger = RoughlyEnoughItemsCore.PERFORMANCE_LOGGER.stage(name(reloadableClass))) {
                    try (PerformanceLogger.Plugin.Inner inner = perfLogger.stage("reloadable-plugin/" + name(reloadableClass) + "/prompt-others-before")) {
                        for (Reloadable<P> listener : reloadables) {
                            try {
                                listener.beforeReloadable(context.stage(), reloadable);
                            } catch (Throwable throwable) {
                                InternalLogger.getInstance().error("Failed to prompt others before reloadable [" + name(reloadableClass) + "] in stage [" + context.stage() + "]", throwable);
                            }
                        }
                    }
                    
                    pluginSection(context, "reloadable-plugin/" + name(reloadableClass) + "/", plugins, reloadable, (plugin, sink) -> {
                        try (PerformanceLogger.Plugin.Inner inner = perfLogger.plugin(new Pair<>(plugin.provider, plugin.plugin))) {
                            sink.accept(true, () -> {
                                for (Reloadable<P> listener : reloadables) {
                                    try {
                                        listener.beforeReloadablePlugin(context.stage(), reloadable, plugin.plugin);
                                    } catch (Throwable throwable) {
                                        InternalLogger.getInstance().error("Failed to run pre-reloadable task for " + plugin.getPluginProviderName() + " before reloadable [" + name(reloadableClass) + "] in stage [" + context.stage() + "]", throwable);
                                    }
                                }
                                
                                try {
                                    reloadable.acceptPlugin(plugin.plugin, context.stage());
                                } finally {
                                    for (Reloadable<P> listener : reloadables) {
                                        try {
                                            listener.afterReloadablePlugin(context.stage(), reloadable, plugin.plugin);
                                        } catch (Throwable throwable) {
                                            InternalLogger.getInstance().error("Failed to run post-reloadable task for " + plugin.getPluginProviderName() + " after reloadable [" + name(reloadableClass) + "] in stage [" + context.stage() + "]", throwable);
                                        }
                                    }
                                }
                            });
                        }
                    });
                    
                    try (PerformanceLogger.Plugin.Inner inner = perfLogger.stage("reloadable-plugin/" + name(reloadableClass) + "/prompt-others-after")) {
                        for (Reloadable<P> listener : reloadables) {
                            try {
                                listener.afterReloadable(context.stage(), reloadable);
                            } catch (Throwable throwable) {
                                InternalLogger.getInstance().error("Failed to prompt others after reloadable [" + name(reloadableClass) + "] in stage [" + context.stage() + "]", throwable);
                            }
                        }
                    }
                }
            }
            
            // Post Reload
            InternalLogger.getInstance().debug("========================================");
            InternalLogger.getInstance().debug(name(pluginClass) + " ending main-reload for " + context.stage() + ".");
            InternalLogger.getInstance().debug("========================================");
            
            try (SectionClosable endReloadAll = section(context, "end-reload/");
                 PerformanceLogger.Plugin perfLogger = RoughlyEnoughItemsCore.PERFORMANCE_LOGGER.stage("Reload Finalization")) {
                for (Reloadable<P> reloadable : reloadables) {
                    Class<?> reloadableClass = reloadable.getClass();
                    try (SectionClosable endReload = section(context, "end-reload/" + name(reloadableClass) + "/");
                         PerformanceLogger.Plugin.Inner inner = perfLogger.stage(name(reloadableClass))) {
                        reloadable.endReload(context.stage());
                    } catch (Throwable throwable) {
                        if (throwable instanceof InterruptedException) throw (InterruptedException) throwable;
                        InternalLogger.getInstance().error("Failed to run end-reload task for reloadable [" + name(reloadableClass) + "] in stage [" + context.stage() + "]", throwable);
                    }
                }
            }
            
            this.reloadStopwatch.stop();
            InternalLogger.getInstance().debug("========================================");
            InternalLogger.getInstance().debug(name(pluginClass) + " ended main-reload for " + context.stage() + " in " + reloadingStopwatch.stop() + ".");
            InternalLogger.getInstance().debug("========================================");
        } catch (Throwable throwable) {
            if (throwable instanceof InterruptedException) throw (InterruptedException) throwable;
            InternalLogger.getInstance().error("Failed to run reload task in stage [" + context0.stage() + "]", throwable);
        } finally {
            reloading = null;
        }
    }
    
    public List<ReloadStage> getObservedStages() {
        return observedStages;
    }
}
