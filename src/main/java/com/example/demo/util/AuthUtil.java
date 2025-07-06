package com.example.demo.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 讯飞API认证工具类
 *
 * @author example
 * @version 1.0.0
 */
@Component
@Getter
public class AuthUtil {
    @Value("${xfyun.iat.app-id}")
    private String appId;

    @Value("${xfyun.iat.api-key}")
    private String apiKey;

    @Value("${xfyun.iat.api-secret}")
    private String apiSecret;

    @Value("${xfyun.iat.host-url}")
    private String hostUrl;

    @Value("${xfyun.iat.mock-mode:false}")
    private boolean mockMode;

    public String getAuthUrl() throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n")
                .append("date: ").append(date).append("\n")
                .append("GET ").append(url.getPath()).append(" HTTP/1.1");

        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);

        String authParam = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
        String authUrl = "https://" + url.getHost() + url.getPath() +
                "?authorization=" + authParam +
                "&date=" + date +
                "&host=" + url.getHost();

        return authUrl;
    }

    /**
     * 生成WebSocket认证URL
     */
    public String getAuthUrl(String wsHostUrl, String requestPath) {
        try {
            // 将wss://转换为https://进行URL解析
            String httpUrl = wsHostUrl.replace("wss://", "https://") + requestPath;
            URL url = new URL(httpUrl);

            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());

            StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n")
                    .append("date: ").append(date).append("\n")
                    .append("GET ").append(url.getPath()).append(" HTTP/1.1");

            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(StandardCharsets.UTF_8));
            String sha = Base64.getEncoder().encodeToString(hexDigits);

            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    apiKey, "hmac-sha256", "host date request-line", sha);

            String authParam = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));

            // 返回WebSocket URL
            String authUrl = wsHostUrl + requestPath +
                    "?authorization=" + authParam +
                    "&date=" + date +
                    "&host=" + url.getHost();

            return authUrl;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate WebSocket auth URL", e);
        }
    }

    public String getAppId() {
        return appId;
    }

    public boolean isMockMode() {
        return mockMode;
    }
}