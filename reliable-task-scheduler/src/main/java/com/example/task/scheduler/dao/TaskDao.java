package com.example.task.scheduler.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.example.task.scheduler.dto.Task;

public interface TaskDao {
    List<Task> getUnfinishedTasks(LocalDateTime beforeTime);

    boolean saveTask(Task task);

    List<Task> getAndUpdateUnfinishedTasks(LocalDateTime beforeTime, String newStatus);
}
