package org.open4goods.b2bapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.open4goods.b2bapi.dto.playground.PlaygroundRequest;
import org.open4goods.b2bapi.dto.playground.PlaygroundResponse;
import org.open4goods.b2bapi.dto.product.B2bPriceDto;
import org.open4goods.b2bapi.dto.product.B2bResponse;
import org.open4goods.b2bapi.exception.ErrorCode;
import org.open4goods.b2bapi.exception.InsufficientCreditsException;
import org.open4goods.b2bapi.exception.InvalidGtinException;
import org.open4goods.b2bapi.exception.ResourceNotFoundException;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.service.ApiKeyPrincipal;
import org.open4goods.b2bapi.service.B2bProductService;
import org.open4goods.b2bapi.service.DashboardPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for B2B playground proxy operations.
 */
@RestController
@RequestMapping("/api/v1/customer/playground")
@ConditionalOnBean(name = "entityManagerFactory")
public class CustomerPlaygroundController {

    private final ApiKeyRepository apiKeyRepository;
    private final B2bProductService b2bProductService;

    public CustomerPlaygroundController(
            final ApiKeyRepository apiKeyRepository,
            final B2bProductService b2bProductService) {
        this.apiKeyRepository = apiKeyRepository;
        this.b2bProductService = b2bProductService;
    }

    /**
     * Proxies product price requests on behalf of dashboard playground sessions without revealing API keys.
     */
    @PostMapping("/products/price")
    @PreAuthorize("@organizationRbacService.canUsePlaygroundOrReadUsage(authentication)")
    public PlaygroundResponse proxyProductPrice(
            @Valid @RequestBody final PlaygroundRequest body,
            final Authentication authentication,
            final HttpServletRequest servletRequest,
            final HttpServletResponse servletResponse) {

        final DashboardPrincipal principal = principal(authentication);
        final UUID orgId = principal.organizationId();

        // 1. Resolve key and check ownership
        final ApiKey apiKey = apiKeyRepository.findByIdAndOrganizationId(body.apiKeyId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found."));

        final String maskedKey = "Bearer pdapi_" + apiKey.getKeyPrefix() + "...masked";
        final String requestPath = "/api/v1/products/" + body.gtin() + "/price"
                + (body.language() != null ? "?language=" + body.language() : "?language=en");

        final PlaygroundResponse.PlaygroundRequestDetails requestDetails = new PlaygroundResponse.PlaygroundRequestDetails(
                "GET",
                requestPath,
                Map.of("Authorization", maskedKey)
        );

        // 2. If key is not ACTIVE, mock 401 response
        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            final ProblemDetail problem = toProblem(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Missing, invalid, or revoked API key.",
                    requestPath,
                    "unavailable"
            );
            return new PlaygroundResponse(
                    requestDetails,
                    new PlaygroundResponse.PlaygroundResponseDetails(401, Map.of(), problem),
                    new PlaygroundResponse.PlaygroundMeteringDetails(false, 0L, 0L, "invalid-credentials")
            );
        }

        // 3. Intercept response headers to capture metering metrics set by service
        final HeaderInterceptingResponse responseWrapper = new HeaderInterceptingResponse(servletResponse);

        int status = 200;
        Object responseBody = null;
        boolean billable = false;
        long creditsConsumed = 0;
        long creditsRemaining = 0;
        String reason = null;

        try {
            final ApiKeyPrincipal apiKeyPrincipal = new ApiKeyPrincipal(orgId, apiKey.getId());
            final B2bResponse<B2bPriceDto> b2bResponse = b2bProductService.getProductPrice(
                    body.gtin(),
                    body.language() != null ? body.language() : "en",
                    apiKeyPrincipal,
                    servletRequest,
                    responseWrapper
            );
            responseBody = b2bResponse;
            billable = b2bResponse.meta().billable();
            creditsConsumed = b2bResponse.meta().creditsConsumed();
            creditsRemaining = b2bResponse.meta().creditsRemaining();
            reason = billable ? "fresh-offer" : "no-fresh-offer";

        } catch (final InvalidGtinException ex) {
            status = 400;
            reason = "invalid-gtin";
            final String requestId = responseWrapper.getInterceptedHeaders().getOrDefault("X-Request-Id", "unavailable");
            responseBody = toProblem(ErrorCode.INVALID_GTIN, ex.getMessage(), requestPath, requestId);
        } catch (final InsufficientCreditsException ex) {
            status = 402;
            reason = "insufficient-credits";
            final String requestId = responseWrapper.getInterceptedHeaders().getOrDefault("X-Request-Id", "unavailable");
            responseBody = toProblem(ErrorCode.INSUFFICIENT_CREDITS, ex.getMessage(), requestPath, requestId);
        } catch (final ResourceNotFoundException ex) {
            status = 404;
            reason = "not-found";
            final String requestId = responseWrapper.getInterceptedHeaders().getOrDefault("X-Request-Id", "unavailable");
            responseBody = toProblem(ErrorCode.PRODUCT_NOT_FOUND, ex.getMessage(), requestPath, requestId);
        } catch (final Exception ex) {
            status = 500;
            reason = "internal-error";
            final String requestId = responseWrapper.getInterceptedHeaders().getOrDefault("X-Request-Id", "unavailable");
            responseBody = toProblem(ErrorCode.INTERNAL_ERROR, "Unexpected server error.", requestPath, requestId);
        }

        // Retrieve remaining credits from wrapper header if not set by success path
        if (creditsRemaining == 0) {
            final String remainingStr = responseWrapper.getInterceptedHeaders().get("X-Credits-Remaining");
            if (remainingStr != null) {
                try {
                    creditsRemaining = Long.parseLong(remainingStr);
                } catch (final NumberFormatException ignored) {
                }
            }
        }

        final PlaygroundResponse.PlaygroundResponseDetails responseDetails = new PlaygroundResponse.PlaygroundResponseDetails(
                status,
                responseWrapper.getInterceptedHeaders(),
                responseBody
        );

        final PlaygroundResponse.PlaygroundMeteringDetails meteringDetails = new PlaygroundResponse.PlaygroundMeteringDetails(
                billable,
                creditsConsumed,
                creditsRemaining,
                reason
        );

        return new PlaygroundResponse(requestDetails, responseDetails, meteringDetails);
    }

    private ProblemDetail toProblem(final ErrorCode errorCode, final String detail, final String requestUri, final String requestId) {
        final HttpStatus status = errorCode.status();
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://product-data-api.com/problems/" + errorCode.slug()));
        problem.setTitle(errorCode.title());
        problem.setInstance(URI.create(requestUri));
        if (requestId != null && !"unavailable".equals(requestId)) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }

    private DashboardPrincipal principal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DashboardPrincipal principal)) {
            throw new AccessDeniedException("Dashboard authentication is required.");
        }
        return principal;
    }

    private static class HeaderInterceptingResponse extends HttpServletResponseWrapper {
        private final Map<String, String> headers = new HashMap<>();

        public HeaderInterceptingResponse(final HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setHeader(final String name, final String value) {
            headers.put(name, value);
        }

        @Override
        public void addHeader(final String name, final String value) {
            headers.put(name, value);
        }

        public Map<String, String> getInterceptedHeaders() {
            return headers;
        }
    }
}
