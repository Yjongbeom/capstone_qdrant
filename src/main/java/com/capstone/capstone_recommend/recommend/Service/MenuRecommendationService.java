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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuRecommendationService {

    private final QdrantClient qdrantClient;
    private static final String COLLECTION_NAME = "menu_recommendations";

    public List<MenuRecommendation> recommendMenus(UserPreferenceRequest request) {
        try {
            // 1. 사용자 선호도를 0~1 범위로 변환
            double[] userVector = transformUserPreference(request);

            // 2. 벡터 정규화
            double[] normalizedVector = normalizeVector(userVector);

            // 3. float 리스트로 변환
            List<Float> floatVector = Arrays.stream(normalizedVector)
                    .mapToObj(d -> (float) d)
                    .collect(Collectors.toList());

            // 4. Qdrant 검색 요청 생성
            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName(COLLECTION_NAME)
                    .addAllVector(floatVector)
                    .setLimit(5)
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build()) // ✅ 추가
                    .build();

            System.out.println(searchPoints);

            // 5. Qdrant에서 벡터 검색
            List<Points.ScoredPoint> results = qdrantClient.searchAsync(searchPoints).get();

            System.out.println(results);

            // 6. 결과 매핑
            return results.stream()
                    .map(this::mapToMenuRecommendation)
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Qdrant 벡터 검색 실패", e);
        }
    }

    private double[] transformUserPreference(UserPreferenceRequest request) {
        return new double[]{
                (request.getUmami() - 1) / 4.0,
                (request.getSweet() - 1) / 4.0,
                (request.getSour() - 1) / 4.0,
                (request.getSpicy() - 1) / 4.0,
                (request.getBitter() - 1) / 4.0,
                (request.getSalty() - 1) / 4.0
        };
    }

    private double[] normalizeVector(double[] vector) {
        double norm = Math.sqrt(Arrays.stream(vector).map(v -> v * v).sum());
        return norm > 0 ? Arrays.stream(vector).map(v -> v / norm).toArray() : vector;
    }

    private MenuRecommendation mapToMenuRecommendation(Points.ScoredPoint point) {
        Map<String, JsonWithInt.Value> payload = point.getPayloadMap();

        MenuRecommendation recommendation = new MenuRecommendation();
        recommendation.setRestaurant(payload.get("restaurant").getStringValue());
        recommendation.setMenu(payload.get("menu").getStringValue());
        recommendation.setSimilarityScore(point.getScore());
        recommendation.setReviewCount((int) payload.get("total_count").getIntegerValue());

        // 1. taste_profile 구조체 추출
        JsonWithInt.Value tasteProfileValue = payload.get("taste_profile");
        JsonWithInt.Struct tasteProfileStruct = tasteProfileValue.getStructValue();
        Map<String, JsonWithInt.Value> tasteFields = tasteProfileStruct.getFieldsMap();

        // 2. 내부 필드에서 값 추출
        TasteProfile profile = new TasteProfile();
        profile.setUmami(tasteFields.get("umami").getDoubleValue() * 4 + 1);
        profile.setSweet(tasteFields.get("sweet").getDoubleValue() * 4 + 1);
        profile.setSour(tasteFields.get("sour").getDoubleValue() * 4 + 1);
        profile.setSpicy(tasteFields.get("spicy").getDoubleValue() * 4 + 1);
        profile.setBitter(tasteFields.get("bitter").getDoubleValue() * 4 + 1);
        profile.setSalty(tasteFields.get("salty").getDoubleValue() * 4 + 1);

        recommendation.setTasteProfile(profile);
        return recommendation;
    }
}
