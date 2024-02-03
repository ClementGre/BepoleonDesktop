package fr.clementgre.bepoleondesktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Main implements HidManager.HidDataListener {
    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static final HidManager hidManager = new HidManager(new Main());
    public static final SpotifyManager spotifyManager = new SpotifyManager();

    private static final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private static final AtomicBoolean isDeviceReady = new AtomicBoolean(false);
    private static long last_refresh_millis = System.currentTimeMillis() - 1000 * 60 * 60; // will refresh at start
    private static long last_spotify_millis = System.currentTimeMillis();
    private static long last_perfs_millis = System.currentTimeMillis();
    private static long last_loop_millis = System.currentTimeMillis();

    private static final long REFRESH_INTERVAL = 1000 * 60 * 59;
    private static final long SPOTIFY_INTERVAL = 2000 * 5;
    private static final long PERFS_INTERVAL = 2067;

    public static final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();


    public static void main(String[] args) {
//        SpotifyAuthorizer.printAuthRequestUri(spotifyManager.spotifyApi);
//        SpotifyAuthorizer.printAuthTokensFromCode(spotifyManager.spotifyApi, "");

        while (true) {
            try {
                Thread.sleep(100);
                while (!queue.isEmpty()){
                    queue.take().run();
                }

                if (System.currentTimeMillis() - last_loop_millis > 1000 * 20) {
                    // System went to sleep, unregister device in case the device has been unplugged
                    if (hidManager.isDeviceReady()) {
                        hidManager.unregisterDevice();
                        hidManager.findDevice();
                    }
                }
                last_loop_millis = System.currentTimeMillis();

                if (System.currentTimeMillis() - last_refresh_millis > REFRESH_INTERVAL) {
                    logger.info("Refreshing tokens...");
                    if (SpotifyAuthorizer.authorizationCodeRefresh_Sync(spotifyManager.spotifyApi))
                        last_refresh_millis = System.currentTimeMillis();
                    else
                        last_refresh_millis = System.currentTimeMillis() - REFRESH_INTERVAL - 1000 * 5;
                }
                if (System.currentTimeMillis() - last_spotify_millis > SPOTIFY_INTERVAL) {
                    if (hidManager.isDeviceReady()) {
                        isDeviceReady.set(true);
                        if (isDeviceReady.get()) {
                            last_spotify_millis = System.currentTimeMillis();
                            CurrentlyPlayingContext currentlyPlaying = spotifyManager.getInformationAboutUsersCurrentPlayback_Sync();
                            if (currentlyPlaying != null && currentlyPlaying.getIs_playing()) {
                                isPlaying.set(true);
                                sendSpotifyData(currentlyPlaying);
                            } else {
                                isPlaying.set(false);
                            }
                        }
                    } else {
                        isDeviceReady.set(false);
                        //hidManager.findDevice();
                    }

                }
                if (System.currentTimeMillis() - last_perfs_millis > PERFS_INTERVAL) {
                    last_perfs_millis = System.currentTimeMillis();
                    if (!isPlaying.get() && isDeviceReady.get()) {
                        sendSystemDataAndWait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public static void sendSpotifyData(CurrentlyPlayingContext currentlyPlaying) {
        Track track = spotifyManager.getTrack_Sync(currentlyPlaying.getItem().getId());
        byte[] data = new byte[32];

        data[1] = (byte) (((float) currentlyPlaying.getProgress_ms()) / ((float) track.getDurationMs()) * 18f);

        data[2] = spotifyManager.isTrackLiked(currentlyPlaying.getItem().getId()) ? (byte) 0x01 : (byte) 0x00;
        data[3] = currentlyPlaying.getShuffle_state() ? (byte) 0x01 : (byte) 0x00;

        // song
        data[0] = 0x01;
        for (int i = 4; i <= 25; i++) data[i] = 0x20;
        char[] chars = removeAccents(currentlyPlaying.getItem().getName()).toCharArray();
        for (int i = 4; i < chars.length + 4 && i <= 25; i++) data[i] = (byte) chars[i - 4];
        hidManager.sendData(data);

        // singer
        data[0] = 0x02;
        for (int i = 4; i <= 25; i++) data[i] = 0x20;
        chars = Arrays.stream(track.getArtists()).map(a -> removeAccents(a.getName())).collect(Collectors.joining(", ")).toCharArray();
        for (int i = 4; i < chars.length + 4 && i <= 25; i++) data[i] = (byte) chars[i - 4];
        hidManager.sendData(data);
    }

    private static long[] oldCpuTicks = new long[CentralProcessor.TickType.values().length];
    private static long oldCpuTicksTime = 0;
    public static void sendSystemDataAndWait() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        CentralProcessor cpu = hal.getProcessor();

        double cpuTemp = hal.getSensors().getCpuTemperature();
        double ramTotal = hal.getMemory().getTotal() / 1073741824d;
        double ramUsed = ramTotal - (hal.getMemory().getAvailable() / 1073741824d);
        double ramLoad = ramUsed / ramTotal;
        double cpuLoad;
        if(System.currentTimeMillis() - oldCpuTicksTime > 5000){
            cpuLoad = cpu.getSystemCpuLoad(1000) * 10;
        }else{
            cpuLoad = cpu.getSystemCpuLoadBetweenTicks(oldCpuTicks) * 10;
        }
        logger.debug("System specs: " + cpuTemp + "°C " + ramUsed + "GB/" + ramTotal + "GB " + (cpuLoad*100) + "%" + " " + (ramLoad*100) + "%");

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

        oldCpuTicks = cpu.getSystemCpuLoadTicks();
        oldCpuTicksTime = System.currentTimeMillis();
        if (!isPlaying.get() && isDeviceReady.get()) {
            hidManager.sendData(data);
        }
    }

    private static String removeAccents(String input) {
        if (input == null) return null;
        return Normalizer.normalize(input, Normalizer.Form.NFKD)
                .replaceAll("e\u0301", "\u000E")
                .replaceAll("[^ -~\u000E]", "");
    }

    public static int getCpuLoad() {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"wmic", "cpu", "get", "loadpercentage"};
        try {
            Process proc = rt.exec(commands);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            String s = null;
            boolean isLastTitle = false;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                if (isLastTitle && !s.isBlank()) {
                    return Integer.parseInt(s.trim());
                }
                if (s.startsWith("LoadPercentage")) {
                    isLastTitle = true;
                }
            }
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void requestDataSendUpdate(){
        // Adding a delay of 300ms to make sure the spotify status is updated
        last_spotify_millis = System.currentTimeMillis() - SPOTIFY_INTERVAL + 300;
        last_perfs_millis = System.currentTimeMillis() - PERFS_INTERVAL + 300;
    }

    @Override
    public void onHidDataReceived(byte[] data) {
        if (data[0] == 0x00) {
            requestDataSendUpdate();
        }else if (data[0] == 0x01) {
            // Spotify control
            if (data[1] == 0x00){
                logger.info("SPS Key pressed received!");
                CurrentlyPlayingContext currentlyPlaying = spotifyManager.getInformationAboutUsersCurrentPlayback_Sync();
                if (currentlyPlaying != null && currentlyPlaying.getIs_playing()) {
                    spotifyManager.likeUnlike(currentlyPlaying.getItem().getId());
                }else{
                    spotifyManager.resume();
                }
            }else if (data[1] == 0x01){
                logger.info("SNext Key pressed received!");
                spotifyManager.next();
            }else if (data[1] == 0x02){
                logger.info("SBack Key pressed received!");
                spotifyManager.previous();
            }else if (data[1] == 0x03){
                logger.info("SPause Key pressed received!");
                spotifyManager.pauseResume();
            }
            requestDataSendUpdate();
        }
    }
}
