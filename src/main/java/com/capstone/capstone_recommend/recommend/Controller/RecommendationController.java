package com.capstone.capstone_recommend.recommend.Controller;

import com.capstone.capstone_recommend.recommend.Dto.MenuRecommendation;
import com.capstone.capstone_recommend.recommend.Dto.UserPreferenceRequest;
import com.capstone.capstone_recommend.recommend.Service.MenuRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "메뉴 추천 API", description = "사용자 선호도 기반 메뉴 추천 기능")
public class RecommendationController {

    private final MenuRecommendationService menuRecommendationService;

    @PostMapping("/")
    @Operation(summary = "메뉴 추천 요청", description = "사용자의 맛 선호도를 입력받아 추천 메뉴 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "추천 메뉴 목록 반환 성공",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = MenuRecommendation.class))))
    public ResponseEntity<List<MenuRecommendation>> getRecommendations(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용자 맛 선호도 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserPreferenceRequest.class))
            )
            UserPreferenceRequest request
    ) {
        List<MenuRecommendation> recommendations = menuRecommendationService.recommendMenus(request);
        return ResponseEntity.ok(recommendations);
    }

}