package org.open4goods.services.prompt.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Represents the response from the OpenAI File Upload API.
 *
 * <p>
 * Contains details such as file ID, size, creation time, and purpose.
 * </p>
 */
public class FileUploadResponse {
    private String id;
    private String object;
    private Integer bytes;
    @JsonProperty("created_at")
    private long createdAt;
    private String filename;
    private String purpose;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Integer getBytes() {
        return bytes;
    }

    public void setBytes(Integer bytes) {
        this.bytes = bytes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    @Override
    public String toString() {
        return "FileUploadResponse{" +
                "id='" + id + '\'' +
                ", object='" + object + '\'' +
                ", bytes=" + bytes +
                ", createdAt=" + createdAt +
                ", filename='" + filename + '\'' +
                ", purpose='" + purpose + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileUploadResponse)) return false;
        FileUploadResponse that = (FileUploadResponse) o;
        return createdAt == that.createdAt &&
               Objects.equals(id, that.id) &&
               Objects.equals(object, that.object) &&
               Objects.equals(bytes, that.bytes) &&
               Objects.equals(filename, that.filename) &&
               Objects.equals(purpose, that.purpose);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, object, bytes, createdAt, filename, purpose);
    }
}
