package com.example.demo.dto;

import lombok.Data;

/**
 * 讯飞语音识别响应DTO
 *
 * @author example
 * @version 1.0.0
 */
@Data
public class IatResponse {
    private int code;
    private String message;
    private String sid;
    private IatResponseData data;

    @Data
    public static class IatResponseData {
        private int status;
        private Result result;

        @Data
        public static class Result {
            private int bg;
            private int ed;
            private String pgs;
            private int[] rg;
            private int sn;
            private Ws[] ws;
            private boolean ls;

            public String getText() {
                StringBuilder sb = new StringBuilder();
                for (Ws w : ws) {
                    sb.append(w.cw[0].w);
                }
                return sb.toString();
            }

            @Data
            public static class Ws {
                private Cw[] cw;
                private int bg;
                private int ed;

                @Data
                public static class Cw {
                    private int sc;
                    private String w;
                }
            }
        }
    }
}