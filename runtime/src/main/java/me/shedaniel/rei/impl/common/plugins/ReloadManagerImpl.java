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
import com.google.common.base.Suppliers;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import me.shedaniel.rei.RoughlyEnoughItemsCore;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.plugins.REIPlugin;
import me.shedaniel.rei.api.common.registry.ReloadStage;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import me.shedaniel.rei.impl.common.InternalLogger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

@ApiStatus.Internal
public class ReloadManagerImpl {
    private static final Supplier<Executor> RELOAD_PLUGINS = Suppliers.memoize(() -> Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "REI-ReloadPlugins");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(($, exception) -> {
            if (exception instanceof InterruptedException) {
                InternalLogger.getInstance().debug("Interrupted while reloading plugins, could be caused by a new request to reload plugins!", new UncaughtException(exception));
                return;
            }
            
            InternalLogger.getInstance().throwException(new UncaughtException(exception));
        });
        return thread;
    }));
    
    private static final List<Task> RELOAD_TASKS = new CopyOnWriteArrayList<>();
    
    private static class Task {
        private final Future<?> future;
        private boolean interrupted = false;
        private boolean completed = false;
        
        public Task(Future<?> future) {
            this.future = future;
        }
    }
    
    private static Executor executor() {
        if (usesREIThread()) {
            return RELOAD_PLUGINS.get();
        } else {
            return runnable -> {
                try {
                    runnable.run();
                } catch (Throwable throwable) {
                    InternalLogger.getInstance().throwException(throwable);
                }
            };
        }
    }
    
    private static boolean usesREIThread() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            return usesREIThreadClient();
        } else {
            return false;
        }
    }
    
    @Environment(EnvType.CLIENT)
    private static boolean usesREIThreadClient() {
        return ConfigObject.getInstance().doesRegisterRecipesInAnotherThread();
    }
    
    public static int countRunningReloadTasks() {
        return CollectionUtils.sumInt(RELOAD_TASKS, task -> !task.future.isDone() || !task.completed ? 1 : 0);
    }
    
    public static int countUninterruptedRunningReloadTasks() {
        return CollectionUtils.sumInt(RELOAD_TASKS, task -> !task.interrupted && (!task.future.isDone() || !task.completed) ? 1 : 0);
    }
    
    public static void reloadPlugins(@Nullable ReloadStage start, ReloadInterruptionContext interruptionContext) {
        InternalLogger.getInstance().debug("Starting Reload Plugins of stage " + start, new Throwable());
        if (usesREIThread()) {
            if ((start == ReloadStage.START || start == null) && countRunningReloadTasks() > 0) {
                InternalLogger.getInstance().warn("Trying to start reload plugins of stage %s but found %d existing reload task(s)!", start, countRunningReloadTasks());
                terminateReloadTasks();
            }
            
            if (!RELOAD_TASKS.isEmpty()) {
                InternalLogger.getInstance().warn("Found %d existing reload task(s) after trying to terminate them!", RELOAD_TASKS.size());
            }
            
            Task[] task = new Task[1];
            Future<?> future = CompletableFuture.runAsync(() -> reloadPlugins0(start, () -> interruptionContext.isInterrupted() || (task[0] != null && task[0].interrupted)), executor())
                    .whenComplete((unused, throwable) -> {
                        // Remove the future from the list of futures
                        if (task[0] != null) {
                            task[0].completed = true;
                            RELOAD_TASKS.remove(task[0]);
                            task[0] = null;
                        }
                    });
            task[0] = new Task(future);
            RELOAD_TASKS.add(task[0]);
        } else {
            reloadPlugins0(start, interruptionContext);
        }
    }
    
    private static void reloadPlugins0(@Nullable ReloadStage stage, ReloadInterruptionContext interruptionContext) {
        if (stage == null) {
            for (ReloadStage reloadStage : ReloadStage.values()) {
                reloadPlugins0(reloadStage, interruptionContext);
            }
        } else {
            reloadPlugins0(PluginReloadContext.of(stage, interruptionContext));
        }
    }
    
    private static void reloadPlugins0(PluginReloadContext context) {
        if (context.stage() == ReloadStage.START) RoughlyEnoughItemsCore.PERFORMANCE_LOGGER.clear();
        try {
            for (PluginManager<? extends REIPlugin<?>> instance : PluginManager.getActiveInstances()) {
                instance.view().pre(context);
            }
            for (PluginManager<? extends REIPlugin<?>> instance : PluginManager.getActiveInstances()) {
                instance.view().reload(context);
            }
            for (PluginManager<? extends REIPlugin<?>> instance : PluginManager.getActiveInstances()) {
                instance.view().post(context);
            }
        } catch (InterruptedException e) {
            InternalLogger.getInstance().debug("Interrupted while reloading plugins, could be caused by a new request to reload plugins!", e);
        } catch (Throwable throwable) {
            InternalLogger.getInstance().throwException(throwable);
        }
    }
    
    public static void terminateReloadTasks() {
        if (countUninterruptedRunningReloadTasks() == 0) {
            InternalLogger.getInstance().debug("Did not fulfill the request of termination of REI reload tasks because there are no uninterrupted running tasks. This is not an error.");
            RELOAD_TASKS.clear();
            return;
        }
        
        InternalLogger.getInstance().debug("Requested the termination of REI reload tasks.");
        
        for (Task task : RELOAD_TASKS) {
            task.interrupted = true;
        }
        
        long startTerminateTime = System.currentTimeMillis();
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (countRunningReloadTasks() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                InternalLogger.getInstance().error("Thread interrupted while waiting for reload tasks to terminate!", e);
            }
            
            if (System.currentTimeMillis() - startTerminateTime > 5000) {
                InternalLogger.getInstance().error("Took too long to terminate reload tasks (over 5 seconds)! Now forcefully terminating them!");
                for (Task task : RELOAD_TASKS) {
                    task.future.cancel(Platform.isFabric());
                }
                break;
            }
        }
        
        if (countRunningReloadTasks() == 0) {
            RELOAD_TASKS.clear();
            InternalLogger.getInstance().debug("Successfully terminated reload tasks in %s", stopwatch.stop());
        } else {
            InternalLogger.getInstance().error("Failed to terminate reload tasks! Found %d running tasks!", countRunningReloadTasks());
        }
    }
    
    private static class UncaughtException extends Exception {
        public UncaughtException(Throwable cause) {
            super(cause);
        }
    }
}
