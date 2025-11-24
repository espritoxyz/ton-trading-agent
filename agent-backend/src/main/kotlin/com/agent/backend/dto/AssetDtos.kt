package com.agent.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AssetCreateRequest(
    @field:NotBlank val address: String,
    @field:NotNull  val amountNano: Long
)

data class AssetUpdateRequest(
    val address: String? = null,
    val amountNano: Long? = null
)

data class AssetResponse(
    val id: Long,
    val address: String,
    val amountNano: Long
)
