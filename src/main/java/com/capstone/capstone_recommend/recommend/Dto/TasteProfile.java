package com.capstone.capstone_recommend.recommend.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TasteProfile {

    @Schema(description = "감칠맛 수치 (1~5)", example = "3")
    private double umami;

    @Schema(description = "단맛 수치 (1~5)", example = "2")
    private double sweet;

    @Schema(description = "신맛 수치 (1~5)", example = "3")
    private double sour;

    @Schema(description = "매운맛 수치 (1~5)", example = "3")
    private double spicy;

    @Schema(description = "쓴맛 수치 (1~5)", example = "1")
    private double bitter;

    @Schema(description = "짠맛 수치 (1~5)", example = "3")
    private double salty;
}
