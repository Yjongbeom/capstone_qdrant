package com.capstone.capstone_recommend.recommend.Config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {
    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.api-key}")
    private String qdrantApiKey;


    @Value("${qdrant.port}")
    private int qdrantPort;

    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(QdrantGrpcClient.newBuilder(
                qdrantHost,
                qdrantPort,
                true
        ).withApiKey(qdrantApiKey).build());
    }
}
