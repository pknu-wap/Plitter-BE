package com.playlist.plitter.track.infrastructure.spotify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.plitter.global.exception.ApiException;
import com.playlist.plitter.track.application.dto.TrackSearchResponse;
import com.playlist.plitter.track.exception.TrackErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.ModelObjectType;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.special.SearchResult;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.AudioFeatures;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.search.SearchItemRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpotifyTrackClient {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private final ObjectMapper objectMapper;

    public List<TrackSearchResponse> searchTracks(String keyword, Integer limit) {
        try {
            SpotifyApi spotifyApi = createSpotifyApi();

            SearchItemRequest searchItemRequest = spotifyApi.searchItem(keyword, ModelObjectType.TRACK.getType())
                    .limit(limit)
                    .build();

            SearchResult searchResult = searchItemRequest.execute();
            Track[] tracks = searchResult.getTracks().getItems();

            List<TrackSearchResponse> result = new ArrayList<>();

            for (Track track : tracks) {
                String artistName = track.getArtists().length > 0
                        ? track.getArtists()[0].getName()
                        : "";

                String albumImageUrl = "";
                Image[] images = track.getAlbum().getImages();
                if (images != null && images.length > 0) {
                    albumImageUrl = images[0].getUrl();
                }

                String spotifyUrl = track.getExternalUrls().getExternalUrls().get("spotify");

                result.add(new TrackSearchResponse(
                        track.getId(),
                        track.getName(),
                        artistName,
                        albumImageUrl,
                        track.getPreviewUrl(),
                        spotifyUrl
                ));
            }

            return result;
        } catch (UnauthorizedException e) {
            throw new ApiException(TrackErrorCode.SPOTIFY_UNAUTHORIZED);
        } catch (Exception e) {
            throw new ApiException(TrackErrorCode.TRACK_SEARCH_FAILED);
        }
    }

    public SpotifyTrackFeatureResponse getTrackFeature(String spotifyTrackId) {
        try {
            SpotifyApi spotifyApi = createSpotifyApi();

            AudioFeatures audioFeatures = spotifyApi.getAudioFeaturesForTrack(spotifyTrackId)
                    .build()
                    .execute();
            Track track = spotifyApi.getTrack(spotifyTrackId)
                    .build()
                    .execute();
            String genre = getGenre(spotifyApi, track);

            return new SpotifyTrackFeatureResponse(
                    toBigDecimal(audioFeatures.getTempo()),
                    genre,
                    toBigDecimal(audioFeatures.getEnergy()),
                    toBigDecimal(audioFeatures.getValence()),
                    toRawFeatureJson(audioFeatures, genre)
            );
        } catch (UnauthorizedException e) {
            throw new ApiException(TrackErrorCode.SPOTIFY_UNAUTHORIZED);
        } catch (Exception e) {
            throw new ApiException(TrackErrorCode.TRACK_SEARCH_FAILED);
        }
    }

    private SpotifyApi createSpotifyApi() throws Exception {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();

        ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
        ClientCredentials clientCredentials = clientCredentialsRequest.execute();

        spotifyApi.setAccessToken(clientCredentials.getAccessToken());
        return spotifyApi;
    }

    private String getGenre(SpotifyApi spotifyApi, Track track) throws Exception {
        ArtistSimplified[] artists = track.getArtists();
        if (artists == null || artists.length == 0 || artists[0].getId() == null) {
            return "unknown";
        }

        Artist artist = spotifyApi.getArtist(artists[0].getId())
                .build()
                .execute();
        String[] genres = artist.getGenres();
        if (genres == null || genres.length == 0) {
            return "unknown";
        }

        String genre = String.join(", ", genres);
        return genre.length() > 100 ? genre.substring(0, 100) : genre;
    }

    private BigDecimal toBigDecimal(Float value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value.doubleValue());
    }

    private String toRawFeatureJson(AudioFeatures audioFeatures, String genre) throws JsonProcessingException {
        Map<String, Object> rawFeature = new LinkedHashMap<>();
        rawFeature.put("id", audioFeatures.getId());
        rawFeature.put("tempo", audioFeatures.getTempo());
        rawFeature.put("energy", audioFeatures.getEnergy());
        rawFeature.put("valence", audioFeatures.getValence());
        rawFeature.put("danceability", audioFeatures.getDanceability());
        rawFeature.put("acousticness", audioFeatures.getAcousticness());
        rawFeature.put("instrumentalness", audioFeatures.getInstrumentalness());
        rawFeature.put("liveness", audioFeatures.getLiveness());
        rawFeature.put("loudness", audioFeatures.getLoudness());
        rawFeature.put("speechiness", audioFeatures.getSpeechiness());
        rawFeature.put("key", audioFeatures.getKey());
        rawFeature.put("mode", audioFeatures.getMode());
        rawFeature.put("timeSignature", audioFeatures.getTimeSignature());
        rawFeature.put("durationMs", audioFeatures.getDurationMs());
        rawFeature.put("genre", genre);

        return objectMapper.writeValueAsString(rawFeature);
    }
}
