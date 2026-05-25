package com.chessdigitizer.backend.infrastructure.adapter.out.vision;

import com.chessdigitizer.backend.application.config.GlobalProperties.VisionProperties;
import com.chessdigitizer.backend.domain.model.CellImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class BoardSegmenterClient {

    private final RestClient restClient;

    public BoardSegmenterClient(VisionProperties visionProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(visionProperties.getSegmenterUrl())
                .build();
    }


    public List<CellImage> segment(byte[] boardImage) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(boardImage) {
            @Override public String getFilename() { return "board.png"; }
        });

        Map<String, Object> response = restClient.post()
                .uri("/segment")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(Map.class);

        String method = (String) response.get("method");
        log.info("MS2 usó método: {}", method);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) response.get("cells");

        return cells.stream()
                .map(this::mapToCellImage)
                .toList();
    }

    private CellImage mapToCellImage(Map<String, Object> raw) {
        String square = (String) raw.get("square");
        byte[] image = Base64.getDecoder().decode((String) raw.get("image_base64"));
        return new CellImage(square, image);
    }
}