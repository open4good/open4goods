package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.open4goods.services.opendata.config.OpenDataConfig.Publication;
import org.open4goods.services.opendata.model.ObjectHeaders;
import org.springframework.util.unit.DataSize;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * Requests of {@link S3OpenDataStorage} on a mocked S3 client: parts, headers, abort on failure, multipart copy.
 */
class S3OpenDataStorageTest {

    private static final long MB = 1024 * 1024;
    private static final ObjectHeaders HEADERS = new ObjectHeaders("application/zip",
            "attachment; filename=\"gtin-open-data.zip\"", "public, max-age=300", "ab".repeat(32));

    @TempDir
    Path tempDir;

    private S3Client client;
    private Publication publication;
    private S3OpenDataStorage storage;

    @BeforeEach
    void setUp() {
        client = mock(S3Client.class);
        publication = new Publication();
        publication.setPartSize(DataSize.ofMegabytes(5));
        publication.setCopyPartSize(DataSize.ofMegabytes(5));
        publication.setUploadBandwidth(DataSize.ofBytes(0));
        storage = new S3OpenDataStorage(client, publication);
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload-1").build());
    }

    @Test
    void uploadSendsFixedSizePartsReadFromTheFileThenCompletes() throws Exception {
        byte[] content = content(11 * MB + 3);
        Path file = Files.write(tempDir.resolve("gtin.zip"), content);
        List<byte[]> sent = new ArrayList<>();
        when(client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenAnswer(invocation -> {
            RequestBody body = invocation.getArgument(1);
            try (InputStream in = body.contentStreamProvider().newStream()) {
                sent.add(in.readAllBytes());
            }
            return UploadPartResponse.builder().eTag("etag-" + sent.size()).build();
        });

        storage.upload(file, ".staging/gtin-open-data.zip", HEADERS);

        ArgumentCaptor<CreateMultipartUploadRequest> created = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(client).createMultipartUpload(created.capture());
        assertThat(created.getValue().key()).isEqualTo(".staging/gtin-open-data.zip");
        assertThat(created.getValue().contentType()).isEqualTo("application/zip");
        assertThat(created.getValue().contentDisposition()).isEqualTo("attachment; filename=\"gtin-open-data.zip\"");
        assertThat(created.getValue().cacheControl()).isEqualTo("public, max-age=300");
        assertThat(created.getValue().metadata()).containsExactly(java.util.Map.entry("sha256", "ab".repeat(32)));

        assertThat(sent).extracting(part -> part.length).containsExactly((int) (5 * MB), (int) (5 * MB), (int) MB + 3);
        byte[] joined = new byte[content.length];
        int offset = 0;
        for (byte[] part : sent) {
            System.arraycopy(part, 0, joined, offset, part.length);
            offset += part.length;
        }
        assertThat(joined).isEqualTo(content);

        ArgumentCaptor<CompleteMultipartUploadRequest> completed =
                ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(client).completeMultipartUpload(completed.capture());
        assertThat(completed.getValue().multipartUpload().parts())
                .extracting(part -> part.partNumber() + ":" + part.eTag())
                .containsExactly("1:etag-1", "2:etag-2", "3:etag-3");
        verify(client, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
    }

    @Test
    void failedPartAbortsTheUploadWithoutCompletingIt() throws Exception {
        Path file = Files.write(tempDir.resolve("gtin.zip"), content(11 * MB));
        when(client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenReturn(UploadPartResponse.builder().eTag("etag-1").build())
                .thenThrow(S3Exception.builder().message("connection reset").statusCode(500).build());

        assertThatThrownBy(() -> storage.upload(file, ".staging/gtin-open-data.zip", HEADERS))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("upload .staging/gtin-open-data.zip");

        ArgumentCaptor<AbortMultipartUploadRequest> aborted = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(client).abortMultipartUpload(aborted.capture());
        assertThat(aborted.getValue().uploadId()).isEqualTo("upload-1");
        verify(client, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }

    @Test
    void smallCopyReplacesTheHeadersInOneRequest() throws Exception {
        storage.copy(".staging/isbn-open-data.zip", "isbn-open-data.zip", 5 * MB, HEADERS);

        ArgumentCaptor<CopyObjectRequest> copy = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(client).copyObject(copy.capture());
        assertThat(copy.getValue().sourceKey()).isEqualTo(".staging/isbn-open-data.zip");
        assertThat(copy.getValue().destinationKey()).isEqualTo("isbn-open-data.zip");
        assertThat(copy.getValue().metadataDirective()).isEqualTo(MetadataDirective.REPLACE);
        assertThat(copy.getValue().contentType()).isEqualTo("application/zip");
        assertThat(copy.getValue().metadata()).containsEntry("sha256", "ab".repeat(32));
        verify(client, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    }

    @Test
    void largeCopyIsAMultipartCopyByRanges() throws Exception {
        when(client.uploadPartCopy(any(UploadPartCopyRequest.class))).thenAnswer(invocation ->
                UploadPartCopyResponse.builder().copyPartResult(CopyPartResult.builder()
                        .eTag("copy-" + ((UploadPartCopyRequest) invocation.getArgument(0)).partNumber()).build()).build());

        storage.copy(".staging/gtin-open-data.zip", "gtin-open-data.zip", 12 * MB, HEADERS);

        ArgumentCaptor<UploadPartCopyRequest> parts = ArgumentCaptor.forClass(UploadPartCopyRequest.class);
        verify(client, org.mockito.Mockito.times(3)).uploadPartCopy(parts.capture());
        assertThat(parts.getAllValues()).extracting(UploadPartCopyRequest::copySourceRange).containsExactly(
                "bytes=0-" + (5 * MB - 1), "bytes=" + 5 * MB + "-" + (10 * MB - 1), "bytes=" + 10 * MB + "-" + (12 * MB - 1));
        verify(client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        verify(client, never()).copyObject(any(CopyObjectRequest.class));
    }

    @Test
    void partStreamStopsAtItsLength() throws Exception {
        try (InputStream in = new S3OpenDataStorage.PartInputStream(new ByteArrayInputStream(content(100)), 40, null)) {
            assertThat(in.readAllBytes()).hasSize(40);
            assertThat(in.read()).isEqualTo(-1);
        }
    }

    private static byte[] content(long size) {
        byte[] content = new byte[(int) size];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i % 251);
        }
        return content;
    }
}
