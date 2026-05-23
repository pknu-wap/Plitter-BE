package com.playlist.plitter.character.infrastructure.s3;

import com.playlist.plitter.character.application.port.ImageStorageClient;
import com.playlist.plitter.character.application.port.dto.DownloadUrlResult;
import com.playlist.plitter.character.exception.CharacterErrorCode;
import com.playlist.plitter.global.exception.ApiException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Primary
@Component
public class S3ImageStorageClient implements ImageStorageClient, DisposableBean {

    private static final Pattern DATA_URI_PATTERN =
            Pattern.compile("^data:(image/[a-zA-Z0-9.+-]+);base64,(.+)$", Pattern.DOTALL);
    private static final String DEFAULT_CONTENT_TYPE = MediaType.IMAGE_PNG_VALUE;

    private final String bucket;
    private final String keyPrefix;
    private final String publicBaseUrl;
    private final int downloadExpiryMinutes;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RestClient restClient;

    public S3ImageStorageClient(
            @Value("${aws.region:${AWS_REGION:ap-northeast-2}}") String awsRegion,
            @Value("${aws.access-key-id:${AWS_ACCESS_KEY_ID:}}") String accessKeyId,
            @Value("${aws.secret-access-key:${AWS_SECRET_ACCESS_KEY:}}") String secretAccessKey,
            @Value("${character.storage.s3.bucket}") String bucket,
            @Value("${character.storage.s3.key-prefix:characters}") String keyPrefix,
            @Value("${character.storage.s3.public-base-url:}") String publicBaseUrl,
            @Value("${character.storage.s3.download-expiry-minutes:30}") int downloadExpiryMinutes
    ) {
        this.bucket = bucket;
        this.keyPrefix = normalizeKeyPrefix(keyPrefix);
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
        this.downloadExpiryMinutes = downloadExpiryMinutes > 0 ? downloadExpiryMinutes : 30;

        Region region = Region.of(awsRegion);
        AwsCredentialsProvider credentialsProvider = resolveCredentialsProvider(accessKeyId, secretAccessKey);
        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
        this.s3Presigner = S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String storeCharacterImage(Long playlistId, String sourceImageUrl) {
        try {
            StoredImageInput input = resolveSourceImage(sourceImageUrl);
            String objectKey = createObjectKey(playlistId, input.extension());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(input.contentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(input.bytes()));
            return buildPublicImageUrl(objectKey);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    @Override
    public DownloadUrlResult createDownloadUrl(String imageUrl) {
        try {
            String objectKey = extractObjectKey(imageUrl);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(downloadExpiryMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(downloadExpiryMinutes);
            return new DownloadUrlResult(presignedRequest.url().toString(), expiresAt);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    @Override
    public void destroy() {
        s3Presigner.close();
        s3Client.close();
    }

    private StoredImageInput resolveSourceImage(String sourceImageUrl) {
        if (!StringUtils.hasText(sourceImageUrl)) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
        if (sourceImageUrl.startsWith("data:image/")) {
            return parseDataUri(sourceImageUrl);
        }
        return downloadRemoteImage(sourceImageUrl);
    }

    private StoredImageInput parseDataUri(String dataUri) {
        Matcher matcher = DATA_URI_PATTERN.matcher(dataUri);
        if (!matcher.matches()) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }

        String contentType = matcher.group(1);
        String base64Data = matcher.group(2);
        byte[] bytes = Base64.getDecoder().decode(base64Data);
        String extension = resolveExtension(contentType);
        return new StoredImageInput(bytes, contentType, extension);
    }

    private StoredImageInput downloadRemoteImage(String sourceImageUrl) {
        ResponseEntity<byte[]> response = restClient.get()
                .uri(URI.create(sourceImageUrl))
                .retrieve()
                .toEntity(byte[].class);

        byte[] bytes = response.getBody();
        if (bytes == null || bytes.length == 0) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }

        String contentType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : DEFAULT_CONTENT_TYPE;
        String extension = resolveExtension(contentType);
        return new StoredImageInput(bytes, contentType, extension);
    }

    private String createObjectKey(Long playlistId, String extension) {
        String randomName = UUID.randomUUID().toString().replace("-", "");
        return keyPrefix + "/" + playlistId + "/" + randomName + "." + extension;
    }

    private String buildPublicImageUrl(String objectKey) {
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl + "/" + objectKey;
        }
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder().bucket(bucket).key(objectKey).build())
                .toExternalForm();
    }

    private String extractObjectKey(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
        if (StringUtils.hasText(publicBaseUrl) && imageUrl.startsWith(publicBaseUrl + "/")) {
            return imageUrl.substring((publicBaseUrl + "/").length());
        }

        URI uri = URI.create(imageUrl);
        String path = uri.getPath();
        if (!StringUtils.hasText(path)) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }

        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        if (normalizedPath.startsWith(bucket + "/")) {
            return normalizedPath.substring(bucket.length() + 1);
        }
        return normalizedPath;
    }

    private String resolveExtension(String contentType) {
        String lowerContentType = contentType.toLowerCase();
        if (lowerContentType.contains("image/png")) {
            return "png";
        }
        if (lowerContentType.contains("image/jpeg") || lowerContentType.contains("image/jpg")) {
            return "jpg";
        }
        if (lowerContentType.contains("image/webp")) {
            return "webp";
        }
        return "png";
    }

    private String normalizeKeyPrefix(String rawPrefix) {
        if (!StringUtils.hasText(rawPrefix)) {
            return "characters";
        }
        String trimmed = rawPrefix.trim();
        String withoutLeading = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        return withoutLeading.endsWith("/") ? withoutLeading.substring(0, withoutLeading.length() - 1) : withoutLeading;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private AwsCredentialsProvider resolveCredentialsProvider(String accessKeyId, String secretAccessKey) {
        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey)) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim())
            );
        }
        return DefaultCredentialsProvider.create();
    }

    private record StoredImageInput(
            byte[] bytes,
            String contentType,
            String extension
    ) {
    }
}
