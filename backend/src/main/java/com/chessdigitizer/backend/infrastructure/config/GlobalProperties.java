package com.chessdigitizer.backend.infrastructure.config;

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
}

