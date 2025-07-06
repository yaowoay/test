package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语音识别结果DTO
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResult {

    /**
     * 识别的文本内容
     */
    private String text;

    /**
     * 是否为最终结果
     */
    private boolean isFinal;

    /**
     * 置信度分数 (0-1)
     */
    private double confidence;

    /**
     * 识别状态码
     */
    private int status;

    /**
     * 错误信息（如果有）
     */
    private String error;

    /**
     * 创建成功的识别结果
     */
    public static RecognitionResult success(String text, boolean isFinal, double confidence) {
        return new RecognitionResult(text, isFinal, confidence, 0, null);
    }

    /**
     * 创建错误的识别结果
     */
    public static RecognitionResult error(String error) {
        return new RecognitionResult(null, false, 0.0, -1, error);
    }
}
