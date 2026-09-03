package com.trustlance.aiservice.service;

import com.trustlance.aiservice.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for reading document content from MinIO object storage.
 * Documents are retrieved and converted to a text representation for AI analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * Retrieves an object from MinIO and returns its content as a Base64-encoded string
     * for inclusion in the Gemini prompt. For text-based documents, returns plain text.
     *
     * @param objectStorageRef the object storage reference (can be a full URL or object key)
     * @return content as a string suitable for the AI prompt
     */
    public String fetchDocumentContent(String objectStorageRef) {
        // Extract object key from reference (strip bucket prefix if full URL)
        String objectKey = extractObjectKey(objectStorageRef);

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .build())) {

            byte[] bytes = stream.readAllBytes();

            // For images/binary docs: return base64 encoded + mime hint
            // For text documents: return as UTF-8 string directly
            if (looksLikeText(bytes)) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            return "base64:" + Base64.getEncoder().encodeToString(bytes);

        } catch (MinioException e) {
            log.error("MinIO error fetching object {}: {}", objectKey, e.getMessage());
            throw new RuntimeException("Failed to fetch document from storage: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching object {} from MinIO", objectKey, e);
            throw new RuntimeException("Failed to fetch document from storage", e);
        }
    }

    private String extractObjectKey(String ref) {
        // If ref is a full URL like http://minio:9000/bucket/path/to/object
        // extract everything after the bucket name
        if (ref != null && ref.startsWith("http")) {
            String bucket = minioProperties.getBucket();
            int idx = ref.indexOf("/" + bucket + "/");
            if (idx >= 0) {
                return ref.substring(idx + bucket.length() + 2);
            }
        }
        return ref;
    }

    /**
     * Simple heuristic: if >90% of bytes are printable ASCII/UTF-8, treat as text.
     */
    private boolean looksLikeText(byte[] bytes) {
        if (bytes.length == 0) return true;
        int printableCount = 0;
        int sampleSize = Math.min(bytes.length, 512);
        for (int i = 0; i < sampleSize; i++) {
            int b = bytes[i] & 0xFF;
            if (b >= 32 || b == 9 || b == 10 || b == 13) {
                printableCount++;
            }
        }
        return (double) printableCount / sampleSize > 0.90;
    }
}
