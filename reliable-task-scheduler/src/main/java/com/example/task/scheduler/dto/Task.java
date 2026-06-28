package com.example.task.scheduler.dto;

import java.time.LocalDateTime;

public record Task(boolean shouldFail, String name, String description, LocalDateTime scheduledTime, String status) {

}
