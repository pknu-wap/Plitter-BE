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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Primary
@Component
public class OpenAiCharacterImageEditClient implements CharacterImageEditClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCharacterImageEditClient.class);
    private static final String IMAGE_EDIT_PATH = "/v1/images/edits";
    private static final String DEFAULT_IMAGE_MEDIA_TYPE = "image/png";
    private static final int TRANSPARENT_BACKGROUND_THRESHOLD = 245;
    private static final String SHAPE_PRESERVATION_RULES =
            "Strict character constraints: keep the same doodle star mascot family and base silhouette logic. " +
                    "Preserve the rough hand-drawn line quality and naive doodle character feel from the input image. " +
                    "Create one fresh applied variation, not a polished redraw and not a different species or object. " +
                    "Props may vary from generation to generation, but they must stay secondary and must not hide the face or replace the star silhouette. " +
                    "Keep exactly one full-body character.";
    private static final String REFERENCE_SHEET_GUIDE =
            "The additional uploaded reference sheet shows the same character family with base, expression, and applied examples. " +
                    "Use that sheet as style vocabulary only. Create one new applied variation instead of copying any exact example composition.";

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
            List<SourceImage> inputImages = prepareInputImages(sourceImage);
            if (inputImages.isEmpty() || inputImages.get(0).bytes() == null || inputImages.get(0).bytes().length == 0) {
                log.error("Loaded base image bytes are empty: source={}", request.baseCharacterImage().imageUrl());
                throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
            }

            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("model", openAiModel);
            formData.add("prompt", buildPrompt(request.editSpec().promptText(), inputImages.size() > 1));
            formData.add("size", openAiSize);
            formData.add("quality", openAiQuality);
            formData.add("n", "1");
            formData.add("output_format", "png");
            for (SourceImage inputImage : inputImages) {
                HttpHeaders imageHeaders = new HttpHeaders();
                imageHeaders.setContentType(inputImage.mediaType());
                HttpEntity<ByteArrayResource> imagePart = new HttpEntity<>(
                        new NamedByteArrayResource(inputImage.bytes(), inputImage.filename()),
                        imageHeaders
                );
                formData.add("image[]", imagePart);
            }

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
                return toTransparentPngDataUri(downloadGeneratedImage(result.url));
            }
            if (StringUtils.hasText(result.b64Json)) {
                return toTransparentPngDataUri(java.util.Base64.getDecoder().decode(result.b64Json));
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

    private String buildPrompt(String promptText, boolean hasReferenceSheet) {
        if (!hasReferenceSheet) {
            return SHAPE_PRESERVATION_RULES + " " + promptText;
        }
        return SHAPE_PRESERVATION_RULES + " " + REFERENCE_SHEET_GUIDE + " " + promptText;
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

    private byte[] downloadGeneratedImage(String imageUrl) {
        ResponseEntity<byte[]> response = sourceImageRestClient.get()
                .uri(URI.create(imageUrl))
                .retrieve()
                .toEntity(byte[].class);

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            log.error("Generated image download returned empty body: {}", imageUrl);
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
        return body;
    }

    private List<SourceImage> prepareInputImages(SourceImage sourceImage) throws Exception {
        if (!looksLikeReferenceSheet(sourceImage)) {
            return List.of(sourceImage);
        }

        SourceImage primaryBaseImage = extractPrimaryBaseCharacter(sourceImage);
        List<SourceImage> inputImages = new ArrayList<>();
        inputImages.add(primaryBaseImage);
        inputImages.add(new SourceImage(
                sourceImage.bytes(),
                sourceImage.mediaType(),
                appendFilenameSuffix(sourceImage.filename(), "-reference")
        ));
        return inputImages;
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

    private boolean looksLikeReferenceSheet(SourceImage sourceImage) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(sourceImage.bytes()));
            return image != null && image.getWidth() >= image.getHeight() * 2;
        } catch (Exception e) {
            return false;
        }
    }

    private SourceImage extractPrimaryBaseCharacter(SourceImage sourceImage) throws Exception {
        BufferedImage sheet = ImageIO.read(new ByteArrayInputStream(sourceImage.bytes()));
        if (sheet == null) {
            return sourceImage;
        }

        int width = sheet.getWidth();
        int height = sheet.getHeight();
        int scanStartY = Math.max(0, height / 5);
        int scanEndY = height - 1;
        int scanEndX = Math.min(width - 1, Math.max(height, width / 4));

        int minX = scanEndX;
        int minY = scanEndY;
        int maxX = 0;
        int maxY = 0;

        for (int y = scanStartY; y <= scanEndY; y++) {
            for (int x = 0; x <= scanEndX; x++) {
                if (isInkPixel(sheet.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX <= minX || maxY <= minY) {
            return sourceImage;
        }

        int padding = Math.max(12, height / 20);
        int cropX = Math.max(0, minX - padding);
        int cropY = Math.max(0, minY - padding);
        int cropWidth = Math.min(width - cropX, (maxX - minX) + (padding * 2) + 1);
        int cropHeight = Math.min(height - cropY, (maxY - minY) + (padding * 2) + 1);

        BufferedImage cropped = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = cropped.createGraphics();
        graphics.drawImage(
                sheet,
                0,
                0,
                cropWidth,
                cropHeight,
                cropX,
                cropY,
                cropX + cropWidth,
                cropY + cropHeight,
                null
        );
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(cropped, "png", outputStream);
        return new SourceImage(
                outputStream.toByteArray(),
                MediaType.IMAGE_PNG,
                appendFilenameSuffix(sourceImage.filename(), "-source")
        );
    }

    private boolean isInkPixel(int argb) {
        int alpha = (argb >> 24) & 0xff;
        int red = (argb >> 16) & 0xff;
        int green = (argb >> 8) & 0xff;
        int blue = argb & 0xff;
        return alpha > 32 && (red < 245 || green < 245 || blue < 245);
    }

    private String appendFilenameSuffix(String filename, String suffix) {
        if (!StringUtils.hasText(filename)) {
            return "base-character" + suffix + ".png";
        }
        int extensionIndex = filename.lastIndexOf('.');
        if (extensionIndex < 0) {
            return filename + suffix;
        }
        return filename.substring(0, extensionIndex) + suffix + filename.substring(extensionIndex);
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

    private String toTransparentPngDataUri(byte[] imageBytes) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            log.error("Generated image bytes could not be decoded");
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }

        BufferedImage transparentImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                transparentImage.setRGB(x, y, toTransparentBackgroundPixel(argb));
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(transparentImage, "png", outputStream);
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private int toTransparentBackgroundPixel(int argb) {
        int alpha = (argb >> 24) & 0xff;
        int red = (argb >> 16) & 0xff;
        int green = (argb >> 8) & 0xff;
        int blue = argb & 0xff;

        if (alpha == 0) {
            return 0x00000000;
        }

        boolean nearWhite = red >= TRANSPARENT_BACKGROUND_THRESHOLD
                && green >= TRANSPARENT_BACKGROUND_THRESHOLD
                && blue >= TRANSPARENT_BACKGROUND_THRESHOLD;
        if (nearWhite) {
            return 0x00000000;
        }
        return argb;
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
