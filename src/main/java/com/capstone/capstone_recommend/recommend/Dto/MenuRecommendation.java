package com.capstone.capstone_recommend.recommend.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MenuRecommendation {
    @Schema(description = "식당 이름", example = "맛있는 식당")
    private String restaurant;

    @Schema(description = "메뉴 이름", example = "불고기 정식")
    private String menu;

    @Schema(description = "유사도 점수", example = "0.95")
    private float similarityScore;

    @Schema(description = "리뷰 수", example = "42")
    private int reviewCount;

    @Schema(description = "맛 프로필")
    private TasteProfile tasteProfile;
}
