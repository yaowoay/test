package com.example.demo.dto;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class IatRequest {
    // 必填参数
    private String appId;
    private String language;  // 语言: zh_cn（中文）、en_us（英文）等
    private String domain;     // 领域: iat（日常用语）、medical（医疗）等

    // 可选参数
    private String accent;     // 方言: mandarin（普通话）、cantonese（粤语）等
    private Boolean ptt;       // 是否加标点（true/false）
    private String dwa;        // 动态修正: wpgs（开启）

    // 转换为讯飞API要求的JSON格式
    public JsonObject toFirstFrameJson() {
        JsonObject frame = new JsonObject();
        JsonObject common = new JsonObject();
        JsonObject business = new JsonObject();

        // 填充公共参数
        common.addProperty("app_id", this.appId);

        // 填充业务参数
        business.addProperty("language", this.language != null ? this.language : "zh_cn");
        business.addProperty("domain", this.domain != null ? this.domain : "iat");

        if (this.accent != null) {
            business.addProperty("accent", this.accent);
        }
        if (this.ptt != null) {
            business.addProperty("ptt", this.ptt ? 1 : 0); // 转换为整数
        } else {
            business.addProperty("ptt", 1); // 默认开启标点，使用整数
        }
        if (this.dwa != null) {
            business.addProperty("dwa", this.dwa);
        }

        frame.add("common", common);
        frame.add("business", business);
        return frame;
    }
}