package com.trustlance.contractservice.dto;

import lombok.Data;

@Data
public class SubmitMilestoneRequest {

    private String notes;

    // Comma-separated or JSON list of object-storage keys/URLs
    // Binary files go to MinIO/S3, only the references are stored here
    private String objectStorageRefs;
}
