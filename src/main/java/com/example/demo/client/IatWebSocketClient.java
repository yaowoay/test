package com.example.demo.client;

import com.example.demo.dto.IatRequest;
import com.example.demo.dto.RecognitionResult;
import com.example.demo.service.SpeechRecognitionService;
import com.example.demo.util.AuthUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞语音识别WebSocket客户端
 * 
 * @author example
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IatWebSocketClient {

    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;

    private final AuthUtil authUtil;
    private final Gson gson = new Gson();

    private WebSocket webSocket;
    private IatRequest iatRequest;
    private SpeechRecognitionService speechRecognitionService;
    private OkHttpClient client;

    @PostConstruct
    public void init() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.iatRequest = new IatRequest();
        this.iatRequest.setAppId(authUtil.getAppId());
    }

    /**
     * 设置语音识别服务引用（避免循环依赖）
     */
    public void setSpeechRecognitionService(SpeechRecognitionService service) {
        this.speechRecognitionService = service;
    }

    /**
     * 建立WebSocket连接
     */
    public void connect() {
        if (webSocket != null) {
            log.warn("WebSocket already connected");
            return;
        }

        // 检查是否为模拟模式
        if (authUtil.isMockMode()) {
            log.info("运行在模拟模式，跳过真实WebSocket连接");
            simulateConnection();
            return;
        }

        try {
            String authUrl = authUtil.getAuthUrl("wss://iat-api.xfyun.cn", "/v2/iat");
            Request request = new Request.Builder()
                    .url(authUrl)
                    .build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("WebSocket connected to XunFei IAT service");
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("WebSocket connection failed", t);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("WebSocket connection closed: {} - {}", code, reason);
                }
            });

        } catch (Exception e) {
            log.error("Failed to connect to XunFei IAT service", e);
        }
    }

    /**
     * 处理来自讯飞服务器的消息
     */
    private void handleMessage(String message) {
        try {
            JsonObject response = JsonParser.parseString(message).getAsJsonObject();

            if (response.has("data")) {
                JsonObject data = response.getAsJsonObject("data");
                if (data.has("result")) {
                    // 解析识别结果
                    String resultText = parseRecognitionResult(data);
                    boolean isFinal = data.has("status") && data.get("status").getAsInt() == 2;

                    RecognitionResult result = RecognitionResult.success(resultText, isFinal, 1.0);

                    if (speechRecognitionService != null) {
                        speechRecognitionService.handleRecognitionResult(gson.toJson(result));
                    }
                }
            }

            if (response.has("code")) {
                int code = response.get("code").getAsInt();
                if (code != 0) {
                    String errorMsg = response.has("message") ?
                            response.get("message").getAsString() : "Unknown error";
                    log.error("XunFei IAT error: {} - {}", code, errorMsg);

                    RecognitionResult errorResult = RecognitionResult.error(errorMsg);
                    if (speechRecognitionService != null) {
                        speechRecognitionService.handleRecognitionResult(gson.toJson(errorResult));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse recognition result", e);
        }
    }

    /**
     * 解析识别结果文本
     */
    private String parseRecognitionResult(JsonObject data) {
        StringBuilder resultText = new StringBuilder();

        if (data.has("result") && data.get("result").isJsonObject()) {
            JsonObject result = data.getAsJsonObject("result");
            if (result.has("ws") && result.get("ws").isJsonArray()) {
                result.getAsJsonArray("ws").forEach(ws -> {
                    if (ws.isJsonObject() && ws.getAsJsonObject().has("cw")) {
                        ws.getAsJsonObject().getAsJsonArray("cw").forEach(cw -> {
                            if (cw.isJsonObject() && cw.getAsJsonObject().has("w")) {
                                resultText.append(cw.getAsJsonObject().get("w").getAsString());
                            }
                        });
                    }
                });
            }
        }

        return resultText.toString();
    }
    
    /**
     * 发送第一帧音频数据
     */
    public void sendFirstFrame(byte[] audioData, int length) {
        // 检查是否为模拟模式
        if (authUtil.isMockMode()) {
            log.info("模拟模式：发送第一帧音频数据，长度: {}", length);
            simulateRecognition(Base64.getEncoder().encodeToString(audioData));
            return;
        }

        if (webSocket == null) {
            connect();
            // 等待连接建立
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (webSocket == null) {
            log.error("WebSocket connection not established");
            return;
        }

        log.info("Sending first frame, length: {}", length);

        // 构造第一帧消息
        JsonObject frame = iatRequest.toFirstFrameJson();

        // 添加音频数据
        JsonObject data = new JsonObject();
        data.addProperty("status", StatusFirstFrame);
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");
        data.addProperty("audio", Base64.getEncoder().encodeToString(audioData));

        frame.add("data", data);

        log.debug("第一帧JSON: {}", frame.toString());

        boolean sent = webSocket.send(frame.toString());
        if (!sent) {
            log.error("Failed to send first frame");
        }
    }

    /**
     * 发送继续帧音频数据
     */
    public void sendContinueFrame(byte[] audioData, int length) {
        // 检查是否为模拟模式
        if (authUtil.isMockMode()) {
            log.debug("模拟模式：发送继续帧音频数据，长度: {}", length);
            return; // 模拟模式下不需要继续发送
        }

        if (webSocket == null) {
            log.warn("WebSocket not connected, skipping continue frame");
            return;
        }

        log.debug("Sending continue frame, length: {}", length);

        JsonObject frame = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("status", StatusContinueFrame);
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");
        data.addProperty("audio", Base64.getEncoder().encodeToString(audioData));

        frame.add("data", data);

        log.debug("继续帧JSON长度: {}", frame.toString().length());

        boolean sent = webSocket.send(frame.toString());
        if (!sent) {
            log.error("Failed to send continue frame");
        }
    }

    /**
     * 发送最后一帧（结束标志）
     */
    public void sendLastFrame() {
        // 检查是否为模拟模式
        if (authUtil.isMockMode()) {
            log.info("模拟模式：发送最后一帧");
            return;
        }

        if (webSocket == null) {
            log.warn("WebSocket not connected, skipping last frame");
            return;
        }

        log.info("Sending last frame");

        JsonObject frame = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("status", StatusLastFrame);
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");
        data.addProperty("audio", "");

        frame.add("data", data);

        log.debug("结束帧JSON: {}", frame.toString());

        boolean sent = webSocket.send(frame.toString());
        if (!sent) {
            log.error("Failed to send last frame");
        }

        // 关闭连接
        disconnect();
    }

    /**
     * 断开WebSocket连接
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
            webSocket = null;
            log.info("WebSocket disconnected");
        }
    }

    /**
     * 模拟WebSocket连接
     */
    private void simulateConnection() {
        log.info("模拟WebSocket连接建立");
        // 模拟连接成功
    }

    /**
     * 模拟识别结果
     */
    private void simulateRecognition(String audioData) {
        // 模拟处理延迟
        new Thread(() -> {
            try {
                Thread.sleep(500); // 模拟网络延迟

                // 模拟部分识别结果
                String partialResult = "{\"data\":{\"result\":{\"ws\":[{\"cw\":[{\"w\":\"你好\"}]}]},\"status\":1},\"code\":0}";
                handleMessage(partialResult);

                Thread.sleep(1000);

                // 模拟最终识别结果
                String finalResult = "{\"data\":{\"result\":{\"ws\":[{\"cw\":[{\"w\":\"你好世界，这是模拟识别结果\"}]}]},\"status\":2},\"code\":0}";
                handleMessage(finalResult);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }


}
