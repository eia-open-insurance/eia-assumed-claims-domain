package com.eia.assumedclaims.domain;

import java.time.LocalDate;

/**
 * Command to open a diary task. The expert assigns the task number, the initial {@code OPEN}
 * status, and the starting version.
 */
public record CreateTaskCommand(
        String title,
        String description,
        LocalDate dueDate,
        String priority,
        String assignedTo
) {}
