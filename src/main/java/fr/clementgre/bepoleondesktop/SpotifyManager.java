package fr.clementgre.bepoleondesktop;

import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger logger = LoggerFactory.getLogger(SpotifyManager.class);
    public SpotifyApi spotifyApi;

    public SpotifyManager() {

        Properties secrets = SecretsManager.loadSecrets("spotify_secrets.properties");
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(secrets.getProperty("clientId"))
                .setClientSecret(secrets.getProperty("clientSecret"))
                .setRedirectUri(SpotifyHttpManager.makeUri("https://example.com"))
                .setRefreshToken(secrets.getProperty("refreshToken"))
                .build();
    }

    public CurrentlyPlayingContext getInformationAboutUsersCurrentPlayback_Sync() {
        try {
            final GetInformationAboutUsersCurrentPlaybackRequest getInformationAboutUsersCurrentPlaybackRequest =
                    spotifyApi.getInformationAboutUsersCurrentPlayback().build();
            return getInformationAboutUsersCurrentPlaybackRequest.execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }
        return null;
    }

    public Track getTrack_Sync(String id) {
        try {
            final GetTrackRequest getTrackRequest = spotifyApi.getTrack(id)
                    .build();
            return getTrackRequest.execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }
        return null;
    }

    public boolean isTrackLiked(String id) {
        try {
            final CheckUsersSavedTracksRequest checkUsersSavedTracksRequest = spotifyApi.checkUsersSavedTracks(id)
                    .build();
            return checkUsersSavedTracksRequest.execute()[0];
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }
        return false;
    }

    public void pauseResume() {
        CurrentlyPlayingContext currentlyPlaying = getInformationAboutUsersCurrentPlayback_Sync();
        if (currentlyPlaying != null && currentlyPlaying.getIs_playing()) {
            pause();
        } else {
            resume();
        }
    }
    public void resume() {
        try {
            spotifyApi.startResumeUsersPlayback().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }
    }
    public void pause() {
        try {
            spotifyApi.pauseUsersPlayback().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }
    }
    public void next() {
        try {
            spotifyApi.skipUsersPlaybackToNextTrack().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }
    }
    public void previous() {
        try {
            spotifyApi.skipUsersPlaybackToPreviousTrack().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }
    }
    public void likeUnlike(String id) {
        try {
            if (isTrackLiked(id)) {
                logger.info("Unliking track: " + id);
                spotifyApi.removeUsersSavedTracks(id).build().execute();
            } else {
                logger.info("Liking track: " + id);
                spotifyApi.saveTracksForUser(id).build().execute();
            }
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }
    }
}
