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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

@Primary
@Component
public class S3ImageStorageClient implements ImageStorageClient, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(S3ImageStorageClient.class);
    private static final Pattern DATA_URI_PATTERN =
            Pattern.compile("^data:(image/[a-zA-Z0-9.+-]+);base64,(.+)$", Pattern.DOTALL);
    private static final String DEFAULT_CONTENT_TYPE = MediaType.IMAGE_PNG_VALUE;
    private static final int BACKGROUND_WHITE_THRESHOLD = 245;
    private static final int STROKE_RESIDUAL_WHITE_THRESHOLD = 235;
    private static final int STROKE_SOFT_BACKGROUND_THRESHOLD = 220;
    private static final int STROKE_SOFT_BACKGROUND_ALPHA_THRESHOLD = 96;
    private static final int MIN_STROKE_ALPHA = 170;

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
            log.error("S3 image upload failed: playlistId={}, source={}", playlistId, sourceImageUrl, e);
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
            log.error("S3 download URL creation failed: imageUrl={}", imageUrl, e);
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
        return normalizeStoredImage(bytes, contentType);
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
        return normalizeStoredImage(bytes, contentType);
    }

    private StoredImageInput normalizeStoredImage(byte[] bytes, String contentType) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                String extension = resolveExtension(contentType);
                return new StoredImageInput(bytes, contentType, extension);
            }

            BufferedImage alphaImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            alphaImage.getGraphics().drawImage(image, 0, 0, null);
            alphaImage.getGraphics().dispose();

            int transparentPixelCount = makeOuterBrightBackgroundTransparent(alphaImage);
            if (transparentPixelCount == 0) {
                transparentPixelCount = makeAllBrightPixelsTransparent(alphaImage);
            }

            normalizeForegroundStrokes(alphaImage);

            if (transparentPixelCount == 0) {
                String extension = resolveExtension(contentType);
                return new StoredImageInput(bytes, contentType, extension);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(alphaImage, "png", outputStream);
            return new StoredImageInput(outputStream.toByteArray(), DEFAULT_CONTENT_TYPE, "png");
        } catch (Exception e) {
            log.warn("Image background normalization skipped: {}", e.getMessage());
            String extension = resolveExtension(contentType);
            return new StoredImageInput(bytes, contentType, extension);
        }
    }

    private int makeOuterBrightBackgroundTransparent(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        ArrayDeque<Point> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();

        for (int x = 0; x < width; x++) {
            enqueueBackgroundPixel(image, queue, visited, x, 0);
            enqueueBackgroundPixel(image, queue, visited, x, height - 1);
        }
        for (int y = 0; y < height; y++) {
            enqueueBackgroundPixel(image, queue, visited, 0, y);
            enqueueBackgroundPixel(image, queue, visited, width - 1, y);
        }

        int transparentCount = 0;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            int x = point.x;
            int y = point.y;

            if (!isBrightBackgroundCandidate(image.getRGB(x, y))) {
                continue;
            }

            image.setRGB(x, y, 0x00000000);
            transparentCount++;

            if (x > 0) {
                enqueueBackgroundPixel(image, queue, visited, x - 1, y);
            }
            if (x + 1 < width) {
                enqueueBackgroundPixel(image, queue, visited, x + 1, y);
            }
            if (y > 0) {
                enqueueBackgroundPixel(image, queue, visited, x, y - 1);
            }
            if (y + 1 < height) {
                enqueueBackgroundPixel(image, queue, visited, x, y + 1);
            }
        }
        return transparentCount;
    }

    private void enqueueBackgroundPixel(
            BufferedImage image,
            ArrayDeque<Point> queue,
            Set<Long> visited,
            int x,
            int y
    ) {
        long key = (((long) x) << 32) | (y & 0xffffffffL);
        if (!visited.add(key)) {
            return;
        }
        if (!isBrightBackgroundCandidate(image.getRGB(x, y))) {
            return;
        }
        queue.addLast(new Point(x, y));
    }

    private int makeAllBrightPixelsTransparent(BufferedImage image) {
        int transparentCount = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isBrightBackgroundCandidate(image.getRGB(x, y))) {
                    image.setRGB(x, y, 0x00000000);
                    transparentCount++;
                }
            }
        }
        return transparentCount;
    }

    private void normalizeForegroundStrokes(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xff;
                if (alpha == 0) {
                    continue;
                }

                int red = (argb >> 16) & 0xff;
                int green = (argb >> 8) & 0xff;
                int blue = argb & 0xff;
                int luminance = (red + green + blue) / 3;

                if (luminance >= STROKE_RESIDUAL_WHITE_THRESHOLD
                        || (luminance >= STROKE_SOFT_BACKGROUND_THRESHOLD
                        && alpha <= STROKE_SOFT_BACKGROUND_ALPHA_THRESHOLD)) {
                    image.setRGB(x, y, 0x00000000);
                    continue;
                }

                int darkness = 255 - luminance;
                int targetAlpha = Math.max(alpha, Math.min(255, darkness * 2));
                if (luminance < STROKE_SOFT_BACKGROUND_THRESHOLD) {
                    targetAlpha = Math.max(targetAlpha, MIN_STROKE_ALPHA);
                }

                image.setRGB(x, y, (targetAlpha << 24));
            }
        }
    }

    private boolean isBrightBackgroundCandidate(int argb) {
        int alpha = (argb >> 24) & 0xff;
        if (alpha == 0) {
            return true;
        }

        int red = (argb >> 16) & 0xff;
        int green = (argb >> 8) & 0xff;
        int blue = argb & 0xff;
        return red >= BACKGROUND_WHITE_THRESHOLD
                && green >= BACKGROUND_WHITE_THRESHOLD
                && blue >= BACKGROUND_WHITE_THRESHOLD;
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
