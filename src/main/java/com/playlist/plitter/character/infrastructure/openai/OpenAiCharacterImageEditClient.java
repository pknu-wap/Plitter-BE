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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Primary
@Component
public class OpenAiCharacterImageEditClient implements CharacterImageEditClient {

    private static final String IMAGE_EDIT_PATH = "/v1/images/edits";
    private static final String DEFAULT_IMAGE_MEDIA_TYPE = "image/png";

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
            @Value("${character.openai.image-edit.quality:medium}") String openAiQuality,
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
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }

        try {
            byte[] imageBytes = sourceImageRestClient.get()
                    .uri(URI.create(request.baseCharacterImage().imageUrl()))
                    .retrieve()
                    .body(byte[].class);

            if (imageBytes == null || imageBytes.length == 0) {
                throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
            }

            HttpHeaders imageHeaders = new HttpHeaders();
            imageHeaders.setContentType(MediaType.parseMediaType(DEFAULT_IMAGE_MEDIA_TYPE));
            HttpEntity<ByteArrayResource> imagePart = new HttpEntity<>(
                    new NamedByteArrayResource(imageBytes, "base-character.png"),
                    imageHeaders
            );

            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("model", openAiModel);
            formData.add("prompt", request.editSpec().promptText());
            formData.add("size", openAiSize);
            formData.add("quality", openAiQuality);
            formData.add("n", "1");
            formData.add("image", imagePart);

            ImageEditResponse response = openAiRestClient.post()
                    .uri(IMAGE_EDIT_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                    .body(formData)
                    .retrieve()
                    .body(ImageEditResponse.class);

            if (response == null || response.data == null || response.data.isEmpty()) {
                throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
            }

            ImageResult result = response.data.get(0);
            if (StringUtils.hasText(result.url)) {
                return result.url;
            }
            if (StringUtils.hasText(result.b64Json)) {
                return "data:image/png;base64," + result.b64Json;
            }
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        } catch (RestClientResponseException e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(CharacterErrorCode.CHARACTER_GENERATION_FAILED);
        }
    }

    private SimpleClientHttpRequestFactory createRequestFactory(int timeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int safeTimeout = Optional.of(timeoutMillis)
                .filter(value -> value > 0)
                .orElse(30000);
        requestFactory.setConnectTimeout(Duration.ofMillis(safeTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(safeTimeout));
        return requestFactory;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ImageEditResponse {
        private List<ImageResult> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ImageResult {
        private String url;
        @JsonProperty("b64_json")
        private String b64Json;
    }
}
