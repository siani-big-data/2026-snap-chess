package com.chessdigitizer.backend.infrastructure.adapter.out.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisNodeDTO {

    private String move;
    private String comment;
    private Integer evalCp;
    private List<AnalysisNodeDTO> children = new ArrayList<>();
}