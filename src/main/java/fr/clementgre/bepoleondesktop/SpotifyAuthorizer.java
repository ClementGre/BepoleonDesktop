package fr.clementgre.bepoleondesktop;

import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SpotifyAuthorizer {

    public static final Logger logger = LoggerFactory.getLogger(SpotifyAuthorizer.class);

    public static void printAuthRequestUri(SpotifyApi spotifyApi) {
        final AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope(AuthorizationScope.STREAMING, AuthorizationScope.APP_REMOTE_CONTROL, AuthorizationScope.USER_READ_PLAYBACK_STATE,
                        AuthorizationScope.USER_MODIFY_PLAYBACK_STATE, AuthorizationScope.PLAYLIST_READ_PRIVATE, AuthorizationScope.PLAYLIST_MODIFY_PRIVATE,
                        AuthorizationScope.USER_LIBRARY_READ, AuthorizationScope.USER_LIBRARY_MODIFY)
                .build();
        final URI uri = authorizationCodeUriRequest.execute();

        logger.info("URI: " + uri.toString());
    }

    public static void printAuthTokensFromCode(SpotifyApi spotifyApi, String code) {
        try {
            final AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();

            logger.info("AccessToken: " + authorizationCodeCredentials.getAccessToken());
            logger.info("RefreshToken: " + authorizationCodeCredentials.getRefreshToken());
            logger.info("Expires in: " + authorizationCodeCredentials.getExpiresIn());
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
        }
    }

    public static boolean authorizationCodeRefresh_Sync(SpotifyApi spotifyApi) {
        try {
            final AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
                    .build();
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();

            // Set access and refresh token for further "spotifyApi" object usage
            logger.debug("New access token: " + authorizationCodeCredentials.getAccessToken());
            logger.debug("New refresh token: " + authorizationCodeCredentials.getRefreshToken());
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());

            logger.debug("Expires in: " + authorizationCodeCredentials.getExpiresIn());
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            logger.error("Error: " + e.getMessage());
            return false;
        }
    }

    public static CompletableFuture<AuthorizationCodeCredentials> authorizationCodeRefresh_Async(SpotifyApi spotifyApi) {
        try {
            final AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
                    .build();
            return authorizationCodeRefreshRequest.executeAsync();
        } catch (CompletionException e) {
            logger.error("Error: " + e.getCause().getMessage());
        } catch (CancellationException e) {
            logger.error("Async operation cancelled.");
        }
        return null;
    }

}
