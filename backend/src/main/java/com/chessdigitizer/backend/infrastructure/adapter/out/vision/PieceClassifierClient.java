package com.chessdigitizer.backend.infrastructure.adapter.out.vision;

import com.chessdigitizer.backend.application.config.GlobalProperties.VisionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class PieceClassifierClient {

    private final RestClient restClient;

    public PieceClassifierClient(VisionProperties visionProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(visionProperties.getClassifierUrl())
                .build();
    }

    public String classify(byte[] cellImage) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(cellImage) {
            @Override public String getFilename() { return "cell.png"; }
        });

        Map<String, Object> response = restClient.post()
                .uri("/classify")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);

        return mapToInternalClass(response);
    }

    private String mapToInternalClass(Map<String, Object> response) {
        String piece = (String) response.get("piece");
        String color = (String) response.get("color");

        log.info("MS3 respuesta cruda → piece: '{}', color: '{}'", piece, color);

        if ("empty".equals(piece)) {
            return "empty";
        }

        String prefix = "white".equals(color) ? "w" : "b";
        log.info("MS3 clasificación interna → '{}'", prefix+piece);
        return prefix + piece;
    }
}