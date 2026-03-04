package com.agent.backend

import com.agent.backend.dto.AuthErrorCode
import com.agent.backend.service.AuthService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AuthErrorMappingTest {

    @ParameterizedTest(name = "Keycloak \"{0}\" (error={1}) -> {2}")
    @CsvSource(
        "Invalid user credentials, invalid_grant, INVALID_CREDENTIALS",
        "Account is not fully set up, invalid_grant, ACCOUNT_NOT_VERIFIED",
        "Account disabled, invalid_grant, ACCOUNT_DISABLED",
        "User is disabled, invalid_grant, ACCOUNT_DISABLED",
        "Account temporarily locked, invalid_grant, ACCOUNT_LOCKED",
        "User account is temporarily disabled, invalid_grant, ACCOUNT_LOCKED",
        "Some unknown error, invalid_grant, INVALID_CREDENTIALS",
        "Invalid credentials, unknown_code, INVALID_CREDENTIALS",
        "Something random, invalid_grant, INVALID_CREDENTIALS"
    )
    fun mapKeycloakError_returnsCorrectCode(
        errorDescription: String,
        errorCode: String,
        expectedCode: AuthErrorCode
    ) {
        val result = AuthService.mapKeycloakError(errorDescription, errorCode)
        assertEquals(expectedCode, result)
    }

    @Test
    fun mapKeycloakError_withNullCode_fallsBackCorrectly() {
        val result = AuthService.mapKeycloakError("Invalid credentials", null)
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, result)
    }

    @Test
    fun humanReadableMessage_returnsNonBlankForEveryCode() {
        AuthErrorCode.entries.forEach { code ->
            val message = AuthService.humanReadableMessage(code)
            assert(message.isNotBlank()) { "Message for $code should not be blank" }
        }
    }

    @Test
    fun humanReadableMessage_returnsDistinctMessages() {
        val messages = AuthErrorCode.entries.map { AuthService.humanReadableMessage(it) }.toSet()
        assertEquals(AuthErrorCode.entries.size, messages.size, "Each error code should have a unique message")
    }
}
