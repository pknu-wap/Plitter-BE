package com.playlist.plitter.track.infrastructure.spotify;

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
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.search.SearchItemRequest;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SpotifyTrackClient {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    public List<TrackSearchResponse> searchTracks(String keyword, Integer limit) {
        try {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .build();

            ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
            ClientCredentials clientCredentials = clientCredentialsRequest.execute();

            spotifyApi.setAccessToken(clientCredentials.getAccessToken());

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
}
