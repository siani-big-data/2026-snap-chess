package com.chessdigitizer.backend.infrastructure.adapter.out.vision;

import com.chessdigitizer.backend.application.config.GlobalProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PieceClassifierClientTest {

    private MockWebServer server;
    private PieceClassifierClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        GlobalProperties.VisionProperties vision = new GlobalProperties.VisionProperties();
        vision.setDetectorUrl("http://unused");
        vision.setSegmenterUrl("http://unused");
        vision.setClassifierUrl(server.url("/").toString().replaceAll("/$", ""));
        client = new PieceClassifierClient(vision);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void classify_mapsPieceAndWhiteColor_toInternalWp() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"piece\":\"p\",\"color\":\"white\",\"confidence\":0.99}")
                .addHeader("Content-Type", "application/json"));

        assertEquals("wp", client.classify(new byte[]{1, 2, 3}));

        assertClassifyMultipartRequest();
    }

    @Test
    void classify_emptyPiece_returnsEmptyToken() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"piece\":\"empty\",\"confidence\":1.0}")
                .addHeader("Content-Type", "application/json"));

        assertEquals("empty", client.classify(new byte[]{}));

        assertClassifyMultipartRequest();
    }

    private void assertClassifyMultipartRequest() throws InterruptedException {
        var recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertTrue(recorded.getPath().endsWith("/classify"));
        assertNotNull(recorded.getHeader("Content-Type"));
        assertTrue(recorded.getHeader("Content-Type").startsWith("multipart/form-data"));
    }
}
