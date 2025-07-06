package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;

/**
 * 音频捕获服务
 *
 * @author example
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AudioCaptureService {
    private static final Logger logger = LoggerFactory.getLogger(AudioCaptureService.class);

    private final SpeechRecognitionService recognitionService;

    private TargetDataLine targetDataLine;
    private boolean isCapturing = false;

    // 音频格式配置
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            16000,  // 采样率 16kHz
            16,     // 采样位数
            1,      // 单声道
            true,   // 有符号
            false   // 小端序
    );

    public void startCapture() {
        if (isCapturing) {
            logger.warn("Audio capture is already running");
            return;
        }

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(AUDIO_FORMAT);
            targetDataLine.start();

            isCapturing = true;
            new Thread(this::captureAudio).start();
            logger.info("Audio capture started");
        } catch (LineUnavailableException e) {
            logger.error("Failed to start audio capture", e);
        }
    }

    public void stopCapture() {
        isCapturing = false;
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
            logger.info("Audio capture stopped");
        }
    }

    private void captureAudio() {
        int frameSize = 1280; // 每40ms的音频数据 (16000Hz * 16bit * 1channel * 0.04s / 8 = 1280 bytes)
        byte[] buffer = new byte[frameSize];

        while (isCapturing) {
            int bytesRead = targetDataLine.read(buffer, 0, frameSize);
            if (bytesRead > 0) {
                recognitionService.processAudioFrame(buffer, bytesRead);
            }
        }
    }
}