package com.example.task.scheduler.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.task.scheduler.dto.Task;

public class PostgresTaskDao implements TaskDao {
    private static final String COMPLETED_STATUS = "COMPLETED";

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public PostgresTaskDao(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public static PostgresTaskDao fromEnvironment() {
        String jdbcUrl = getEnvOrDefault("TASK_DB_URL", "jdbc:postgresql://localhost:5432/task_scheduler");
        String username = getEnvOrDefault("TASK_DB_USER", "task_scheduler");
        String password = getEnvOrDefault("TASK_DB_PASSWORD", "task_scheduler");
        return new PostgresTaskDao(jdbcUrl, username, password);
    }

    @Override
    public List<Task> getUnfinishedTasks(LocalDateTime beforeTime) {
        String sql = """
                SELECT should_fail, name, description, scheduled_time, status
                FROM tasks
                WHERE scheduled_time <= ?
                  AND status <> ?
                ORDER BY scheduled_time, id
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(beforeTime));
            statement.setString(2, COMPLETED_STATUS);

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTasks(resultSet);
            }
        } catch (SQLException e) {
            throw new TaskDaoException("Failed to get unfinished tasks before " + beforeTime, e);
        }
    }

    @Override
    public boolean saveTask(Task task) {
        String sql = """
                INSERT INTO tasks (should_fail, name, description, scheduled_time, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, task.shouldFail());
            statement.setString(2, task.name());
            statement.setString(3, task.description());
            statement.setTimestamp(4, Timestamp.valueOf(task.scheduledTime()));
            statement.setString(5, task.status());

            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new TaskDaoException("Failed to save task " + task.name(), e);
        }
    }

    @Override
    public List<Task> getAndUpdateUnfinishedTasks(LocalDateTime beforeTime, String newStatus) {
        String sql = """
                WITH claimed_tasks AS (
                    SELECT id
                    FROM tasks
                    WHERE scheduled_time <= ?
                      AND status <> ?
                      AND status <> ?
                    ORDER BY scheduled_time, id
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE tasks
                SET status = ?,
                    updated_at = CURRENT_TIMESTAMP
                FROM claimed_tasks
                WHERE tasks.id = claimed_tasks.id
                RETURNING tasks.should_fail, tasks.name, tasks.description, tasks.scheduled_time, tasks.status
                """;

        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(beforeTime));
            statement.setString(2, COMPLETED_STATUS);
            statement.setString(3, newStatus);
            statement.setString(4, newStatus);

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTasks(resultSet);
            }
        } catch (SQLException e) {
            throw new TaskDaoException("Failed to claim unfinished tasks before " + beforeTime, e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private static List<Task> mapTasks(ResultSet resultSet) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        while (resultSet.next()) {
            tasks.add(new Task(
                    resultSet.getBoolean("should_fail"),
                    resultSet.getString("name"),
                    resultSet.getString("description"),
                    resultSet.getTimestamp("scheduled_time").toLocalDateTime(),
                    resultSet.getString("status")));
        }
        return tasks;
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
