package org.open4goods.b2bapi.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.config.BillingCatalogProperties;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeDimensions;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeMetadata;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeOptions;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderMeta;
import org.springframework.scheduling.annotation.Scheduled;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderRequest;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderResponse;
import org.open4goods.b2bapi.exception.InsufficientCreditsException;
import org.open4goods.b2bapi.exception.InvalidBarcodeException;
import org.open4goods.b2bapi.exception.RedisUnavailableException;
import org.open4goods.b2bapi.model.BarcodeAsset;
import org.open4goods.b2bapi.repository.BarcodeAssetRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Service for rendering barcodes, managing credit transactions, and caching assets.
 */
@Service
public class B2bBarcodeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(B2bBarcodeService.class);
    public static final String FACET_BARCODE_RENDER = "barcode.render";

    private final B2bApiProperties properties;
    private final BillingCatalogProperties billingCatalogProperties;
    private final RedisMeteringService redisMeteringService;
    private final CreditLedgerService creditLedgerService;
    private final UsageStreamService usageStreamService;
    private final BarcodeAssetRepository barcodeAssetRepository;
    private final CreditBucketRepository creditBucketRepository;
    private final Clock clock;

    @Autowired
    public B2bBarcodeService(
            final B2bApiProperties properties,
            final BillingCatalogProperties billingCatalogProperties,
            final ObjectProvider<RedisMeteringService> redisMeteringService,
            final ObjectProvider<CreditLedgerService> creditLedgerService,
            final ObjectProvider<UsageStreamService> usageStreamService,
            final ObjectProvider<BarcodeAssetRepository> barcodeAssetRepository,
            final ObjectProvider<CreditBucketRepository> creditBucketRepository) {
        this(
                properties,
                billingCatalogProperties,
                redisMeteringService.getIfAvailable(),
                creditLedgerService.getIfAvailable(),
                usageStreamService.getIfAvailable(),
                barcodeAssetRepository.getIfAvailable(),
                creditBucketRepository.getIfAvailable(),
                Clock.systemUTC());
    }

    B2bBarcodeService(
            final B2bApiProperties properties,
            final BillingCatalogProperties billingCatalogProperties,
            final RedisMeteringService redisMeteringService,
            final CreditLedgerService creditLedgerService,
            final UsageStreamService usageStreamService,
            final BarcodeAssetRepository barcodeAssetRepository,
            final CreditBucketRepository creditBucketRepository,
            final Clock clock) {
        this.properties = properties;
        this.billingCatalogProperties = billingCatalogProperties;
        this.redisMeteringService = redisMeteringService;
        this.creditLedgerService = creditLedgerService;
        this.usageStreamService = usageStreamService;
        this.barcodeAssetRepository = barcodeAssetRepository;
        this.creditBucketRepository = creditBucketRepository;
        this.clock = clock;
    }

    /**
     * Renders a single barcode, reserves and settles credit, and writes the output to the asset cache.
     *
     * @param req the barcode rendering request parameters
     * @param principal the authenticated API key principal
     * @param request servlet request object
     * @param response servlet response object
     * @return the barcode rendering response containing metadata and signed URL
     */
    @Transactional
    public B2bBarcodeRenderResponse renderBarcode(
            final B2bBarcodeRenderRequest req,
            final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        ensureServices();

        final long startTime = clock.millis();
        final UUID orgId = principal.organizationId();
        final UUID keyId = principal.apiKeyId();

        redisMeteringService.checkRateLimit(keyId);

        final int maxCost = 1;

        boolean reserved = false;
        long currentRedisBalance = 0;
        RedisBalanceResult reserveResult = redisMeteringService.reserveCredits(orgId, maxCost);
        if (reserveResult.status() == RedisBalanceStatus.BALANCE_NOT_LOADED) {
            final long dbBalance = creditBucketRepository.sumLiveCredits(orgId);
            redisMeteringService.reconcileBalance(orgId, dbBalance);
            reserveResult = redisMeteringService.reserveCredits(orgId, maxCost);
        }

        if (reserveResult.status() == RedisBalanceStatus.RESERVED) {
            reserved = true;
            currentRedisBalance = reserveResult.balance();
        } else if (reserveResult.status() == RedisBalanceStatus.INSUFFICIENT_CREDITS) {
            final long duration = clock.millis() - startTime;
            final String requestId = resolveOrCreateRequestId(request);
            long remaining = creditBucketRepository.sumLiveCredits(orgId);
            setHeadersAndAttributes(request, response, requestId, 0L, remaining, duration);

            usageStreamService.emit(new UsageStreamEvent(
                    orgId,
                    keyId,
                    FACET_BARCODE_RENDER,
                    req.data(),
                    requestId,
                    402,
                    false,
                    0L,
                    "insufficient-credits",
                    (int) duration,
                    Instant.now(clock)));
            throw new InsufficientCreditsException("Insufficient credits to perform request.");
        } else {
            throw new RedisUnavailableException("Redis is unavailable.");
        }

        final String requestId = resolveOrCreateRequestId(request);
        boolean billable = false;
        long actualCost = 0;
        String noPayReason = null;
        int httpStatus = 200;
        long remainingBalance = currentRedisBalance;

        byte[] barcodeBytes = null;
        String token = null;
        Instant expiresAt = clock.instant().plus(java.time.Duration.ofDays(30));

        try {
            barcodeBytes = generateBarcodeBytes(req);
            token = generateSignedToken(req);

            String contentType = req.format().equalsIgnoreCase("svg") ? "image/svg+xml" : "image/png";
            BarcodeAsset asset = new BarcodeAsset(token, barcodeBytes, contentType, expiresAt);
            barcodeAssetRepository.save(asset);

            billable = true;
            actualCost = 1;
        } catch (final InvalidBarcodeException e) {
            httpStatus = 400;
            noPayReason = "invalid-input";
            throw e;
        } catch (final RuntimeException e) {
            httpStatus = 500;
            noPayReason = "render-failure";
            throw e;
        } catch (final Throwable t) {
            httpStatus = 500;
            noPayReason = "render-failure";
            throw new RuntimeException(t);
        } finally {
            if (reserved) {
                if (actualCost == 0) {
                    final RedisBalanceResult refundResult = redisMeteringService.refundCredits(orgId, maxCost);
                    if (refundResult.status() == RedisBalanceStatus.UPDATED) {
                        remainingBalance = refundResult.balance();
                    } else {
                        remainingBalance = creditBucketRepository.sumLiveCredits(orgId);
                    }
                } else {
                    try {
                        final CreditSettlementResult settlementResult = creditLedgerService.settleDebit(
                                orgId,
                                requestId,
                                FACET_BARCODE_RENDER,
                                req.data(),
                                actualCost);
                        remainingBalance = settlementResult.durableBalance();

                        if (settlementResult.idempotentReplay()) {
                            actualCost = 0;
                        }

                        redisMeteringService.reconcileBalance(orgId, remainingBalance);
                    } catch (final InsufficientCreditsException ex) {
                        redisMeteringService.refundCredits(orgId, maxCost);
                        httpStatus = 402;
                        billable = false;
                        actualCost = 0;
                        noPayReason = "insufficient-credits";
                        remainingBalance = creditBucketRepository.sumLiveCredits(orgId);
                    }
                }
            }

            final long duration = clock.millis() - startTime;
            setHeadersAndAttributes(request, response, requestId, actualCost, remainingBalance, duration);

            usageStreamService.emit(new UsageStreamEvent(
                    orgId,
                    keyId,
                    FACET_BARCODE_RENDER,
                    req.data(),
                    requestId,
                    httpStatus,
                    billable,
                    actualCost,
                    noPayReason,
                    (int) duration,
                    Instant.now(clock)));
        }

        String publicAssetUrl = properties.getPublicBaseUrl() + "/api/v1/barcodes/assets/" + token;
        B2bBarcodeDimensions dims = new B2bBarcodeDimensions(req.width(), req.height(), req.options().dpi());
        B2bBarcodeRenderMeta meta = new B2bBarcodeRenderMeta(requestId, billable, actualCost);
        String inputHash = computeInputHash(req);

        return new B2bBarcodeRenderResponse(
                meta,
                publicAssetUrl,
                expiresAt,
                dims,
                req.format().equalsIgnoreCase("svg") ? "image/svg+xml" : "image/png",
                Collections.emptyList(),
                inputHash);
    }

    /**
     * Renders a batch of barcodes, packaging them into a ZIP archive, and billing credits accordingly.
     *
     * @param requests the list of rendering requests
     * @param principal the authenticated API key principal
     * @param request servlet request object
     * @param response servlet response object
     * @return the bytes of the generated ZIP archive
     */
    @Transactional
    public byte[] renderBarcodeZip(
            final List<B2bBarcodeRenderRequest> requests,
            final ApiKeyPrincipal principal,
            final HttpServletRequest request,
            final HttpServletResponse response) {
        ensureServices();

        final long startTime = clock.millis();
        final UUID orgId = principal.organizationId();
        final UUID keyId = principal.apiKeyId();

        redisMeteringService.checkRateLimit(keyId);

        if (requests == null || requests.isEmpty()) {
            throw new InvalidBarcodeException("Request list cannot be empty");
        }

        final int maxCost = requests.size();

        boolean reserved = false;
        long currentRedisBalance = 0;
        RedisBalanceResult reserveResult = redisMeteringService.reserveCredits(orgId, maxCost);
        if (reserveResult.status() == RedisBalanceStatus.BALANCE_NOT_LOADED) {
            final long dbBalance = creditBucketRepository.sumLiveCredits(orgId);
            redisMeteringService.reconcileBalance(orgId, dbBalance);
            reserveResult = redisMeteringService.reserveCredits(orgId, maxCost);
        }

        if (reserveResult.status() == RedisBalanceStatus.RESERVED) {
            reserved = true;
            currentRedisBalance = reserveResult.balance();
        } else if (reserveResult.status() == RedisBalanceStatus.INSUFFICIENT_CREDITS) {
            final long duration = clock.millis() - startTime;
            final String requestId = resolveOrCreateRequestId(request);
            long remaining = creditBucketRepository.sumLiveCredits(orgId);
            setHeadersAndAttributes(request, response, requestId, 0L, remaining, duration);

            usageStreamService.emit(new UsageStreamEvent(
                    orgId,
                    keyId,
                    FACET_BARCODE_RENDER,
                    "BATCH_ZIP",
                    requestId,
                    402,
                    false,
                    0L,
                    "insufficient-credits",
                    (int) duration,
                    Instant.now(clock)));
            throw new InsufficientCreditsException("Insufficient credits to perform request.");
        } else {
            throw new RedisUnavailableException("Redis is unavailable.");
        }

        final String requestId = resolveOrCreateRequestId(request);
        boolean billable = false;
        long actualCost = 0;
        String noPayReason = null;
        int httpStatus = 200;
        long remainingBalance = currentRedisBalance;

        ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();

        try {
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(zipBaos)) {
                for (int i = 0; i < requests.size(); i++) {
                    B2bBarcodeRenderRequest req = requests.get(i);
                    byte[] barcodeBytes = generateBarcodeBytes(req);
                    String token = generateSignedToken(req);
                    Instant expiresAt = clock.instant().plus(java.time.Duration.ofDays(30));

                    String contentType = req.format().equalsIgnoreCase("svg") ? "image/svg+xml" : "image/png";
                    BarcodeAsset asset = new BarcodeAsset(token, barcodeBytes, contentType, expiresAt);
                    barcodeAssetRepository.save(asset);

                    String ext = req.format().equalsIgnoreCase("svg") ? "svg" : "png";
                    String safeData = req.data().replaceAll("[^a-zA-Z0-9_-]", "");
                    String entryName = String.format("%d_%s_%s.%s", i, req.type().toLowerCase(), safeData, ext);

                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
                    zos.putNextEntry(entry);
                    zos.write(barcodeBytes);
                    zos.closeEntry();

                    actualCost++;
                }
            }
            billable = true;
        } catch (final InvalidBarcodeException e) {
            httpStatus = 400;
            noPayReason = "invalid-input";
            throw e;
        } catch (final RuntimeException e) {
            httpStatus = 500;
            noPayReason = "render-failure";
            throw e;
        } catch (final Throwable t) {
            httpStatus = 500;
            noPayReason = "render-failure";
            throw new RuntimeException(t);
        } finally {
            if (reserved) {
                if (actualCost == 0) {
                    final RedisBalanceResult refundResult = redisMeteringService.refundCredits(orgId, maxCost);
                    if (refundResult.status() == RedisBalanceStatus.UPDATED) {
                        remainingBalance = refundResult.balance();
                    } else {
                        remainingBalance = creditBucketRepository.sumLiveCredits(orgId);
                    }
                } else {
                    try {
                        final CreditSettlementResult settlementResult = creditLedgerService.settleDebit(
                                orgId,
                                requestId,
                                FACET_BARCODE_RENDER,
                                "BATCH_ZIP",
                                actualCost);
                        remainingBalance = settlementResult.durableBalance();

                        if (settlementResult.idempotentReplay()) {
                            actualCost = 0;
                        }

                        final long refund = maxCost - actualCost;
                        if (refund > 0) {
                            redisMeteringService.refundCredits(orgId, refund);
                        }
                        redisMeteringService.reconcileBalance(orgId, remainingBalance);
                    } catch (final InsufficientCreditsException ex) {
                        redisMeteringService.refundCredits(orgId, maxCost);
                        httpStatus = 402;
                        billable = false;
                        actualCost = 0;
                        noPayReason = "insufficient-credits";
                        remainingBalance = creditBucketRepository.sumLiveCredits(orgId);
                    }
                }
            }

            final long duration = clock.millis() - startTime;
            setHeadersAndAttributes(request, response, requestId, actualCost, remainingBalance, duration);

            usageStreamService.emit(new UsageStreamEvent(
                    orgId,
                    keyId,
                    FACET_BARCODE_RENDER,
                    "BATCH_ZIP",
                    requestId,
                    httpStatus,
                    billable,
                    actualCost,
                    noPayReason,
                    (int) duration,
                    Instant.now(clock)));
        }

        return zipBaos.toByteArray();
    }

    /**
     * Resolves a barcode asset by token, checking cache first, or rendering on-the-fly.
     *
     * @param token the signed asset token
     * @return the resolved barcode asset
     */
    public BarcodeAsset getBarcodeAsset(final String token) {
        if (barcodeAssetRepository == null) {
            throw new IllegalStateException("Barcode storage is not available.");
        }
        Optional<BarcodeAsset> cached = barcodeAssetRepository.findByToken(token);
        if (cached.isPresent()) {
            BarcodeAsset asset = cached.get();
            if (asset.getExpiresAt().isAfter(clock.instant())) {
                return asset;
            }
        }

        try {
            B2bBarcodeRenderRequest req = parseSignedToken(token);
            byte[] content = generateBarcodeBytes(req);
            String contentType = req.format().equalsIgnoreCase("svg") ? "image/svg+xml" : "image/png";

            Instant expiresAt = clock.instant().plus(java.time.Duration.ofDays(30));
            BarcodeAsset asset = new BarcodeAsset(token, content, contentType, expiresAt);
            barcodeAssetRepository.save(asset);
            return asset;
        } catch (Exception exception) {
            throw new org.open4goods.b2bapi.exception.ResourceNotFoundException("Asset not found or expired.");
        }
    }

    /**
     * Generates a signed HS256 JWT containing the barcode request parameters.
     *
     * @param req the rendering request parameters
     * @return the serialized JWT string
     */
    public String generateSignedToken(final B2bBarcodeRenderRequest req) {
        final Instant issuedAt = clock.instant();
        final Instant expiresAt = issuedAt.plus(java.time.Duration.ofDays(30));

        try {
            Map<String, String> metaMap = new LinkedHashMap<>();
            if (req.metadata() != null) {
                if (req.metadata().copyright() != null) metaMap.put("copyright", req.metadata().copyright());
                if (req.metadata().author() != null) metaMap.put("author", req.metadata().author());
                if (req.metadata().description() != null) metaMap.put("description", req.metadata().description());
            }

            final JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer("barcode-service")
                    .subject(req.type())
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .claim("data", req.data())
                    .claim("format", req.format())
                    .claim("width", req.width())
                    .claim("height", req.height())
                    .claim("foreground", req.foreground())
                    .claim("background", req.background())
                    .claim("rotation", req.rotation())
                    .claim("showText", req.showText())
                    .claim("quietZone", req.quietZone())
                    .claim("options_dpi", req.options().dpi())
                    .claim("options_moduleWidthMm", req.options().moduleWidthMm())
                    .claim("options_barHeightMm", req.options().barHeightMm())
                    .claim("options_fontSize", req.options().fontSize())
                    .claim("options_preset", req.options().preset())
                    .claim("metadata", metaMap)
                    .build();

            final SignedJWT signedJwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.HS256).type(com.nimbusds.jose.JOSEObjectType.JWT).build(),
                    claims);

            byte[] secret = properties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8);
            if (secret.length < 32) {
                byte[] padded = new byte[32];
                System.arraycopy(secret, 0, padded, 0, Math.min(secret.length, 32));
                secret = padded;
            }
            signedJwt.sign(new MACSigner(secret));
            return signedJwt.serialize();
        } catch (final JOSEException exception) {
            throw new IllegalStateException("Unable to sign barcode JWT", exception);
        }
    }

    /**
     * Parses and verifies a signed token, reconstructing the request parameters.
     *
     * @param token the signed token
     * @return the reconstructed B2bBarcodeRenderRequest
     */
    public B2bBarcodeRenderRequest parseSignedToken(final String token) {
        try {
            final SignedJWT signedJwt = SignedJWT.parse(token);
            byte[] secret = properties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8);
            if (secret.length < 32) {
                byte[] padded = new byte[32];
                System.arraycopy(secret, 0, padded, 0, Math.min(secret.length, 32));
                secret = padded;
            }
            final boolean validSignature = signedJwt.verify(new MACVerifier(secret));
            if (!validSignature) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            final JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            final Instant expiresAt = claims.getExpirationTime().toInstant();
            if (!expiresAt.isAfter(clock.instant())) {
                throw new IllegalArgumentException("Expired JWT");
            }

            String type = claims.getSubject();
            String data = claims.getStringClaim("data");
            String format = claims.getStringClaim("format");
            int width = getIntClaim(claims, "width");
            int height = getIntClaim(claims, "height");
            String foreground = claims.getStringClaim("foreground");
            String background = claims.getStringClaim("background");
            int rotation = getIntClaim(claims, "rotation");
            boolean showText = claims.getBooleanClaim("showText");
            boolean quietZone = claims.getBooleanClaim("quietZone");

            int dpi = getIntClaim(claims, "options_dpi");
            double moduleWidthMm = getDoubleClaim(claims, "options_moduleWidthMm");
            double barHeightMm = getDoubleClaim(claims, "options_barHeightMm");
            double fontSize = getDoubleClaim(claims, "options_fontSize");
            String preset = claims.getStringClaim("options_preset");
            B2bBarcodeOptions options = new B2bBarcodeOptions(dpi, moduleWidthMm, barHeightMm, fontSize, preset);

            Map<?, ?> metaMap = (Map<?, ?>) claims.getClaim("metadata");
            String copyright = metaMap != null ? (String) metaMap.get("copyright") : null;
            String author = metaMap != null ? (String) metaMap.get("author") : null;
            String description = metaMap != null ? (String) metaMap.get("description") : null;
            B2bBarcodeMetadata metadata = new B2bBarcodeMetadata(copyright, author, description);

            return new B2bBarcodeRenderRequest(
                    type, data, format, width, height, foreground, background, rotation, showText, quietZone, options, metadata);
        } catch (Exception exception) {
            throw new InvalidBarcodeException("Invalid token: " + exception.getMessage(), exception);
        }
    }

    private int getIntClaim(final JWTClaimsSet claims, final String name) {
        Object val = claims.getClaim(name);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        throw new IllegalArgumentException("Missing or invalid integer claim: " + name);
    }

    private double getDoubleClaim(final JWTClaimsSet claims, final String name) {
        Object val = claims.getClaim(name);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        throw new IllegalArgumentException("Missing or invalid double claim: " + name);
    }

    /**
     * Helper to compute a SHA-256 hash over standardized request parameters.
     *
     * @param req the barcode rendering request
     * @return the calculated input hash string
     */
    public static String computeInputHash(final B2bBarcodeRenderRequest req) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(req.type().toLowerCase()).append("|")
              .append(req.data()).append("|")
              .append(req.format()).append("|")
              .append(req.width()).append("|")
              .append(req.height()).append("|")
              .append(req.foreground()).append("|")
              .append(req.background()).append("|")
              .append(req.rotation()).append("|")
              .append(req.showText()).append("|")
              .append(req.quietZone()).append("|");
            if (req.options() != null) {
                sb.append(req.options().dpi()).append("|")
                  .append(req.options().moduleWidthMm()).append("|")
                  .append(req.options().barHeightMm()).append("|")
                  .append(req.options().fontSize()).append("|")
                  .append(req.options().preset()).append("|");
            }
            if (req.metadata() != null) {
                sb.append(req.metadata().copyright()).append("|")
                  .append(req.metadata().author()).append("|")
                  .append(req.metadata().description()).append("|");
            }
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder("sha256_");
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 digest error", e);
        }
    }

    /**
     * Generates barcode output bytes based on format (PNG or SVG).
     *
     * @param req the request parameters
     * @return the barcode bytes
     */
    public byte[] generateBarcodeBytes(final B2bBarcodeRenderRequest req) {
        if (req.format().equalsIgnoreCase("svg")) {
            String svg = generateBarcodeSvg(req);
            return svg.getBytes(StandardCharsets.UTF_8);
        } else {
            BufferedImage image = generateBarcodeImage(req);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", baos);
                byte[] rawPng = baos.toByteArray();
                if (req.metadata() != null) {
                    Map<String, String> metaMap = new LinkedHashMap<>();
                    if (req.metadata().copyright() != null) metaMap.put("Copyright", req.metadata().copyright());
                    if (req.metadata().author() != null) metaMap.put("Author", req.metadata().author());
                    if (req.metadata().description() != null) metaMap.put("Description", req.metadata().description());
                    return injectPngMetadata(rawPng, metaMap);
                }
                return rawPng;
            } catch (Exception e) {
                throw new InvalidBarcodeException("Failed to generate PNG image: " + e.getMessage(), e);
            }
        }
    }

    private BufferedImage generateBarcodeImage(final B2bBarcodeRenderRequest req) {
        String type = req.type().toLowerCase();
        if (is2dSymbology(type)) {
            return generateZxingImage(req);
        } else {
            return generateBarcode4jImage(req);
        }
    }

    private String generateBarcodeSvg(final B2bBarcodeRenderRequest req) {
        String type = req.type().toLowerCase();
        if (is2dSymbology(type)) {
            return generateZxingSvg(req);
        } else {
            return generateBarcode4jSvg(req);
        }
    }

    private boolean is2dSymbology(final String type) {
        return "qr".equals(type) || "aztec".equals(type) || "datamatrix".equals(type) || "pdf417".equals(type);
    }

    private BufferedImage generateBarcode4jImage(final B2bBarcodeRenderRequest req) {
        org.krysalis.barcode4j.impl.AbstractBarcodeBean bean = createBarcode4jBean(req);
        int dpi = req.options().dpi() != null ? req.options().dpi() : 300;

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider canvas =
                    new org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider(
                            os, "image/png", dpi, BufferedImage.TYPE_BYTE_BINARY, false, req.rotation());
            bean.generateBarcode(canvas, req.data());
            canvas.finish();

            BufferedImage rawImage = ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
            Color fg = Color.decode(req.foreground());
            Color bg = Color.decode(req.background());
            BufferedImage colored = recolor(rawImage, fg, bg);

            return resizeImage(colored, req.width(), req.height());
        } catch (Exception e) {
            throw new InvalidBarcodeException("Failed to generate 1D barcode: " + e.getMessage(), e);
        }
    }

    private String generateBarcode4jSvg(final B2bBarcodeRenderRequest req) {
        org.krysalis.barcode4j.impl.AbstractBarcodeBean bean = createBarcode4jBean(req);
        try {
            org.krysalis.barcode4j.output.svg.SVGCanvasProvider canvas =
                    new org.krysalis.barcode4j.output.svg.SVGCanvasProvider(false, req.rotation());
            bean.generateBarcode(canvas, req.data());
            org.w3c.dom.DocumentFragment frag = canvas.getDOMFragment();

            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(frag), new StreamResult(writer));
            String rawSvg = writer.toString();

            return recolorAndInjectSvg(rawSvg, req.foreground(), req.background(), req.metadata());
        } catch (Exception e) {
            throw new InvalidBarcodeException("Failed to generate 1D barcode SVG: " + e.getMessage(), e);
        }
    }

    private org.krysalis.barcode4j.impl.AbstractBarcodeBean createBarcode4jBean(final B2bBarcodeRenderRequest req) {
        String type = req.type().toLowerCase();
        org.krysalis.barcode4j.impl.AbstractBarcodeBean bean;
        switch (type) {
            case "ean8" -> bean = new org.krysalis.barcode4j.impl.upcean.EAN8Bean();
            case "ean13" -> bean = new org.krysalis.barcode4j.impl.upcean.EAN13Bean();
            case "upca" -> bean = new org.krysalis.barcode4j.impl.upcean.UPCABean();
            case "upce" -> bean = new org.krysalis.barcode4j.impl.upcean.UPCEBean();
            case "code128" -> bean = new org.krysalis.barcode4j.impl.code128.Code128Bean();
            case "gs128", "gs1-128" -> bean = new org.krysalis.barcode4j.impl.code128.EAN128Bean();
            case "itf14" -> bean = new org.krysalis.barcode4j.impl.int2of5.ITF14Bean();
            default -> throw new InvalidBarcodeException("Unsupported 1D barcode type: " + type);
        }

        if (req.options().moduleWidthMm() != null) {
            bean.setModuleWidth(req.options().moduleWidthMm());
        }
        if (req.options().barHeightMm() != null) {
            bean.setHeight(req.options().barHeightMm());
        }
        if (req.options().fontSize() != null) {
            bean.setFontSize(req.options().fontSize());
        }
        if (Boolean.FALSE.equals(req.showText())) {
            bean.setMsgPosition(org.krysalis.barcode4j.HumanReadablePlacement.HRP_NONE);
        } else {
            bean.setMsgPosition(org.krysalis.barcode4j.HumanReadablePlacement.HRP_BOTTOM);
        }
        bean.doQuietZone(Boolean.TRUE.equals(req.quietZone()));
        return bean;
    }

    private BufferedImage generateZxingImage(final B2bBarcodeRenderRequest req) {
        BarcodeFormat zxingFormat = getZxingFormat(req.type());
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            if (Boolean.FALSE.equals(req.quietZone())) {
                hints.put(EncodeHintType.MARGIN, 0);
            } else {
                hints.put(EncodeHintType.MARGIN, 2);
            }

            MultiFormatWriter writer = new MultiFormatWriter();
            int matrixW = req.width();
            int matrixH = req.height();
            if (req.rotation() == 90 || req.rotation() == 270) {
                matrixW = req.height();
                matrixH = req.width();
            }

            BitMatrix bitMatrix = writer.encode(req.data(), zxingFormat, matrixW, matrixH, hints);

            int onColor = Color.decode(req.foreground()).getRGB();
            int offColor = Color.decode(req.background()).getRGB();
            MatrixToImageConfig config = new MatrixToImageConfig(onColor, offColor);

            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix, config);

            if (req.rotation() != 0) {
                image = rotateImage(image, req.rotation());
            }

            return image;
        } catch (Exception e) {
            throw new InvalidBarcodeException("Failed to generate 2D barcode: " + e.getMessage(), e);
        }
    }

    private String generateZxingSvg(final B2bBarcodeRenderRequest req) {
        BarcodeFormat zxingFormat = getZxingFormat(req.type());
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            if (Boolean.FALSE.equals(req.quietZone())) {
                hints.put(EncodeHintType.MARGIN, 0);
            } else {
                hints.put(EncodeHintType.MARGIN, 2);
            }

            MultiFormatWriter writer = new MultiFormatWriter();
            int matrixW = req.width();
            int matrixH = req.height();
            if (req.rotation() == 90 || req.rotation() == 270) {
                matrixW = req.height();
                matrixH = req.width();
            }

            BitMatrix bitMatrix = writer.encode(req.data(), zxingFormat, matrixW, matrixH, hints);

            if (req.rotation() == 90) {
                bitMatrix = rotateBitMatrix90(bitMatrix);
            } else if (req.rotation() == 180) {
                bitMatrix = rotateBitMatrix180(bitMatrix);
            } else if (req.rotation() == 270) {
                bitMatrix = rotateBitMatrix270(bitMatrix);
            }

            String rawSvg = bitMatrixToSvg(bitMatrix, req.foreground(), req.background());

            return recolorAndInjectSvg(rawSvg, req.foreground(), req.background(), req.metadata());
        } catch (Exception e) {
            throw new InvalidBarcodeException("Failed to generate 2D barcode SVG: " + e.getMessage(), e);
        }
    }

    private BarcodeFormat getZxingFormat(final String type) {
        switch (type.toLowerCase()) {
            case "qr" -> { return BarcodeFormat.QR_CODE; }
            case "aztec" -> { return BarcodeFormat.AZTEC; }
            case "datamatrix" -> { return BarcodeFormat.DATA_MATRIX; }
            case "pdf417" -> { return BarcodeFormat.PDF_417; }
            default -> throw new InvalidBarcodeException("Unsupported 2D barcode type: " + type);
        }
    }

    private BitMatrix rotateBitMatrix90(final BitMatrix matrix) {
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        BitMatrix rotated = new BitMatrix(h, w);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (matrix.get(x, y)) {
                    rotated.set(h - y - 1, x);
                }
            }
        }
        return rotated;
    }

    private BitMatrix rotateBitMatrix180(final BitMatrix matrix) {
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        BitMatrix rotated = new BitMatrix(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (matrix.get(x, y)) {
                    rotated.set(w - x - 1, h - y - 1);
                }
            }
        }
        return rotated;
    }

    private BitMatrix rotateBitMatrix270(final BitMatrix matrix) {
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        BitMatrix rotated = new BitMatrix(h, w);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (matrix.get(x, y)) {
                    rotated.set(y, w - x - 1);
                }
            }
        }
        return rotated;
    }

    private static String bitMatrixToSvg(final BitMatrix matrix, final String foreground, final String background) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ");
        sb.append("width=\"").append(width).append("\" ");
        sb.append("height=\"").append(height).append("\" ");
        sb.append("viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">\n");
        sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"").append(background).append("\"/>\n");

        sb.append("  <path fill=\"").append(foreground).append("\" d=\"");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    sb.append("M").append(x).append(",").append(y).append("h1v1h-1z ");
                }
            }
        }
        sb.append("\"/>\n");
        sb.append("</svg>");
        return sb.toString();
    }

    private static BufferedImage recolor(final BufferedImage src, final Color foreground, final Color background) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int a = (rgb >> 24) & 0xFF;
                if (a < 10) {
                    dst.setRGB(x, y, background.getRGB());
                } else {
                    double lum = 0.299 * r + 0.587 * g + 0.114 * b;
                    if (lum < 128) {
                        dst.setRGB(x, y, foreground.getRGB());
                    } else {
                        dst.setRGB(x, y, background.getRGB());
                    }
                }
            }
        }
        return dst;
    }

    private static BufferedImage resizeImage(final BufferedImage originalImage, final int targetWidth, final int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }

    private static BufferedImage rotateImage(final BufferedImage img, final int angle) {
        if (angle == 0 || angle % 360 == 0) {
            return img;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        int newW = (angle == 90 || angle == 270) ? h : w;
        int newH = (angle == 90 || angle == 270) ? w : h;
        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rotated.createGraphics();
        g.translate((newW - w) / 2.0, (newH - h) / 2.0);
        g.rotate(Math.toRadians(angle), w / 2.0, h / 2.0);
        g.drawRenderedImage(img, null);
        g.dispose();
        return rotated;
    }

    private String recolorAndInjectSvg(
            final String svgXml,
            final String foreground,
            final String background,
            final B2bBarcodeMetadata metadata) throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(svgXml)));
        Element svgElement = doc.getDocumentElement();

        Element bgRect = doc.createElement("rect");
        bgRect.setAttribute("width", "100%");
        bgRect.setAttribute("height", "100%");
        bgRect.setAttribute("fill", background);
        if (svgElement.hasChildNodes()) {
            svgElement.insertBefore(bgRect, svgElement.getFirstChild());
        } else {
            svgElement.appendChild(bgRect);
        }

        recolorSvgNode(svgElement, foreground);

        if (metadata != null) {
            Element metadataElement = doc.createElement("metadata");
            if (svgElement.hasChildNodes()) {
                svgElement.insertBefore(metadataElement, svgElement.getFirstChild());
            } else {
                svgElement.appendChild(metadataElement);
            }

            addMetaChild(doc, metadataElement, "Copyright", metadata.copyright());
            addMetaChild(doc, metadataElement, "Author", metadata.author());
            addMetaChild(doc, metadataElement, "Description", metadata.description());
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private void recolorSvgNode(final Node node, final String foreground) {
        if (node instanceof Element) {
            Element el = (Element) node;
            String tagName = el.getTagName();
            if ("rect".equals(tagName) || "path".equals(tagName) || "text".equals(tagName)) {
                if (!"100%".equals(el.getAttribute("width"))) {
                    el.setAttribute("fill", foreground);
                    if (el.hasAttribute("stroke")) {
                        el.setAttribute("stroke", foreground);
                    }
                }
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            recolorSvgNode(children.item(i), foreground);
        }
    }

    private void addMetaChild(final Document doc, final Element parent, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            String sanitizedVal = value.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
            if (sanitizedVal.length() > 256) {
                sanitizedVal = sanitizedVal.substring(0, 256);
            }
            Element child = doc.createElement(key.replaceAll("[^a-zA-Z0-9]", ""));
            child.appendChild(doc.createTextNode(sanitizedVal));
            parent.appendChild(child);
        }
    }

    private static byte[] injectPngMetadata(final byte[] pngData, final Map<String, String> metadata) throws Exception {
        if (metadata == null || metadata.isEmpty()) {
            return pngData;
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngData));

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            return pngData;
        }
        ImageWriter writer = writers.next();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);

        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromRenderedImage(image);
        IIOMetadata iioMetadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

        String nativeFormatName = iioMetadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) iioMetadata.getAsTree(nativeFormatName);

        IIOMetadataNode textNode = null;
        NodeList childNodes = root.getElementsByTagName("tEXt");
        if (childNodes.getLength() > 0) {
            textNode = (IIOMetadataNode) childNodes.item(0);
        } else {
            textNode = new IIOMetadataNode("tEXt");
            root.appendChild(textNode);
        }

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String val = entry.getValue();
            if (val != null && !val.isBlank()) {
                String sanitizedVal = val.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
                if (sanitizedVal.length() > 256) {
                    sanitizedVal = sanitizedVal.substring(0, 256);
                }
                IIOMetadataNode entryNode = new IIOMetadataNode("tEXtEntry");
                entryNode.setAttribute("keyword", entry.getKey());
                entryNode.setAttribute("value", sanitizedVal);
                textNode.appendChild(entryNode);
            }
        }

        iioMetadata.setFromTree(nativeFormatName, root);

        writer.write(null, new IIOImage(image, null, iioMetadata), writeParam);

        ios.flush();
        writer.dispose();
        return os.toByteArray();
    }

    private String resolveOrCreateRequestId(final HttpServletRequest request) {
        if (request != null) {
            final Object attributeId = request.getAttribute("X-Request-Id");
            if (attributeId instanceof String) {
                return (String) attributeId;
            }
            final String headerId = request.getHeader("X-Request-Id");
            if (headerId != null && !headerId.isBlank()) {
                return headerId;
            }
        }
        return properties.getRequestIds().getPrefix() + UUID.randomUUID().toString().replace("-", "");
    }

    private void setHeadersAndAttributes(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String requestId,
            final long creditsConsumed,
            final long creditsRemaining,
            final long responseTimeMs) {
        if (request != null) {
            request.setAttribute("X-Request-Id", requestId);
        }
        if (response != null) {
            response.setHeader("X-Request-Id", requestId);
            response.setHeader("X-Credits-Consumed", String.valueOf(creditsConsumed));
            response.setHeader("X-Credits-Remaining", String.valueOf(creditsRemaining));
            response.setHeader("X-Response-Time-Ms", String.valueOf(responseTimeMs));
        }
    }

    private void ensureServices() {
        if (redisMeteringService == null || creditLedgerService == null || usageStreamService == null ||
                barcodeAssetRepository == null || creditBucketRepository == null) {
            throw new IllegalStateException("Required services/repositories are not available.");
        }
    }

    /**
     * Periodically prunes expired barcode assets from the database.
     */
    @Scheduled(cron = "${b2b.barcode.cleanup.cron:0 0 * * * *}")
    @Transactional
    public void pruneExpiredAssets() {
        if (barcodeAssetRepository == null) {
            return;
        }
        LOGGER.info("Starting cleanup of expired barcode assets...");
        final int deleted = barcodeAssetRepository.deleteExpired(Instant.now(clock));
        LOGGER.info("Successfully deleted {} expired barcode assets.", deleted);
    }
}
