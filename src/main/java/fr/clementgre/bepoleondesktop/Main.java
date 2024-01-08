package fr.clementgre.bepoleondesktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Main {
    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static final HidManager hidManager = new HidManager();
    public static final SpotifyManager spotifyManager = new SpotifyManager();

    private static final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private static final AtomicBoolean isDeviceReady = new AtomicBoolean(false);
    private static long last_refresh_millis = System.currentTimeMillis() - 1000 * 60 * 60; // will refresh at start
    private static long last_spotify_millis = System.currentTimeMillis();
    private static long last_perfs_millis = System.currentTimeMillis();

    public static void main(String[] args) {
        while(true){
            try {
                Thread.sleep(100);
                if(System.currentTimeMillis() - last_refresh_millis > 1000 * 60 * 59){
                    logger.info("Refreshing tokens...");
                    last_refresh_millis = System.currentTimeMillis();
                    SpotifyAuthorizer.authorizationCodeRefresh_Sync(spotifyManager.spotifyApi);
                }
                if(System.currentTimeMillis() - last_spotify_millis > 2000 * 5){
                    if(hidManager.isDeviceReady()){
                        isDeviceReady.set(true);
                        if(isDeviceReady.get()){
                            last_spotify_millis = System.currentTimeMillis();
                            CurrentlyPlayingContext currentlyPlaying = spotifyManager.getInformationAboutUsersCurrentPlayback_Sync();
                            if (currentlyPlaying != null && currentlyPlaying.getIs_playing()) {
                                isPlaying.set(true);
                                sendSpotifyData(currentlyPlaying);
                            } else {
                                isPlaying.set(false);
                            }
                        }
                    }else{
                        isDeviceReady.set(false);
                        hidManager.findDevice();
                    }

                }
                if(System.currentTimeMillis() - last_perfs_millis > 2067){
                    last_perfs_millis = System.currentTimeMillis();
                    if(!isPlaying.get() && isDeviceReady.get()){
                        sendSystemDataAndWait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }




    public static void sendSpotifyData(CurrentlyPlayingContext currentlyPlaying){
        Track track = spotifyManager.getTrack_Sync(currentlyPlaying.getItem().getId());
        byte[] data = new byte[32];

        data[1] = (byte) (((float) currentlyPlaying.getProgress_ms()) / ((float) track.getDurationMs()) * 18f);

        data[2] = spotifyManager.isTrackLiked(currentlyPlaying.getItem().getId()) ? (byte) 0x01 : (byte) 0x00;
        data[3] = currentlyPlaying.getShuffle_state() ? (byte) 0x01 : (byte) 0x00;

        // song
        data[0] = 0x01;
        for(int i = 4; i <= 25; i++) data[i] = 0x20;
        char[] chars = removeAccents(currentlyPlaying.getItem().getName()).toCharArray();
        for(int i = 4; i < chars.length+4 && i <= 25; i++) data[i] = (byte) chars[i - 4];
        hidManager.sendData(data);

        // singer
        data[0] = 0x02;
        for(int i = 4; i <= 25; i++) data[i] = 0x20;
        chars = Arrays.stream(track.getArtists()).map(a -> removeAccents(a.getName())).collect(Collectors.joining(", ")).toCharArray();
        for(int i = 4; i < chars.length+4 && i <= 25; i++) data[i] = (byte) chars[i - 4];
        hidManager.sendData(data);
    }

    public static void sendSystemDataAndWait(){
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        CentralProcessor cpu = hal.getProcessor();

        double cpuTemp = hal.getSensors().getCpuTemperature();
        double ramTotal = hal.getMemory().getTotal() / 1073741824d;
        double ramUsed = ramTotal - (hal.getMemory().getAvailable() / 1073741824d);
        double ramLoad = ramUsed / ramTotal;
        double cpuLoad = cpu.getSystemCpuLoad(1900);
        System.out.println("System specs: " + cpuTemp + "°C " + ramUsed + "GB/" + ramTotal + "GB " + cpuLoad + "%" + " " + ramLoad + "%");

        byte[] data = new byte[32];
        data[0] = 0x03;
        data[1] = (byte) (cpuLoad * 100);
        data[2] = (byte) ((cpuLoad * 1000) % 10);
        data[3] = (byte) (cpuTemp);
        data[4] = (byte) ((cpuTemp * 10) % 10);
        data[5] = (byte) (ramLoad * 100);
        data[6] = (byte) ((ramLoad * 1000) % 10);
        data[7] = (byte) (ramUsed);
        data[8] = (byte) ((ramUsed * 10) % 10);

        if(!isPlaying.get() && isDeviceReady.get()){
            hidManager.sendData(data);
        }
    }

    private static String removeAccents(String input) {
        if (input == null) return null;
        return Normalizer.normalize(input, Normalizer.Form.NFKD)
                .replaceAll("é", "\u000E")
                .replaceAll("[^ -~\u000E]", "");
    }
}
