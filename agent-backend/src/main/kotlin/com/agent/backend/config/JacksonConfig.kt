package com.agent.backend.config

import com.fasterxml.jackson.databind.MapperFeature
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer

@Configuration
class JacksonConfig {
    @Bean
    fun caseInsensitiveEnumsCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
            builder.featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        }
}
