package com.example;

import com.example.task.scheduler.TaskScheduler;
import com.example.task.scheduler.TaskSchedulerImpl;
import com.example.task.scheduler.dao.PostgresTaskDao;
import com.example.task.scheduler.dao.TaskDao;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        
        simulateWithThreeInstances();
    }

    private static void simulateWithThreeInstances() {
        TaskDao taskDao = PostgresTaskDao.fromEnvironment();
        TaskScheduler taskScheduler1 = new TaskSchedulerImpl(taskDao);
        TaskScheduler taskScheduler2 = new TaskSchedulerImpl(taskDao);
        TaskScheduler taskScheduler3 = new TaskSchedulerImpl(taskDao);
    }
}
