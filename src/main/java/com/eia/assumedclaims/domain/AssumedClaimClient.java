package com.eia.assumedclaims.domain;

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

    private final RestClient client;

    public AssumedClaimClient(
            RestClient.Builder builder,
            @Value("${eia.assumed-claims.expert-url:http://localhost:8089}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public List<AssumedClaimDto> findAll(UUID tenantId) {
        try {
            List<AssumedClaimDto> claims = client.get()
                    .uri(uri -> uri.path("/internal/v1/assumed-claims")
                            .queryParam("tenantId", tenantId)
                            .build())
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return claims == null ? List.of() : claims;
        } catch (RestClientResponseException exception) {
            throw translate(exception, null);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    public AssumedClaimDto findById(UUID tenantId, UUID id) {
        try {
            return client.get()
                    .uri(uri -> uri.path("/internal/v1/assumed-claims/{id}")
                            .queryParam("tenantId", tenantId)
                            .build(id))
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .retrieve()
                    .body(AssumedClaimDto.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception, id);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    public AssumedClaimDto create(UUID tenantId, CreateAssumedClaimCommand command) {
        try {
            return client.post()
                    .uri("/internal/v1/assumed-claims")
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .body(new CreateBody(
                            tenantId,
                            command.contractId(),
                            command.cedentClaimNumber(),
                            command.lossDate(),
                            command.reportDate(),
                            command.reserveAmount(),
                            command.reserveCurrency()))
                    .retrieve()
                    .body(AssumedClaimDto.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception, null);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    public AssumedClaimDto changeStatus(UUID tenantId, UUID id, AssumedClaimStatus targetStatus, int expectedVersion) {
        try {
            return client.post()
                    .uri(uri -> uri.path("/internal/v1/assumed-claims/{id}/status").build(id))
                    .header(BOOK_HEADER, BOOK)
                    .header(TENANT_HEADER, tenantId.toString())
                    .body(new StatusBody(tenantId, targetStatus, expectedVersion))
                    .retrieve()
                    .body(AssumedClaimDto.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw ApiException.versionConflict(expectedVersion);
            }
            throw translate(exception, id);
        } catch (ResourceAccessException exception) {
            throw ApiException.expertUnavailable();
        }
    }

    private static ApiException translate(RestClientResponseException exception, UUID notFoundId) {
        int status = exception.getStatusCode().value();
        if (status == 404 && notFoundId != null) {
            return ApiException.assumedClaimNotFound(notFoundId);
        }
        if (status == 400) {
            return ApiException.validation("The assumed claims expert rejected the request as invalid.", List.of());
        }
        if (exception.getStatusCode().is5xxServerError()) {
            return ApiException.expertUnavailable();
        }
        return ApiException.expertRejected(status);
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
}
