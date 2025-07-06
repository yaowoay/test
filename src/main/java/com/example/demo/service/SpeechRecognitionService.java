package com.example.demo.service;

import com.example.demo.client.IatWebSocketClient;
import com.example.demo.dto.RecognitionResult;
import com.example.demo.util.AudioConverter;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * 语音识别服务
 *
 * @author example
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SpeechRecognitionService {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionService.class);

    private final IatWebSocketClient webSocketClient;
    private final AudioConverter audioConverter;
    private final Gson gson = new Gson();

    private int status = IatWebSocketClient.StatusFirstFrame;
    private boolean isFirstFrame = true;
    private Consumer<RecognitionResult> resultHandler;

    @PostConstruct
    public void init() {
        // 设置循环依赖引用
        webSocketClient.setSpeechRecognitionService(this);
    }

    /**
     * 设置识别结果处理器
     */
    public void setResultHandler(Consumer<RecognitionResult> handler) {
        this.resultHandler = handler;
    }

    public void processAudioFrame(byte[] audioData, int length) {
        if (isFirstFrame) {
            sendFirstFrame(audioData, length);
            isFirstFrame = false;
        } else {
            sendContinueFrame(audioData, length);
        }
    }

    private void sendFirstFrame(byte[] audioData, int length) {
        webSocketClient.sendFirstFrame(audioData, length);
        status = IatWebSocketClient.StatusContinueFrame;
    }

    private void sendContinueFrame(byte[] audioData, int length) {
        webSocketClient.sendContinueFrame(audioData, length);
    }

    public void endRecognition() {
        webSocketClient.sendLastFrame();
        isFirstFrame = true;
        status = IatWebSocketClient.StatusFirstFrame;
    }

    public void handleRecognitionResult(String resultJson) {
        try {
            RecognitionResult result = gson.fromJson(resultJson, RecognitionResult.class);
            logger.info("Recognition result: text='{}', isFinal={}, confidence={}",
                    result.getText(), result.isFinal(), result.getConfidence());

            // 调用结果处理器
            if (resultHandler != null) {
                resultHandler.accept(result);
            }

            // 如果有错误，记录错误信息
            if (result.getError() != null) {
                logger.error("Recognition error: {}", result.getError());
            }

        } catch (Exception e) {
            logger.error("Failed to parse recognition result: {}", resultJson, e);
        }
    }

    /**
     * 模拟识别结果（用于测试）
     */
    public void simulateRecognitionResult() {
        logger.info("Simulating recognition result for testing...");

        // 模拟部分结果
        RecognitionResult partialResult = RecognitionResult.success("你好", false, 0.8);
        if (resultHandler != null) {
            resultHandler.accept(partialResult);
        }

        // 延迟后发送最终结果
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                RecognitionResult finalResult = RecognitionResult.success("你好世界", true, 0.95);
                if (resultHandler != null) {
                    resultHandler.accept(finalResult);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 处理来自前端的音频数据
     */
    public void processAudioData(String base64AudioData) {
        try {
            // 解码Base64音频数据
            byte[] webmAudioBytes = Base64.getDecoder().decode(base64AudioData);
            logger.debug("收到WebM音频数据，字节长度: {}", webmAudioBytes.length);

            // 转换为PCM格式
            byte[] pcmAudioBytes = audioConverter.convertWebMToPCM(webmAudioBytes);
            logger.debug("转换后PCM音频数据，字节长度: {}", pcmAudioBytes.length);

            // 验证PCM数据
            if (!audioConverter.isValidPCM(pcmAudioBytes)) {
                logger.warn("PCM数据格式无效，跳过此帧");
                return;
            }

            long duration = audioConverter.getPCMDuration(pcmAudioBytes);
            logger.debug("PCM音频时长: {} ms", duration);

            // 发送到讯飞服务器
            if (isFirstFrame) {
                webSocketClient.sendFirstFrame(pcmAudioBytes, pcmAudioBytes.length);
                isFirstFrame = false;
                status = IatWebSocketClient.StatusContinueFrame;
                logger.info("发送第一帧PCM音频数据: {} bytes", pcmAudioBytes.length);
            } else {
                webSocketClient.sendContinueFrame(pcmAudioBytes, pcmAudioBytes.length);
                logger.debug("发送继续帧PCM音频数据: {} bytes", pcmAudioBytes.length);
            }

        } catch (Exception e) {
            logger.error("处理音频数据失败", e);
        }
    }
}