package com.eia.assumedclaims.domain;

import java.util.UUID;

public record AcceptAdviceCommand(UUID acceptedBy, String note) {}
