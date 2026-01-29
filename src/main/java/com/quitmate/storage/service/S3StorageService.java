package com.quitmate.storage.service;

import com.quitmate.storage.enums.BucketKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket.user}")
    private String userBucketName;

    @Value("${aws.s3.bucket.challenge}")
    private String challengeBucketName;

    @Value("${aws.s3.bucket.challenge-badge}")
    private String challengeBadgeBucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * S3에 파일 업로드
     *
     * @param file 업로드할 파일
     * @return 업로드된 파일의 S3 URL
     */
    public String upload(MultipartFile file, BucketKind bucketKind) {
        validateImageFile(file);
        return uploadFileToS3(file, getBucketName(bucketKind));
    }

    /**
     * MultipartFile을 S3에 업로드
     *
     * @param file       업로드할 파일
     * @param bucketName S3 버킷 이름
     * @return 업로드된 객체 키
     */
    public String uploadFileToS3(MultipartFile file, String bucketName) {
        validateFile(file);

        try {
            String objectKey = generateObjectKey(file.getOriginalFilename());
            String contentType = determineContentType(file);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();

            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            log.info("S3 파일 업로드 성공: {}, ETag: {}", objectKey, response.eTag());
            return objectKey;

        } catch (IOException e) {
            log.error("파일 업로드 중 IO 오류 발생: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("파일 업로드 실패", e);
        } catch (Exception e) {
            log.error("S3 파일 업로드 중 오류 발생: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    /**
     * Presigned URL 생성 (임시 접근 URL)
     *
     * @param objectKey  S3 객체 키
     * @param bucketName S3 버킷 이름
     * @param duration   URL 유효 시간
     * @return Presigned URL
     */
    public String createPresignedUrl(String objectKey, String bucketName, Duration duration) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(req -> req
                            .bucket(bucketName)
                            .key(objectKey))
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.info("Presigned URL 생성 성공: {}, 유효시간: {}", objectKey, duration);
            return presignedUrl;

        } catch (Exception e) {
            log.error("Presigned URL 생성 중 오류 발생: {}", objectKey, e);
            throw new RuntimeException("Presigned URL 생성 실패", e);
        }
    }

    /**
     * Presigned URL 생성 (기본 1시간)
     *
     * @param objectKey  S3 객체 키
     * @param bucketKind 버킷 종류
     * @return Presigned URL
     */
    public String createPresignedUrl(String objectKey, BucketKind bucketKind) {
        String bucketName = getBucketName(bucketKind);
        return createPresignedUrl(objectKey, bucketName, Duration.ofHours(1));
    }

    /**
     * BucketKind에 따른 버킷 이름 반환
     *
     * @param bucketKind 버킷 종류
     * @return 버킷 이름
     */
    private String getBucketName(BucketKind bucketKind) {
        return switch (bucketKind) {
            case USER -> userBucketName;
            case CHALLENGE -> challengeBucketName;
            case CHALLENGE_BADGE -> challengeBadgeBucketName;
        };
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
    }

    private void validateImageFile(MultipartFile file) {
        validateFile(file);

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        String[] allowedTypes = {"image/jpeg", "image/png", "image/gif", "image/webp"};
        boolean isAllowed = false;
        for (String allowedType : allowedTypes) {
            if (allowedType.equals(contentType)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (JPEG, PNG, GIF, WebP만 지원)");
        }
    }

    private String generateObjectKey(String originalFilename) {
        String filename = UUID.randomUUID().toString();

        // 파일 확장자 처리
        String extension = getFileExtension(originalFilename);
        if (!filename.endsWith(extension)) {
            filename += extension;
        }

        return filename;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }

        return filename.substring(lastDotIndex).toLowerCase();
    }

    private String determineContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            return contentType;
        }

        // 파일 확장자로 추정
        String extension = getFileExtension(file.getOriginalFilename());
        return switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".pdf" -> "application/pdf";
            case ".txt" -> "text/plain";
            case ".json" -> "application/json";
            default -> "application/octet-stream";
        };
    }
}
