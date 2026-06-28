package com.example.task.scheduler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.task.scheduler.dao.TaskDao;
import com.example.task.scheduler.dto.Task;

public class TaskSchedulerImpl implements TaskScheduler {
    private final TaskDao taskDao;
    private final ExecutorService threadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public TaskSchedulerImpl(TaskDao taskDao) {
        // Initialize the TaskSchedulerImpl with the provided TaskDao
        this.taskDao = taskDao;
    }

    @Override
    public boolean scheduleTask(Task task) {
        return taskDao.saveTask(task);
    }

    @Override
    public void startPoller() {
        System.out.println("Starting task poller..." + Thread.currentThread().getName());
        while (true) {
            // Poll for unfinished tasks and update their status
            List<Task> unfinishedTasks = taskDao.getAndUpdateUnfinishedTasks(java.time.LocalDateTime.now(),
                    "IN_PROGRESS");
            for (Task task : unfinishedTasks) {
                runTaskInThread(task);
            }
            try {
                // Sleep for a specified interval before polling again
                Thread.sleep(2000); // Poll every 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // Exit the loop if interrupted
            }
        }
    }

    private void runTaskInThread(Task task) {
        threadExecutor.submit(() -> {
            // Process the task in a separate thread
            System.out.println(Thread.currentThread().getName() + " - Processing task: " + task.name() + " - "
                    + task.description());
            // Simulate task processing time
            try {
                // doing processing of task
                if (task.shouldFail()) {
                    throw new RuntimeException("Task failed: " + task.name());
                }
                // Simulate processing time
                Thread.sleep(2000); // Simulate processing time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                scheduleTask(new Task(false, task.name(), task.description(),
                        task.scheduledTime(), "TO-DO"));
            }
            // After processing, you can update the task status to "COMPLETED" or any other
            // status as needed
            System.out.println(Thread.currentThread().getName() + " - Completed task: " + task.name() + " - "
                    + task.description());
        });
    }

}
