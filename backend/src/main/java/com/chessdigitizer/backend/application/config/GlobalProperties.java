package com.chessdigitizer.backend.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class GlobalProperties {

    @ConfigurationProperties(prefix = "app.render")
    @Component
    public static class RenderProperties {
        private int defaultDpi;

        public int getDefaultDpi() {
            return defaultDpi;
        }

        public void setDefaultDpi(int defaultDpi) {
            this.defaultDpi = defaultDpi;
        }
    }

    @ConfigurationProperties(prefix = "app.storage")
    @Component
    public static class StorageProperties{
        private String booksPath;
        private String chessPath;

        public String getBooksPath() {
            return booksPath;
        }

        public void setBooksPath(String booksPath) {
            this.booksPath = booksPath;
        }

        public String getChessPath() {
            return chessPath;
        }

        public void setChessPath(String chessPath) {
            this.chessPath = chessPath;
        }
    }

    @ConfigurationProperties(prefix = "app.vision")
    @Component
    public static class VisionProperties {
        private String detectorUrl;
        private String segmenterUrl;
        private String classifierUrl;

        // getters y setters de los tres campos
        public String getDetectorUrl() { return detectorUrl; }
        public void setDetectorUrl(String detectorUrl) { this.detectorUrl = detectorUrl; }
        public String getSegmenterUrl() { return segmenterUrl; }
        public void setSegmenterUrl(String segmenterUrl) { this.segmenterUrl = segmenterUrl; }
        public String getClassifierUrl() { return classifierUrl; }
        public void setClassifierUrl(String classifierUrl) { this.classifierUrl = classifierUrl; }
    }

    @ConfigurationProperties(prefix = "app.engine")
    @Component
    public static class EngineProperties {
        private String stockfishPath;

        public String getStockfishPath() {
            return stockfishPath;
        }

        public void setStockfishPath(String stockfishPath) {
            this.stockfishPath = stockfishPath;
        }
    }
}

