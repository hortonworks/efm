/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the
 * terms of the Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with
 * a third party authorized to distribute this code.  If you do not have a written agreement with Cloudera
 * or with an authorized and properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS
 *      CODE, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT,
 *      MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS
 *      FOR ANY CLAIMS ARISING FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA
 *      IS NOT LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL
 *      DAMAGES INCLUDING, BUT NOT LIMITED TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF
 *      INCOME, LOSS OF BUSINESS ADVANTAGE OR UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.service;

import com.cloudera.cem.efm.util.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class BackgroundTaskScheduler implements DisposableBean, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundTaskScheduler.class);

    protected final String taskSchedulerName;
    protected final long taskIntervalMs;
    protected final Runnable task;
    protected final boolean stopOnErrors;
    protected final ScheduledExecutorService executorService;
    protected Future futureTasks;

    public BackgroundTaskScheduler(final Runnable task, final Duration taskInterval) {
        this(task, taskInterval, false);
    }

    public BackgroundTaskScheduler(final Runnable task, final Duration taskInterval, boolean stopOnErrors) {
        this(task, taskInterval, stopOnErrors, null);
    }

    public BackgroundTaskScheduler(final Runnable task, final Duration taskInterval, boolean stopOnErrors, ScheduledExecutorService executorService) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        if (taskInterval.isZero() || taskInterval.isNegative()) {
            throw new IllegalArgumentException("taskInterval must be greater than zero");
        }
        this.taskSchedulerName = this.getClass().getSimpleName();
        this.task = task;
        this.taskIntervalMs = taskInterval.toMillis();
        this.stopOnErrors = stopOnErrors;
        this.executorService = executorService != null ? executorService : defaultScheduledExecutorService(taskSchedulerName);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        LOGGER.info("Starting background processing for {} to run every {}ms", taskSchedulerName, taskIntervalMs);

        final Runnable wrappedTask = stopOnErrors ? task : () -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.error("", t);
            }
        };

        futureTasks = executorService.scheduleAtFixedRate(wrappedTask, taskIntervalMs, taskIntervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() throws Exception {
        try {
            // Disable new tasks from being submitted
            futureTasks.cancel(false);
            executorService.shutdown();
            try {
                // Blocking wait for existing tasks to complete. The timeout could perhaps be made configurable by the subclass.
                LOGGER.info("Waiting for in progress background tasks ({}) to complete...", taskSchedulerName);
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Timeout reached while waiting for background tasks ({}). Shutting down now. ", taskSchedulerName);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            LOGGER.error("Error shutting down " + taskSchedulerName + " task", e);
        }
    }

    private static ScheduledExecutorService defaultScheduledExecutorService(final String taskName) {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(defaultThreadFactory(taskName));
        if (executorService instanceof ScheduledThreadPoolExecutor) {
            // Remove cancelled tasks from the queue, so that we can shutdown quickly and gracefully
            // See: https://stackoverflow.com/questions/14423449/how-to-remove-a-task-from-scheduledexecutorservice
            //      https://stackoverflow.com/questions/36747987/how-to-setremoveoncancelpolicy-for-executors-newscheduledthreadpool5/36748183#36748183
            ((ScheduledThreadPoolExecutor) executorService).setRemoveOnCancelPolicy(true);
        }
        return executorService;
    }

    private static ThreadFactory defaultThreadFactory(final String taskName) {
        return new ThreadFactoryBuilder()
                .setNameFormat(taskName)
                .setDaemon(true)
                .setUncaughtExceptionHandler(defaultUncaughtExceptionHandler(taskName))
                .build();
    }

    private static Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler(final String taskName) {
        return (t, e) -> LOGGER.error("Uncaught exception in thread: " + e.getLocalizedMessage(), e);
    }

}
