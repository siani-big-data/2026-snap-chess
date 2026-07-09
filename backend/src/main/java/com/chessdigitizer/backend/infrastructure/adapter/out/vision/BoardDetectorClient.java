package com.chessdigitizer.backend.infrastructure.adapter.out.vision;

import com.chessdigitizer.backend.application.config.GlobalProperties;
import com.chessdigitizer.backend.application.config.GlobalProperties.RenderProperties;
import com.chessdigitizer.backend.application.config.GlobalProperties.VisionProperties;
import com.chessdigitizer.backend.domain.model.BoundingBox;
import com.chessdigitizer.backend.domain.model.BoardDetection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BoardDetectorClient {

    private final RestClient restClient;
    private final RenderProperties renderProperties;

    public BoardDetectorClient(VisionProperties visionProperties, RenderProperties renderProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(visionProperties.getDetectorUrl())
                .build();
        this.renderProperties = renderProperties;
    }

    public List<BoardDetection> detect(byte[] pageImage) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(pageImage) {
            @Override public String getFilename() { return "page.png"; }
        });

        Map<String, Object> response = restClient.post()
                .uri("/detect")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detections =
                (List<Map<String, Object>>) response.get("detections");

        return detections.stream()
                .map(detection -> mapToBoardDetection(detection, pageImage))
                .toList();
    }

    private BoardDetection mapToBoardDetection(Map<String, Object> raw, byte[] pageImage) {
        double x = toDouble(raw.get("x"));
        double y = toDouble(raw.get("y"));
        double w = toDouble(raw.get("w"));
        double h = toDouble(raw.get("h"));

        double factor = renderProperties.getDefaultDpi() / 72.0;
        BoundingBox bbox = new BoundingBox(x / factor, y / factor, w / factor, h / factor);

        byte[] croppedImage = cropImage(pageImage, x, y, w, h);  // el recorte usa píxeles
        return new BoardDetection(bbox, croppedImage);
    }

    private byte[] cropImage(byte[] pageImage, double x, double y, double w, double h) {
        try {
            BufferedImage fullImage = ImageIO.read(new ByteArrayInputStream(pageImage));
            BufferedImage cropped = fullImage.getSubimage(
                    (int) x, (int) y, (int) w, (int) h);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(cropped, "PNG", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al recortar el tablero de la imagen", e);
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        throw new IllegalArgumentException("Valor no numérico en detección: " + value);
    }
}