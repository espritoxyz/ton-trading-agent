package com.agent.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(StonfiProperties::class)
class StonfiConfig

@ConfigurationProperties(prefix = "stonfi")
data class StonfiProperties(
    var baseUrl: String = "https://api.ston.fi",
    var network: String = "mainnet",
    var refreshIntervalMs: Long = 30_000,
)