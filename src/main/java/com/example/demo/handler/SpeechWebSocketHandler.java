package com.example.demo.handler;

import com.example.demo.dto.IatRequest;
import com.example.demo.service.AudioCaptureService;
import com.example.demo.service.SpeechRecognitionService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音WebSocket处理器
 *
 * @author example
 * @version 1.0.0
 */
@Slf4j
public class SpeechWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(SpeechWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final AudioCaptureService audioCaptureService;
    private final SpeechRecognitionService recognitionService;
    private final Gson gson = new Gson();

    public SpeechWebSocketHandler(AudioCaptureService audioCaptureService,
                                  SpeechRecognitionService recognitionService) {
        this.audioCaptureService = audioCaptureService;
        this.recognitionService = recognitionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        logger.info("新的WebSocket连接: {}", session.getId());

        // 发送连接成功消息
        try {
            JsonObject connectResponse = new JsonObject();
            connectResponse.addProperty("type", "status");
            connectResponse.addProperty("message", "WebSocket connected successfully");
            session.sendMessage(new TextMessage(gson.toJson(connectResponse)));
            logger.info("已发送连接成功消息到客户端");
        } catch (Exception e) {
            logger.error("发送连接成功消息失败", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.info("收到WebSocket消息: {}", payload);

        try {
            // 尝试解析JSON消息
            JsonObject messageObj = gson.fromJson(payload, JsonObject.class);
            String type = messageObj.get("type").getAsString();

            if ("audio".equals(type)) {
                // 处理音频数据
                String audioData = messageObj.get("data").getAsString();
                logger.debug("收到音频数据，长度: {}", audioData.length());

                // 将Base64音频数据传递给识别服务
                recognitionService.processAudioData(audioData);
                return;
            } else if ("command".equals(type)) {
                // 处理命令消息
                String action = messageObj.get("action").getAsString();
                logger.info("收到命令: {}", action);
                handleCommand(session, action);
                return;
            }
        } catch (Exception e) {
            // 如果不是JSON格式，按原来的方式处理
            logger.debug("消息不是JSON格式，按文本命令处理: {}", payload);
        }

        // 处理文本命令
        handleCommand(session, payload);
    }

    /**
     * 处理命令
     */
    private void handleCommand(WebSocketSession session, String command) throws Exception {
        if ("start".equals(command)) {
            // 设置识别结果处理器，将结果发送给前端
            recognitionService.setResultHandler(result -> {
                try {
                    if (session.isOpen()) {
                        JsonObject jsonResponse = new JsonObject();
                        jsonResponse.addProperty("type", "recognition");
                        jsonResponse.addProperty("text", result.getText() != null ? result.getText() : "");
                        jsonResponse.addProperty("isFinal", result.isFinal());
                        jsonResponse.addProperty("confidence", result.getConfidence());

                        String resultJson = gson.toJson(jsonResponse);
                        session.sendMessage(new TextMessage(resultJson));
                        logger.info("发送识别结果到前端: {}", resultJson);
                    }
                } catch (Exception e) {
                    logger.error("发送识别结果失败", e);
                }
            });

            audioCaptureService.startCapture();

            JsonObject statusResponse = new JsonObject();
            statusResponse.addProperty("type", "status");
            statusResponse.addProperty("message", "Audio capture started");
            session.sendMessage(new TextMessage(gson.toJson(statusResponse)));

            // 添加模拟测试（用于调试）
            recognitionService.simulateRecognitionResult();

        } else if ("stop".equals(command)) {
            audioCaptureService.stopCapture();
            recognitionService.endRecognition();

            JsonObject stopResponse = new JsonObject();
            stopResponse.addProperty("type", "status");
            stopResponse.addProperty("message", "Audio capture stopped");
            session.sendMessage(new TextMessage(gson.toJson(stopResponse)));
        } else if ("test".equals(command)) {
            logger.info("收到测试命令，开始模拟识别结果");
            // 测试命令，直接模拟识别结果
            recognitionService.setResultHandler(result -> {
                try {
                    if (session.isOpen()) {
                        JsonObject jsonResponse = new JsonObject();
                        jsonResponse.addProperty("type", "recognition");
                        jsonResponse.addProperty("text", result.getText() != null ? result.getText() : "");
                        jsonResponse.addProperty("isFinal", result.isFinal());
                        jsonResponse.addProperty("confidence", result.getConfidence());

                        String resultJson = gson.toJson(jsonResponse);
                        session.sendMessage(new TextMessage(resultJson));
                        logger.info("发送测试识别结果到前端: {}", resultJson);
                    } else {
                        logger.warn("WebSocket会话已关闭，无法发送结果");
                    }
                } catch (Exception e) {
                    logger.error("发送识别结果失败", e);
                }
            });

            logger.info("开始执行模拟识别");
            recognitionService.simulateRecognitionResult();

            JsonObject testResponse = new JsonObject();
            testResponse.addProperty("type", "status");
            testResponse.addProperty("message", "Test recognition started");
            session.sendMessage(new TextMessage(gson.toJson(testResponse)));
            logger.info("测试识别命令处理完成");
        } else {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("type", "error");
            errorResponse.addProperty("message", "Unknown command: " + command);
            session.sendMessage(new TextMessage(gson.toJson(errorResponse)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        audioCaptureService.stopCapture();
        logger.info("WebSocket连接关闭: {}, 状态: {}", session.getId(), status);
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error: {}", session.getId(), exception);
        super.handleTransportError(session, exception);
    }
}