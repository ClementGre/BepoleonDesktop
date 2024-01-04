package fr.clementgre.bepoleondesktop;

import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.library.CheckUsersSavedTracksRequest;
import se.michaelthelin.spotify.requests.data.player.GetInformationAboutUsersCurrentPlaybackRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import java.io.IOException;
import java.util.Properties;

public class SpotifyManager {

    public SpotifyApi spotifyApi;

    public SpotifyManager() {

        Properties secrets = SecretsManager.loadSecrets("spotify_secrets.properties");
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(secrets.getProperty("clientId"))
                .setClientSecret(secrets.getProperty("clientSecret"))
                .setRedirectUri(SpotifyHttpManager.makeUri("https://example.com/spotify-redirect"))
                .setRefreshToken(secrets.getProperty("refreshToken"))
                .build();

        SpotifyAuthorizer.authorizationCodeRefresh_Sync(spotifyApi);
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000 * 60 * 55);
                    SpotifyAuthorizer.authorizationCodeRefresh_Sync(spotifyApi);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public CurrentlyPlayingContext getInformationAboutUsersCurrentPlayback_Sync() {
        try {
            final GetInformationAboutUsersCurrentPlaybackRequest getInformationAboutUsersCurrentPlaybackRequest =
                    spotifyApi.getInformationAboutUsersCurrentPlayback().build();
            return getInformationAboutUsersCurrentPlaybackRequest.execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }

    public Track getTrack_Sync(String id) {
        try {
            final GetTrackRequest getTrackRequest = spotifyApi.getTrack(id)
                    .build();
            return getTrackRequest.execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }

    public boolean isTrackLiked(String id){
        try {
            final CheckUsersSavedTracksRequest checkUsersSavedTracksRequest = spotifyApi.checkUsersSavedTracks(id)
                    .build();
            return checkUsersSavedTracksRequest.execute()[0];
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return false;
    }

}
