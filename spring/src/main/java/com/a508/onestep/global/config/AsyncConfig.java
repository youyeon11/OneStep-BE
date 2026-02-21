package com.a508.onestep.global.config;

import com.a508.onestep.global.logging.utils.LogUtils;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
Executor Configuration
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private ExecutorService taskExecutorService;
    private ExecutorService batchExecutorService;

    /*
     * 일반 비동기 Executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        taskExecutorService = Executors.newVirtualThreadPerTaskExecutor();

        TaskExecutorAdapter adapter = new TaskExecutorAdapter(taskExecutorService);
        adapter.setTaskDecorator(task -> () -> {
            Thread.currentThread().setName("task-" + Thread.currentThread().threadId());
            task.run();
        });
        LogUtils.info("Task Executor 초기화 완료 - Virtual Thread");

        return adapter;
    }

    /*
     * 스케줄러 TaskScheduler
     */
    @Bean(name = "taskScheduler")
    public TaskScheduler taskScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setVirtualThreads(true);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setTaskTerminationTimeout(30);

        scheduler.setErrorHandler(t ->
                LogUtils.error("Scheduler error: {}", t.getMessage(), t)
        );

        LogUtils.info("TaskScheduler 초기화 완료 - Virtual Thread");

        return scheduler;
    }

    /*
     * Batch 작업용 Executor
     */
    @Bean(name = "batchExecutor")
    public Executor batchExecutor() {
        batchExecutorService = Executors.newVirtualThreadPerTaskExecutor();

        TaskExecutorAdapter adapter = new TaskExecutorAdapter(batchExecutorService);
        adapter.setTaskDecorator(task -> () -> {
            Thread.currentThread().setName("batch-" + Thread.currentThread().threadId());
            task.run();
        });

        LogUtils.info("Batch Executor 초기화 완료 - Virtual Thread");

        return adapter;
    }

    /*
     * Graceful shutdown
     */
    @PreDestroy
    public void shutdown() {
        LogUtils.info("Executors shutting down...");
        shutdownExecutor(taskExecutorService, "taskExecutor");
        shutdownExecutor(batchExecutorService, "batchExecutor");
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                LogUtils.warn("{} did not terminate in time, forcing shutdown", name);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
