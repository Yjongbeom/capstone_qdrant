package com.capstone.capstone_recommend.recommend.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("메뉴 추천 API")
                        .version("1.0")
                        .description("사용자 선호도 기반 메뉴 추천 API"));
    }
}