package com.eia.assumedclaims.domain;

/** Command to append an immutable claim note. Notes have no update or delete route. */
public record CreateNoteCommand(
        String noteType,
        String body,
        String author
) {}
