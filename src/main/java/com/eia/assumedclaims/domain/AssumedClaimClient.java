package com.eia.assumedclaims.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Internal client for the assumed claims expert. Every call carries the ASSUMED book scope and the
 * tenant context so the expert can enforce the wall and tenancy independently.
 */
@Component
public class AssumedClaimClient {
    public static final String BOOK = "ASSUMED";
    public static final String BOOK_HEADER = "X-EIA-Book";
    public static final String TENANT_HEADER = "X-EIA-Tenant";
    public static final String USER_HEADER = "X-EIA-User";

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public AssumedClaimClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${eia.assumed-claims.expert-url:http://localhost:8089}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<AssumedClaimDto> findAll(UUID tenantId) {
        return getList(tenantId, "/internal/v1/assumed-claims?tenantId=" + tenantId,
                new ParameterizedTypeReference<>() {}, null);
    }

    public AssumedClaimDto findById(UUID tenantId, UUID id) {
        return get(tenantId, "/internal/v1/assumed-claims/" + id + "?tenantId=" + tenantId,
                AssumedClaimDto.class, id);
    }

    public AssumedClaimDto create(UUID tenantId, CreateAssumedClaimCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims", new CreateBody(
                        tenantId,
                        command.contractId(),
                        command.cedentClaimNumber(),
                        command.lossDate(),
                        command.reportDate(),
                        command.reserveAmount(),
                        command.reserveCurrency()),
                AssumedClaimDto.class, null);
    }

    public AssumedClaimDto changeStatus(UUID tenantId, UUID id, AssumedClaimStatus targetStatus, int expectedVersion) {
        try {
            return client.post()
                    .uri("/internal/v1/assumed-claims/" + id + "/status")
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .body(new StatusBody(tenantId, targetStatus, expectedVersion))
                    .retrieve()
                    .body(AssumedClaimDto.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                ExpertProblem problem = problem(exception);
                if (problem != null && "EIA-RNS-7006".equals(problem.code())) {
                    throw ApiException.versionConflict(expectedVersion);
                }
            }
            throw translate(exception, id);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    public AssumedClaimDto close(UUID tenantId, UUID id) {
        return post(tenantId, "/internal/v1/assumed-claims/" + id + "/close", null, AssumedClaimDto.class, id);
    }

    public FinancialSummaryDto financials(UUID tenantId, UUID id) {
        return get(tenantId, "/internal/v1/assumed-claims/" + id + "/financials", FinancialSummaryDto.class, id);
    }

    public List<ClaimAuditEntryDto> audit(UUID tenantId, UUID id) {
        return getList(tenantId, "/internal/v1/assumed-claims/" + id + "/audit",
                new ParameterizedTypeReference<>() {}, id);
    }

    public List<LossAdviceDto> findAdvices(UUID tenantId, UUID claimId) {
        return getList(tenantId, "/internal/v1/assumed-claims/" + claimId + "/advices",
                new ParameterizedTypeReference<>() {}, claimId);
    }

    public LossAdviceDto recordAdvice(UUID tenantId, UUID claimId, RecordAdviceCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims/" + claimId + "/advices",
                command, LossAdviceDto.class, claimId);
    }

    public LossAdviceDto acceptAdvice(UUID tenantId, UUID claimId, UUID adviceId, AcceptAdviceCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims/" + claimId + "/advices/" + adviceId + "/accept",
                command, LossAdviceDto.class, claimId);
    }

    public List<BordereauBatchDto> findBordereaux(UUID tenantId, UUID claimId) {
        return getList(tenantId, "/internal/v1/assumed-claims/" + claimId + "/bordereaux",
                new ParameterizedTypeReference<>() {}, claimId);
    }

    public BordereauIngestResultDto ingestBordereau(UUID tenantId, UUID claimId, IngestBordereauCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims/" + claimId + "/bordereaux",
                command, BordereauIngestResultDto.class, claimId);
    }

    public List<ReserveHistoryDto> findReserves(UUID tenantId, UUID claimId) {
        return getList(tenantId, "/internal/v1/assumed-claims/" + claimId + "/reserves",
                new ParameterizedTypeReference<>() {}, claimId);
    }

    public ReserveHistoryDto changeReserve(UUID tenantId, UUID claimId, ReserveChangeCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims/" + claimId + "/reserves",
                command, ReserveHistoryDto.class, claimId);
    }

    public List<CashCallDto> findCashCalls(UUID tenantId, UUID claimId) {
        return getList(tenantId, "/internal/v1/assumed-claims/" + claimId + "/cash-calls",
                new ParameterizedTypeReference<>() {}, claimId);
    }

    public CashCallDto requestCashCall(UUID tenantId, UUID claimId, RequestCashCallCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims/" + claimId + "/cash-calls",
                command, CashCallDto.class, claimId);
    }

    public List<RecoveryDto> findRecoveries(UUID tenantId, UUID claimId) {
        return getList(tenantId, "/internal/v1/assumed-claims/" + claimId + "/recoveries",
                new ParameterizedTypeReference<>() {}, claimId);
    }

    public RecoveryDto recordRecovery(UUID tenantId, UUID claimId, RecordRecoveryCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims/" + claimId + "/recoveries",
                command, RecoveryDto.class, claimId);
    }

    public CommutationDto findCommutation(UUID tenantId, UUID claimId) {
        return get(tenantId, "/internal/v1/assumed-claims/" + claimId + "/commutation",
                CommutationDto.class, null);
    }

    public CommutationDto commute(UUID tenantId, UUID claimId, CommuteClaimCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims/" + claimId + "/commute",
                command, CommutationDto.class, claimId);
    }

    public List<DisputeDto> findDisputes(UUID tenantId, UUID claimId) {
        return getList(tenantId, "/internal/v1/assumed-claims/" + claimId + "/disputes",
                new ParameterizedTypeReference<>() {}, claimId);
    }

    public DisputeDto raiseDispute(UUID tenantId, UUID claimId, RaiseDisputeCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims/" + claimId + "/disputes",
                command, DisputeDto.class, claimId);
    }

    public DisputeDto resolveDispute(UUID tenantId, UUID claimId, UUID disputeId, ResolveDisputeCommand command) {
        return post(tenantId, "/internal/v1/assumed-claims/" + claimId + "/disputes/" + disputeId + "/resolve",
                command, DisputeDto.class, claimId);
    }

    // ------------------------------------------------------------------ tasks
    public List<ClaimTaskDto> findTasks(UUID tenantId, UUID claimId) {
        return getChildList(tenantId, "/internal/v1/assumed-claims/" + claimId + "/tasks",
                new ParameterizedTypeReference<>() {}, claimId);
    }

    public ClaimTaskDto addTask(UUID tenantId, UUID claimId, CreateTaskCommand command) {
        return postChild(tenantId, "/internal/v1/assumed-claims/" + claimId + "/tasks",
                command, ClaimTaskDto.class, claimId);
    }

    public ClaimTaskDto changeTaskStatus(UUID tenantId, UUID claimId, UUID taskId,
                                         TaskStatusChangeCommand command) {
        return patchChild(tenantId, "/internal/v1/assumed-claims/" + claimId + "/tasks/" + taskId,
                command, ClaimTaskDto.class, claimId);
    }

    // ------------------------------------------------------------------ notes
    public List<ClaimNoteDto> findNotes(UUID tenantId, UUID claimId) {
        return getChildList(tenantId, "/internal/v1/assumed-claims/" + claimId + "/notes",
                new ParameterizedTypeReference<>() {}, claimId);
    }

    public ClaimNoteDto addNote(UUID tenantId, UUID claimId, CreateNoteCommand command) {
        return postChild(tenantId, "/internal/v1/assumed-claims/" + claimId + "/notes",
                command, ClaimNoteDto.class, claimId);
    }

    private <T> T get(UUID tenantId, String path, Class<T> type, UUID notFoundId) {
        try {
            return client.get()
                    .uri(path)
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException exception) {
            throw translate(exception, notFoundId);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    private <T> List<T> getList(UUID tenantId, String path, ParameterizedTypeReference<List<T>> type, UUID notFoundId) {
        try {
            List<T> values = client.get()
                    .uri(path)
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .retrieve()
                    .body(type);
            return values == null ? List.of() : values;
        } catch (RestClientResponseException exception) {
            throw translate(exception, notFoundId);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    private <T> T post(UUID tenantId, String path, Object body, Class<T> type, UUID notFoundId) {
        try {
            RestClient.RequestBodySpec request = client.post()
                    .uri(path)
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString());
            if (body != null) {
                request.body(body);
            }
            return request.retrieve().body(type);
        } catch (RestClientResponseException exception) {
            throw translate(exception, notFoundId);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    private <T> List<T> getChildList(UUID tenantId, String path, ParameterizedTypeReference<List<T>> type,
                                     UUID claimId) {
        try {
            List<T> values = client.get()
                    .uri(path)
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .retrieve()
                    .body(type);
            return values == null ? List.of() : values;
        } catch (RestClientResponseException exception) {
            throw translateChild(exception, claimId);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    private <T> T postChild(UUID tenantId, String path, Object body, Class<T> type, UUID claimId) {
        try {
            return client.post()
                    .uri(path)
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .body(body)
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException exception) {
            throw translateChild(exception, claimId);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    private <T> T patchChild(UUID tenantId, String path, Object body, Class<T> type, UUID claimId) {
        try {
            return client.patch()
                    .uri(path)
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .body(body)
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException exception) {
            throw translateChild(exception, claimId);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    /**
     * Child-resource errors keep the expert's problem detail and status (400/404/409), unlike the
     * claim-level translation that always collapses a 404 to the domain's own not-found message.
     * A bare 404 still maps to the domain's not-found problem.
     */
    private ApiException translateChild(RestClientResponseException exception, UUID claimId) {
        int status = exception.getStatusCode().value();
        if (exception.getStatusCode().is5xxServerError()) {
            return ApiException.expertUnavailable();
        }
        ExpertProblem problem = problem(exception);
        if (problem != null && problem.code() != null && !problem.code().isBlank()) {
            return ApiException.fromExpert(status, problem.code(), problem.category(), problem.title(),
                    problem.detail(), problem.errors());
        }
        if (status == 404) {
            if (problem != null && problem.detail() != null && !problem.detail().isBlank()) {
                return ApiException.fromExpert(404, "EIA-RNS-7001",
                        problem.category() == null ? "NOT_FOUND" : problem.category(),
                        problem.title() == null ? "Assumed claim not found" : problem.title(),
                        problem.detail(), problem.errors());
            }
            return ApiException.assumedClaimNotFound(claimId);
        }
        if (status == 400) {
            return ApiException.validation(problem != null && problem.detail() != null
                            ? problem.detail()
                            : "The assumed claims expert rejected the request as invalid.",
                    problem == null ? List.of() : problem.errors());
        }
        if (status == 409) {
            return ApiException.fromExpert(409, "EIA-RNS-7006", "CONFLICT", "Version conflict",
                    problem != null && problem.detail() != null
                            ? problem.detail()
                            : "The task was modified concurrently; reload and retry.",
                    problem == null ? List.of() : problem.errors());
        }
        if (problem != null) {
            return ApiException.fromExpert(status, "EIA-RNS-7004", problem.category(), problem.title(),
                    problem.detail(), problem.errors());
        }
        return ApiException.expertRejected(status);
    }

    private ApiException translate(RestClientResponseException exception, UUID notFoundId) {
        int status = exception.getStatusCode().value();
        if (status == 404 && notFoundId != null) {
            return ApiException.assumedClaimNotFound(notFoundId);
        }
        if (exception.getStatusCode().is5xxServerError()) {
            return ApiException.expertUnavailable();
        }
        ExpertProblem problem = problem(exception);
        if (problem != null && problem.code() != null && !problem.code().isBlank()) {
            return ApiException.fromExpert(status, problem.code(), problem.category(), problem.title(),
                    problem.detail(), problem.errors());
        }
        if (status == 400) {
            return ApiException.validation("The assumed claims expert rejected the request as invalid.", List.of());
        }
        return ApiException.expertRejected(status);
    }

    private ExpertProblem problem(RestClientResponseException exception) {
        try {
            String body = exception.getResponseBodyAsString();
            if (body == null || body.isBlank()) {
                return null;
            }
            return objectMapper.readValue(body, ExpertProblem.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    record CreateBody(
            UUID tenantId,
            UUID contractId,
            String cedentClaimNumber,
            LocalDate lossDate,
            LocalDate reportDate,
            BigDecimal reserveAmount,
            String reserveCurrency
    ) {}

    record StatusBody(UUID tenantId, AssumedClaimStatus targetStatus, int expectedVersion) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ExpertProblem(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String code,
            String category,
            boolean retryable,
            List<FieldIssue> errors
    ) {}
}
