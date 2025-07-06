package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 音频格式转换工具类
 * 将WebM/Opus音频转换为PCM格式
 * 
 * @author example
 * @version 1.0.0
 */
@Slf4j
@Component
public class AudioConverter {

    // 讯飞IAT要求的音频格式
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    /**
     * 处理来自前端的PCM音频数据
     * 前端已经转换为16位PCM格式，这里直接验证和返回
     *
     * @param audioData 音频数据（可能是WebM或PCM）
     * @return PCM格式的音频数据
     */
    public byte[] convertWebMToPCM(byte[] audioData) {
        try {
            log.debug("处理音频数据，输入大小: {} bytes", audioData.length);

            // 检查是否已经是PCM格式（前端发送的）
            if (isPCMFormat(audioData)) {
                log.debug("检测到PCM格式，直接返回");
                return audioData;
            }

            // 尝试使用Java Sound API进行转换
            byte[] pcmData = convertUsingJavaSound(audioData);
            if (pcmData != null && pcmData.length > 0) {
                log.debug("JavaSound转换成功，输出大小: {} bytes", pcmData.length);
                return pcmData;
            }

            // 如果转换失败，生成对应时长的静音数据
            log.warn("音频转换失败，生成对应时长的静音数据");
            int estimatedDurationMs = Math.max(100, audioData.length / 32); // 估算时长
            return generateSilence(estimatedDurationMs);

        } catch (Exception e) {
            log.error("音频转换失败", e);
            return generateSilence(1000); // 1秒静音
        }
    }

    /**
     * 简单检测是否为PCM格式
     * 基于数据长度和内容特征进行判断
     */
    private boolean isPCMFormat(byte[] data) {
        // PCM数据通常长度是2的倍数（16位采样）
        if (data.length % 2 != 0) {
            return false;
        }

        // 检查数据范围是否符合16位PCM特征
        // 这是一个简单的启发式检测
        return data.length > 0 && data.length < 100000; // 合理的音频片段大小
    }



    /**
     * 使用Java Sound API进行音频转换
     */
    private byte[] convertUsingJavaSound(byte[] audioData) {
        try {
            // 尝试读取音频数据
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bais);
            
            // 获取原始音频格式
            AudioFormat sourceFormat = audioInputStream.getFormat();
            log.debug("原始音频格式: {}", sourceFormat);
            
            // 定义目标PCM格式
            AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,
                SAMPLE_SIZE_IN_BITS,
                CHANNELS,
                CHANNELS * SAMPLE_SIZE_IN_BITS / 8,
                SAMPLE_RATE,
                BIG_ENDIAN
            );
            
            // 检查是否支持转换
            if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                log.debug("不支持从 {} 转换到 {}", sourceFormat, targetFormat);
                return null;
            }
            
            // 执行转换
            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            
            // 读取转换后的数据
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = convertedStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            convertedStream.close();
            audioInputStream.close();
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.debug("JavaSound转换失败: {}", e.getMessage());
            return null;
        }
    }



    /**
     * 生成指定时长的静音PCM数据
     * 
     * @param durationMs 时长（毫秒）
     * @return 静音PCM数据
     */
    private byte[] generateSilence(int durationMs) {
        int sampleCount = SAMPLE_RATE * durationMs / 1000;
        int byteCount = sampleCount * CHANNELS * SAMPLE_SIZE_IN_BITS / 8;
        
        byte[] silenceData = new byte[byteCount];
        // PCM静音数据就是全零
        
        log.debug("生成静音数据: {} ms, {} bytes", durationMs, byteCount);
        return silenceData;
    }

    /**
     * 验证PCM数据格式是否正确
     */
    public boolean isValidPCM(byte[] pcmData) {
        if (pcmData == null || pcmData.length == 0) {
            return false;
        }
        
        // 检查数据长度是否符合格式要求
        int bytesPerSample = CHANNELS * SAMPLE_SIZE_IN_BITS / 8;
        return pcmData.length % bytesPerSample == 0;
    }

    /**
     * 获取PCM数据的时长（毫秒）
     */
    public long getPCMDuration(byte[] pcmData) {
        if (!isValidPCM(pcmData)) {
            return 0;
        }
        
        int bytesPerSample = CHANNELS * SAMPLE_SIZE_IN_BITS / 8;
        int sampleCount = pcmData.length / bytesPerSample;
        return (long) sampleCount * 1000 / SAMPLE_RATE;
    }
}
