package com.playlist.plitter.character.infrastructure.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.playlist.plitter.character.application.port.CharacterImageEditClient;
import com.playlist.plitter.character.application.port.dto.CharacterImageEditRequest;
import com.playlist.plitter.character.exception.CharacterErrorCode;
import com.playlist.plitter.global.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Primary
@Component
public class OpenAiCharacterImageEditClient implements CharacterImageEditClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCharacterImageEditClient.class);
    private static final String IMAGE_EDIT_PATH = "/v1/images/edits";
    private static final String DEFAULT_IMAGE_MEDIA_TYPE = "image/png";
    private static final String SHAPE_PRESERVATION_RULES =
            "Strict character constraints: keep the same base character identity and silhouette. " +
                    "Modify the provided character only; do not regenerate a different character from scratch. " +
                    "Keep the same body framework, limb count, limb placement, and overall proportions. " +
                    "Keep the star outline fully readable and do not occlude major silhouette points with large accessories. " +
                    "Do not redesign into a different species, humanoid body, or new character archetype. " +
                    "Only apply style-level variations (expression, outfit details, accessories, compact visual marks). " +
                    "Preserve the exact drawing style of input image, including line weight, stroke texture, and rendering style. " +
                    "Keep a rough hand-drawn doodle line-art look. " +
                    "Do not fill the character body with solid colors. " +
                    "Do not fill the background with colors. " +
                    "Do not apply any color fills to character parts or accessories. " +
                    "Keep output strictly monochrome with black or dark-gray lines only. " +
                    "Prefer black or dark-gray thin stroke lines, minimal shading, and a plain background. " +
                    "Avoid heavy cross-hatching or dense sketch textures. " +
                    "Output exactly one full-body character.";

    private final String openAiApiKey;
    private final String openAiModel;
    private final String openAiSize;
    private final String openAiQuality;
    private final RestClient openAiRestClient;
    private final RestClient sourceImageRestClient;

    public OpenAiCharacterImageEditClient(
            @Value("${character.openai.api-key:${OPENAI_API_KEY:}}") String openAiApiKey,
            @Value("${character.openai.base-url:https://api.openai.com}") String openAiBaseUrl,
            @Value("${character.openai.image-edit.model:gpt-image-1-mini}") String openAiModel,
            @Value("${character.openai.image-edit.size:1024x1024}") String openAiSize,
            @Value("${character.openai.image-edit.quality:low}") String openAiQuality,
            @Value("${character.openai.image-edit.timeout-millis:30000}") int timeoutMillis
    ) {
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
        this.openAiSize = openAiSize;
        this.openAiQuality = openAiQuality;
        this.openAiRestClient = RestClient.builder()
                .baseUrl(openAiBaseUrl)
                .requestFactory(createRequestFactory(timeoutMillis))
                .build();
        this.sourceImageRestClient = RestClient.builder()
                .requestFactory(createRequestFactory(timeoutMillis))
                .build();
    }

    @Override
    public String editCharacterImage(CharacterImageEditRequest request) {
        if (!StringUtils.hasText(openAiApiKey)) {
            log.error("OpenAI API key is empty");
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
        if (!StringUtils.hasText(request.baseCharacterImage().imageUrl())) {
            log.error("Base character image URL is empty");
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }

        try {
            SourceImage sourceImage = loadSourceImage(request.baseCharacterImage().imageUrl());
            byte[] imageBytes = sourceImage.bytes();

            if (imageBytes == null || imageBytes.length == 0) {
                log.error("Loaded base image bytes are empty: source={}", request.baseCharacterImage().imageUrl());
                throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
            }

            HttpHeaders imageHeaders = new HttpHeaders();
            imageHeaders.setContentType(sourceImage.mediaType());
            HttpEntity<ByteArrayResource> imagePart = new HttpEntity<>(
                    new NamedByteArrayResource(imageBytes, sourceImage.filename()),
                    imageHeaders
            );

            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("model", openAiModel);
            formData.add("prompt", buildPrompt(request.editSpec().promptText()));
            formData.add("size", openAiSize);
            formData.add("quality", openAiQuality);
            formData.add("n", "1");
            formData.add("image[]", imagePart);
            formData.add("output_format", "png");

            ImageEditResponse response = openAiRestClient.post()
                    .uri(IMAGE_EDIT_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                    .body(formData)
                    .retrieve()
                    .body(ImageEditResponse.class);

            if (response == null || response.data == null || response.data.isEmpty()) {
                log.error("OpenAI image edit response is empty");
                throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
            }

            ImageResult result = response.data.get(0);
            if (StringUtils.hasText(result.url)) {
                return result.url;
            }
            if (StringUtils.hasText(result.b64Json)) {
                return "data:image/png;base64," + result.b64Json;
            }
            log.error("OpenAI image edit response has neither url nor b64_json");
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        } catch (RestClientResponseException e) {
            log.error(
                    "OpenAI image edit failed: status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI image edit unexpected failure: {}", e.getMessage(), e);
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private SimpleClientHttpRequestFactory createRequestFactory(int timeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int safeTimeout = timeoutMillis > 0 ? timeoutMillis : 30000;
        requestFactory.setConnectTimeout(Duration.ofMillis(safeTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(safeTimeout));
        return requestFactory;
    }

    private String buildPrompt(String promptText) {
        return SHAPE_PRESERVATION_RULES + " " + promptText;
    }

    private MediaType resolveImageMediaType(MediaType sourceMediaType) {
        if (sourceMediaType == null || !sourceMediaType.getType().equals("image")) {
            return MediaType.parseMediaType(DEFAULT_IMAGE_MEDIA_TYPE);
        }
        return sourceMediaType;
    }

    private SourceImage loadSourceImage(String sourceImageLocation) throws Exception {
        if (sourceImageLocation.startsWith("classpath:")) {
            String classpathLocation = sourceImageLocation.substring("classpath:".length());
            return fromClasspath(classpathLocation);
        }
        if (sourceImageLocation.startsWith("file:")) {
            Path path = Path.of(URI.create(sourceImageLocation));
            return fromLocalPath(path);
        }

        ResponseEntity<byte[]> sourceImageResponse = sourceImageRestClient.get()
                .uri(URI.create(sourceImageLocation))
                .retrieve()
                .toEntity(byte[].class);
        byte[] body = sourceImageResponse.getBody();
        MediaType mediaType = resolveImageMediaType(sourceImageResponse.getHeaders().getContentType());
        String filename = resolveFilename(sourceImageResponse.getHeaders(), sourceImageLocation);
        return new SourceImage(body, mediaType, filename);
    }

    private SourceImage fromClasspath(String classpathLocation) throws Exception {
        String normalizedLocation = classpathLocation.startsWith("/")
                ? classpathLocation.substring(1)
                : classpathLocation;
        ClassPathResource resource = new ClassPathResource(normalizedLocation);
        byte[] body = resource.getInputStream().readAllBytes();
        String filename = resource.getFilename() != null ? resource.getFilename() : "base-character.png";
        MediaType mediaType = resolveImageMediaType(resolveMediaTypeFromFilename(filename));
        return new SourceImage(body, mediaType, filename);
    }

    private SourceImage fromLocalPath(Path path) throws Exception {
        byte[] body = Files.readAllBytes(path);
        MediaType mediaType = resolveImageMediaType(resolveMediaTypeFromPath(path));
        String filename = path.getFileName() != null ? path.getFileName().toString() : "base-character.png";
        return new SourceImage(body, mediaType, filename);
    }

    private MediaType resolveMediaTypeFromPath(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            if (!StringUtils.hasText(contentType)) {
                return null;
            }
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return null;
        }
    }

    private MediaType resolveMediaTypeFromFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return null;
    }

    private String resolveFilename(HttpHeaders headers, String sourceImageUrl) {
        String headerFilename = headers.getContentDisposition().getFilename();
        if (StringUtils.hasText(headerFilename)) {
            return headerFilename;
        }

        String sanitized = sourceImageUrl.split("\\?")[0];
        int slashIndex = sanitized.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex + 1 < sanitized.length()) {
            return sanitized.substring(slashIndex + 1);
        }
        return "base-character.png";
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private record SourceImage(
            byte[] bytes,
            MediaType mediaType,
            String filename
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ImageEditResponse(
            List<ImageResult> data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ImageResult(
            String url,
            @JsonProperty("b64_json") String b64Json
    ) {
    }
}
