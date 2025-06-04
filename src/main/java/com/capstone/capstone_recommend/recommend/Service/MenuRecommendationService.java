package com.capstone.capstone_recommend.recommend.Service;

import com.capstone.capstone_recommend.recommend.Dto.MenuRecommendation;
import com.capstone.capstone_recommend.recommend.Dto.TasteProfile;
import com.capstone.capstone_recommend.recommend.Dto.UserPreferenceRequest;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.JsonWithInt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuRecommendationService {

    private final QdrantClient qdrantClient;
    private static final String COLLECTION_NAME = "menu_recommendation";
    private static final int CANDIDATE_SIZE = 15; // 후보 메뉴 수 (상위 15개 후보를 뽑아 가중치 적용)

    public List<MenuRecommendation> recommendMenus(UserPreferenceRequest request) {
        try {
            // 1. 사용자 선호도 변환 및 정규화
            List<Float> floatVector = getNormalizedUserVector(request);

            // 2. 후보 메뉴 검색
            List<Points.ScoredPoint> candidates = searchCandidateMenus(floatVector);

            // 3. 가중치 적용 및 정렬
            List<MenuRecommendation> recommendations = processCandidates(candidates);

            // 4. 상위 5개 반환
            return recommendations.stream().limit(5).collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Qdrant 벡터 검색 실패", e);
        }
    }

    // 사용자 벡터 생성 메소드
    private List<Float> getNormalizedUserVector(UserPreferenceRequest request) {
        double[] userVector = {
                (request.getUmami() - 1) / 4.0,
                (request.getSweet() - 1) / 4.0,
                (request.getSour() - 1) / 4.0,
                (request.getSpicy() - 1) / 4.0,
                (request.getBitter() - 1) / 4.0,
                (request.getSalty() - 1) / 4.0
        };

        double norm = Math.sqrt(Arrays.stream(userVector).map(v -> v * v).sum());
        double[] normalized = norm > 0 ?
                Arrays.stream(userVector).map(v -> v / norm).toArray() :
                userVector;

        return Arrays.stream(normalized)
                .mapToObj(v -> (float) v)
                .collect(Collectors.toList());
    }

    // 후보 메뉴 검색
    private List<Points.ScoredPoint> searchCandidateMenus(List<Float> vector)
            throws InterruptedException, ExecutionException {
        SearchPoints searchPoints = SearchPoints.newBuilder()
                .setCollectionName(COLLECTION_NAME)
                .addAllVector(vector)
                .setLimit(CANDIDATE_SIZE)
                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                .build();

        return qdrantClient.searchAsync(searchPoints).get();
    }

    // 가중치 처리 및 정렬
    private List<MenuRecommendation> processCandidates(List<Points.ScoredPoint> candidates) {
        // 임시 저장소: [가중치 점수, 추천 객체]
        List<Map.Entry<Double, MenuRecommendation>> tempList = new ArrayList<>();

        for (Points.ScoredPoint point : candidates) {
            MenuRecommendation rec = mapToMenuRecommendation(point);
            double weight = calculateWeight(rec.getReviewCount(), point.getScore());
            tempList.add(new AbstractMap.SimpleEntry<>(weight, rec));
        }

        // 가중치 기준 내림차순 정렬
        tempList.sort((a, b) -> Double.compare(b.getKey(), a.getKey()));

        // 최종 추천 목록 반환 (가중치 제외)
        return tempList.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    // 가중치 계산: 유사도 * log(리뷰 수 + 1)
    private double calculateWeight(int reviewCount, float similarity) {
        return similarity * Math.log1p(reviewCount);
    }

    // Qdrant 결과 -> DTO 매핑 (소수점 첫째 자리로 반올림)
    private MenuRecommendation mapToMenuRecommendation(Points.ScoredPoint point) {
        Map<String, JsonWithInt.Value> payload = point.getPayloadMap();

        MenuRecommendation rec = new MenuRecommendation();
        rec.setRestaurant(payload.get("restaurant").getStringValue());
        rec.setMenu(payload.get("menu").getStringValue());
        rec.setSimilarityScore(point.getScore());
        rec.setReviewCount((int) payload.get("total_count").getIntegerValue());

        // taste_profile 매핑
        JsonWithInt.Value tasteValue = payload.get("taste_profile");
        JsonWithInt.Struct tasteStruct = tasteValue.getStructValue();
        Map<String, JsonWithInt.Value> tasteFields = tasteStruct.getFieldsMap();

        TasteProfile profile = new TasteProfile();
        profile.setUmami(roundToOneDecimal(tasteFields.get("umami").getDoubleValue()));
        profile.setSweet(roundToOneDecimal(tasteFields.get("sweet").getDoubleValue()));
        profile.setSour(roundToOneDecimal(tasteFields.get("sour").getDoubleValue()));
        profile.setSpicy(roundToOneDecimal(tasteFields.get("spicy").getDoubleValue()));
        profile.setBitter(roundToOneDecimal(tasteFields.get("bitter").getDoubleValue()));
        profile.setSalty(roundToOneDecimal(tasteFields.get("salty").getDoubleValue()));

        rec.setTasteProfile(profile);
        return rec;
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10) / 10.0;
    }
}