package com.eia.assumedclaims.domain;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Public orchestration for assumed-claim tasks and notes.
 *
 * <p>The domain validates the closed sets the caller can fix before any expert call: task
 * statuses, task priorities, and note types. The expert still owns the transactional record, the
 * task version, numbering, and the ASSUMED wall; every rejection it returns is preserved by
 * {@link AssumedClaimClient} as a problem+json {@link ApiException}.</p>
 */
@Service
public class ClaimTaskNoteService {
    static final Set<String> TASK_STATUSES = Set.of("OPEN", "COMPLETE", "CANCELLED");
    static final Set<String> TASK_PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    static final Set<String> NOTE_TYPES = Set.of(
            "GENERAL", "DIARY", "COVERAGE", "RESERVE", "LITIGATION", "SETTLEMENT");

    private final AssumedClaimClient client;

    public ClaimTaskNoteService(AssumedClaimClient client) {
        this.client = client;
    }

    // ------------------------------------------------------------------ tasks
    public List<ClaimTaskDto> tasks(UUID tenantId, UUID claimId) {
        return client.findTasks(tenantId, claimId);
    }

    public CommandResult<ClaimTaskDto> addTask(UUID tenantId, UUID claimId,
                                               CreateTaskCommand command, String idempotencyKey) {
        String key = IdempotencyKeys.normalize(idempotencyKey);
        List<FieldIssue> issues = new ArrayList<>();
        requireText(command.title(), "title", issues);
        if (command.dueDate() == null) {
            issues.add(new FieldIssue("dueDate", "REQUIRED", "dueDate is required"));
        }
        String priority = requireText(command.priority(), "priority", issues);
        if (priority != null && !TASK_PRIORITIES.contains(priority.toUpperCase(Locale.ROOT))) {
            issues.add(new FieldIssue("priority", "UNKNOWN_PRIORITY",
                    "Unknown priority: " + command.priority()));
        }
        throwIfInvalid(issues, "The task command is invalid.");
        return client.addTask(tenantId, claimId, command, key);
    }

    public ClaimTaskDto changeTaskStatus(UUID tenantId, UUID claimId, UUID taskId,
                                         TaskStatusChangeCommand command) {
        List<FieldIssue> issues = new ArrayList<>();
        String status = requireText(command.status(), "status", issues);
        if (status != null && !TASK_STATUSES.contains(status.toUpperCase(Locale.ROOT))) {
            issues.add(new FieldIssue("status", "UNKNOWN_STATUS",
                    "Unknown task status: " + command.status()));
        }
        if (command.version() == null) {
            issues.add(new FieldIssue("version", "REQUIRED", "version is required"));
        }
        throwIfInvalid(issues, "The task status command is invalid.");
        return client.changeTaskStatus(tenantId, claimId, taskId, command);
    }

    // ------------------------------------------------------------------ notes
    public List<ClaimNoteDto> notes(UUID tenantId, UUID claimId) {
        return client.findNotes(tenantId, claimId);
    }

    public CommandResult<ClaimNoteDto> addNote(UUID tenantId, UUID claimId,
                                               CreateNoteCommand command, String idempotencyKey) {
        String key = IdempotencyKeys.normalize(idempotencyKey);
        List<FieldIssue> issues = new ArrayList<>();
        String noteType = requireText(command.noteType(), "noteType", issues);
        if (noteType != null && !NOTE_TYPES.contains(noteType.toUpperCase(Locale.ROOT))) {
            issues.add(new FieldIssue("noteType", "UNKNOWN_NOTE_TYPE",
                    "Unknown noteType: " + command.noteType()));
        }
        requireText(command.body(), "body", issues);
        requireText(command.author(), "author", issues);
        throwIfInvalid(issues, "The note command is invalid.");
        return client.addNote(tenantId, claimId, command, key);
    }

    // ------------------------------------------------------------------ helpers
    private static String requireText(String value, String field, List<FieldIssue> issues) {
        if (value == null || value.isBlank()) {
            issues.add(new FieldIssue(field, "REQUIRED", field + " is required"));
            return null;
        }
        return value.trim();
    }

    private static void throwIfInvalid(List<FieldIssue> issues, String summary) {
        if (issues.isEmpty()) {
            return;
        }
        // Mirror the Direct Claims detail for a single bad field; summarize multiple fields.
        String detail = issues.size() == 1 ? issues.getFirst().message() : summary;
        throw ApiException.validation(detail, issues);
    }
}
