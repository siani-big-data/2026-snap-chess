package com.chessdigitizer.backend.infrastructure.adapter.in;

import com.chessdigitizer.backend.domain.port.in.GetPageRenderUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@Slf4j
public class PdfController {

    GetPageRenderUseCase getPageRenderUseCase;

    public PdfController(GetPageRenderUseCase getPageRenderUseCase) {
        this.getPageRenderUseCase = getPageRenderUseCase;
    }


    @GetMapping("/{id}/pages/{pageNumber}")
    public ResponseEntity<byte[]> getPage(@PathVariable UUID id, @PathVariable int pageNumber) {
        byte[] renderedPage =getPageRenderUseCase.renderPageDefault(id, pageNumber);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return ResponseEntity.ok().headers(headers).body(renderedPage);
    }
}
