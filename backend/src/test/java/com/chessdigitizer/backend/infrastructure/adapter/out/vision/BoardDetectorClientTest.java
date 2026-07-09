package com.chessdigitizer.backend.infrastructure.adapter.out.vision;

import com.chessdigitizer.backend.application.config.GlobalProperties;
import com.chessdigitizer.backend.domain.model.BoardDetection;
import com.chessdigitizer.backend.domain.model.BoundingBox;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardDetectorClientTest {

    private MockWebServer server;
    private BoardDetectorClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        GlobalProperties.VisionProperties vision = new GlobalProperties.VisionProperties();
        vision.setDetectorUrl(server.url("/").toString().replaceAll("/$", ""));
        vision.setSegmenterUrl("http://unused");
        vision.setClassifierUrl("http://unused");

        GlobalProperties.RenderProperties render = new GlobalProperties.RenderProperties();
        render.setDefaultDpi(150);

        client = new BoardDetectorClient(vision, render);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void detect_mapsJsonPayload_andCropsBoardImage() throws Exception {
        byte[] pagePng = createPng(40, 40);
        server.enqueue(new MockResponse()
                .setBody("{\"detections\":[{\"x\":0,\"y\":0,\"w\":10,\"h\":10}]}")
                .addHeader("Content-Type", "application/json"));

        List<BoardDetection> boards = client.detect(pagePng);

        assertEquals(1, boards.size());
        BoardDetection b = boards.getFirst();
        assertNotNull(b.image());
        BufferedImage cropped = ImageIO.read(new java.io.ByteArrayInputStream(b.image()));
        assertEquals(10, cropped.getWidth());
        assertEquals(10, cropped.getHeight());

        BoundingBox bbox = b.boundingBox();
        double factor = 150.0 / 72.0;
        assertEquals(round2(10 / factor), round2(bbox.width()));
        assertEquals(round2(10 / factor), round2(bbox.height()));
        assertRecordedRequestMultipart();
    }

    private void assertRecordedRequestMultipart() throws InterruptedException {
        var recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertTrue(recorded.getPath().endsWith("/detect"));
        String ct = recorded.getHeader("Content-Type");
        assertNotNull(ct);
        assertTrue(ct.startsWith("multipart/form-data"));
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static byte[] createPng(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
