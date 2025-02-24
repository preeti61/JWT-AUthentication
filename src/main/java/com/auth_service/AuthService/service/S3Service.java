package com.auth_service.AuthService.service;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Log
@Service
public class S3Service {
    private final String bucketName;
    private final S3Client s3Client;

    public S3Service(@Value("${aws.s3.bucket-name}") String bucketName, S3Client s3Client) {
        this.bucketName = bucketName;
        this.s3Client = s3Client;
    }
    public String doMultipartUpload(MultipartFile file) {
        String key = "upload/" + file.getOriginalFilename();
        String uploadId;
        try {
            CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            CreateMultipartUploadResponse response =  s3Client.createMultipartUpload(createMultipartUploadRequest);
            uploadId = response.uploadId();

            List<CompletedPart> completedParts = new ArrayList<>();
            int partSize = 5 * 1024 * 1024; // 5 MB
            byte[] fileBytes = file.getBytes();
            int fileLength = fileBytes.length;
            int partNumber = 1;

            for(int offset = 0 ; offset < fileLength; offset += partSize) {
                int remaining = Math.min(partSize, fileLength - offset);
                byte[] chunk = new byte[remaining];
                System.arraycopy(fileBytes, offset, chunk, 0, remaining);
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();
                UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(chunk));
                completedParts.add(CompletedPart.builder()
                                .partNumber(partNumber)
                                .eTag(uploadPartResponse.eTag())
                        .build());
                partNumber++;
            }
            CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                    .build();
            s3Client.completeMultipartUpload(completeMultipartUploadRequest);

            log.info("Multipart upload complete");
            return "File uploaded successfully to S3: " + key;

        } catch (IOException e) {
            throw new RuntimeException("Error uploading file", e);
        }
    }
}
