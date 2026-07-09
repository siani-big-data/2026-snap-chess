package com.chessdigitizer.backend.infrastructure.adapter.out.vision;

import com.chessdigitizer.backend.application.config.GlobalProperties;
import com.chessdigitizer.backend.domain.model.CellImage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardSegmenterClientTest {

    private MockWebServer server;
    private BoardSegmenterClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        GlobalProperties.VisionProperties vision = new GlobalProperties.VisionProperties();
        vision.setDetectorUrl("http://unused");
        vision.setSegmenterUrl(server.url("/").toString().replaceAll("/$", ""));
        vision.setClassifierUrl("http://unused");

        client = new BoardSegmenterClient(vision);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void segment_decodesBase64Cells() throws Exception {
        byte[] cellPng = tinyPng();
        String b64 = Base64.getEncoder().encodeToString(cellPng);

        server.enqueue(new MockResponse()
                .setBody("""
                        {"method":"unit","cells":[{"square":"e4","row":4,"col":4,"image_base64":"%s"}]}
                        """.formatted(b64))
                .addHeader("Content-Type", "application/json"));

        List<CellImage> cells = client.segment(new byte[]{1, 2, 3});

        assertEquals(1, cells.size());
        assertEquals("e4", cells.getFirst().notation());
        assertArrayEquals(cellPng, cells.getFirst().image());

        var recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertTrue(recorded.getPath().endsWith("/segment"));
    }

    private static byte[] tinyPng() throws Exception {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
