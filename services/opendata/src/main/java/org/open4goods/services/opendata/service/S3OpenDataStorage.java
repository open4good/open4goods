package org.open4goods.services.opendata.service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.services.opendata.config.OpenDataConfig.Publication;
import org.open4goods.services.opendata.model.ObjectHeaders;
import org.open4goods.services.opendata.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * {@link OpenDataStorage} on the S3 API of Cloudflare R2 (GOU-2289): path-style requests, region {@code auto},
 * no chunked encoding and checksums only when required, as R2 documents for the AWS SDK for Java v2. Uploads are
 * multipart with parts of {@code part-size}, read from the file at {@code upload-bandwidth} at most; copies above
 * {@code copy-part-size} are multipart copies (a single copy is limited to 5 GB).
 */
public class S3OpenDataStorage implements OpenDataStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3OpenDataStorage.class);

    /** User metadata key of the sha256 ({@code x-amz-meta-sha256}), as written by the R2 upload workflow. */
    static final String SHA256_METADATA = "sha256";

    private static final int MAX_PARTS = 10_000;
    private static final long MIN_PART_BYTES = 5L * 1024 * 1024;

    private final S3Client client;
    private final String bucket;
    private final long partBytes;
    private final long copyPartBytes;
    private final long uploadBytesPerSecond;

    /**
     * Builds the client from the publication settings.
     *
     * @throws IllegalStateException when the endpoint or the credentials are missing
     */
    public S3OpenDataStorage(Publication publication) {
        this(buildClient(publication), publication);
    }

    S3OpenDataStorage(S3Client client, Publication publication) {
        this.client = client;
        this.bucket = publication.getBucket();
        this.partBytes = Math.max(MIN_PART_BYTES, publication.getPartSize().toBytes());
        this.copyPartBytes = Math.max(MIN_PART_BYTES, publication.getCopyPartSize().toBytes());
        this.uploadBytesPerSecond = publication.getUploadBandwidth() == null ? 0 : publication.getUploadBandwidth().toBytes();
    }

    private static S3Client buildClient(Publication publication) {
        String endpoint = publication.resolvedEndpoint();
        if (StringUtils.isBlank(endpoint)) {
            throw new IllegalStateException("open-data-config.publication: no endpoint and no account id (R2_ACCOUNT_ID)");
        }
        if (StringUtils.isAnyBlank(publication.getAccessKeyId(), publication.getSecretAccessKey())) {
            throw new IllegalStateException(
                    "open-data-config.publication: access key id or secret access key missing (R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY)");
        }
        if (StringUtils.isBlank(publication.getBucket())) {
            throw new IllegalStateException("open-data-config.publication.bucket is empty");
        }
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        publication.getAccessKeyId().trim(), publication.getSecretAccessKey().trim())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClientBuilder(ApacheHttpClient.builder())
                .build();
    }

    @Override
    public List<StoredObject> list(String prefix) throws IOException {
        try {
            ListObjectsV2Request.Builder request = ListObjectsV2Request.builder().bucket(bucket);
            if (StringUtils.isNotEmpty(prefix)) {
                request.prefix(prefix);
            }
            List<StoredObject> objects = new ArrayList<>();
            for (S3Object object : client.listObjectsV2Paginator(request.build()).contents()) {
                objects.add(new StoredObject(object.key(), object.size() == null ? 0 : object.size(), null));
            }
            return objects;
        } catch (SdkException e) {
            throw failure("list " + bucket + "/" + StringUtils.defaultString(prefix), e);
        }
    }

    @Override
    public Optional<StoredObject> head(String key) throws IOException {
        try {
            HeadObjectResponse response = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            Map<String, String> metadata = response.metadata();
            return Optional.of(new StoredObject(key, response.contentLength() == null ? 0 : response.contentLength(),
                    metadata == null ? null : metadata.get(SHA256_METADATA)));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw failure("head " + key, e);
        } catch (SdkException e) {
            throw failure("head " + key, e);
        }
    }

    @Override
    public void upload(Path file, String key, ObjectHeaders headers) throws IOException {
        long size = Files.size(file);
        long parts = Math.max(1, (size + partBytes - 1) / partBytes);
        if (parts > MAX_PARTS) {
            throw new IOException("Upload of " + file + " needs " + parts + " parts of " + partBytes
                    + " bytes, more than " + MAX_PARTS + ": raise open-data-config.publication.part-size");
        }
        RateLimiter limiter = uploadBytesPerSecond > 0 ? RateLimiter.create(uploadBytesPerSecond) : null;
        String uploadId = null;
        try {
            uploadId = client.createMultipartUpload(createRequest(key, headers)).uploadId();
            List<CompletedPart> completed = new ArrayList<>();
            for (int part = 1; part <= parts; part++) {
                long offset = (part - 1) * partBytes;
                long length = Math.min(partBytes, size - offset);
                String eTag = client.uploadPart(UploadPartRequest.builder()
                                .bucket(bucket).key(key).uploadId(uploadId).partNumber(part).contentLength(length).build(),
                        RequestBody.fromContentProvider(() -> openPart(file, offset, length, limiter), length,
                                "application/octet-stream"))
                        .eTag();
                completed.add(CompletedPart.builder().partNumber(part).eTag(eTag).build());
                LOGGER.info("Open data publication: {} part {}/{} sent", key, part, parts);
            }
            complete(key, uploadId, completed);
        } catch (RuntimeException e) {
            abort(key, uploadId);
            throw failure("upload " + key, e);
        }
    }

    @Override
    public void put(byte[] content, String key, ObjectHeaders headers) throws IOException {
        try {
            PutObjectRequest.Builder request = PutObjectRequest.builder().bucket(bucket).key(key)
                    .contentLength((long) content.length)
                    .contentType(headers.contentType())
                    .contentDisposition(headers.contentDisposition())
                    .cacheControl(headers.cacheControl());
            if (headers.sha256() != null) {
                request.metadata(Map.of(SHA256_METADATA, headers.sha256()));
            }
            client.putObject(request.build(), RequestBody.fromBytes(content));
        } catch (SdkException e) {
            throw failure("put " + key, e);
        }
    }

    @Override
    public InputStream read(String key) throws IOException {
        try {
            return client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            throw failure("read " + key, e);
        }
    }

    @Override
    public void copy(String sourceKey, String targetKey, long bytes, ObjectHeaders headers) throws IOException {
        if (bytes <= copyPartBytes) {
            try {
                CopyObjectRequest.Builder request = CopyObjectRequest.builder()
                        .sourceBucket(bucket).sourceKey(sourceKey)
                        .destinationBucket(bucket).destinationKey(targetKey)
                        .metadataDirective(MetadataDirective.REPLACE)
                        .contentType(headers.contentType())
                        .contentDisposition(headers.contentDisposition())
                        .cacheControl(headers.cacheControl());
                if (headers.sha256() != null) {
                    request.metadata(Map.of(SHA256_METADATA, headers.sha256()));
                }
                client.copyObject(request.build());
                return;
            } catch (SdkException e) {
                throw failure("copy " + sourceKey + " to " + targetKey, e);
            }
        }
        long parts = (bytes + copyPartBytes - 1) / copyPartBytes;
        String uploadId = null;
        try {
            uploadId = client.createMultipartUpload(createRequest(targetKey, headers)).uploadId();
            List<CompletedPart> completed = new ArrayList<>();
            for (int part = 1; part <= parts; part++) {
                long first = (part - 1) * copyPartBytes;
                long last = Math.min(bytes, first + copyPartBytes) - 1;
                String eTag = client.uploadPartCopy(UploadPartCopyRequest.builder()
                                .sourceBucket(bucket).sourceKey(sourceKey)
                                .destinationBucket(bucket).destinationKey(targetKey)
                                .uploadId(uploadId).partNumber(part)
                                .copySourceRange("bytes=" + first + "-" + last)
                                .build())
                        .copyPartResult().eTag();
                completed.add(CompletedPart.builder().partNumber(part).eTag(eTag).build());
            }
            complete(targetKey, uploadId, completed);
        } catch (RuntimeException e) {
            abort(targetKey, uploadId);
            throw failure("copy " + sourceKey + " to " + targetKey, e);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            throw failure("delete " + key, e);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private CreateMultipartUploadRequest createRequest(String key, ObjectHeaders headers) {
        CreateMultipartUploadRequest.Builder request = CreateMultipartUploadRequest.builder().bucket(bucket).key(key)
                .contentType(headers.contentType())
                .contentDisposition(headers.contentDisposition())
                .cacheControl(headers.cacheControl());
        if (headers.sha256() != null) {
            request.metadata(Map.of(SHA256_METADATA, headers.sha256()));
        }
        return request.build();
    }

    private void complete(String key, String uploadId, List<CompletedPart> parts) {
        client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucket).key(key).uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                .build());
    }

    /** Aborts a multipart upload so that its parts are not kept (and billed); {@code key} is unchanged. */
    private void abort(String key, String uploadId) {
        if (uploadId == null) {
            return;
        }
        try {
            client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket).key(key).uploadId(uploadId).build());
        } catch (SdkException e) {
            LOGGER.error("Unable to abort the multipart upload {} of {}: its parts may stay in the bucket", uploadId,
                    key, e);
        }
    }

    private static InputStream openPart(Path file, long offset, long length, RateLimiter limiter) {
        try {
            InputStream in = Files.newInputStream(file);
            in.skipNBytes(offset);
            return new PartInputStream(in, length, limiter);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file + " at " + offset, e);
        }
    }

    private static IOException failure(String action, Exception e) {
        return new IOException("R2 " + action + " failed: " + e.getMessage(), e);
    }

    /**
     * {@code length} bytes of a file, read at the bandwidth of {@code limiter} (none when null).
     */
    static final class PartInputStream extends FilterInputStream {

        private final RateLimiter limiter;
        private long remaining;

        PartInputStream(InputStream in, long length, RateLimiter limiter) {
            super(in);
            this.remaining = length;
            this.limiter = limiter;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            acquire(1);
            int value = super.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int wanted = (int) Math.min(len, remaining);
            if (wanted == 0) {
                return 0;
            }
            acquire(wanted);
            int read = super.read(b, off, wanted);
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(Math.min(n, remaining));
            remaining -= skipped;
            return skipped;
        }

        @Override
        public int available() throws IOException {
            return (int) Math.min(super.available(), remaining);
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        private void acquire(int permits) {
            if (limiter != null) {
                limiter.acquire(permits);
            }
        }
    }
}
