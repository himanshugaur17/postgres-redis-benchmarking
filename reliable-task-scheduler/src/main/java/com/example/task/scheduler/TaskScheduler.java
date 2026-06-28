package com.example.task.scheduler;

import com.example.task.scheduler.dto.Task;

public interface TaskScheduler {
    boolean scheduleTask(Task task);

    void startPoller();
}
