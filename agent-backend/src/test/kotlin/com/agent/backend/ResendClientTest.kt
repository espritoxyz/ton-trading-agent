package com.agent.backend

import com.agent.backend.email.EmailSendException
import com.agent.backend.email.ResendClient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

class ResendClientTest {

    private val apiUrl = "https://api.resend.test"
    private val apiKey = "re_test_key"

    private lateinit var mockServer: MockRestServiceServer
    private lateinit var client: ResendClient

    @BeforeEach
    fun setup() {
        val restTemplate = RestTemplate()
        mockServer = MockRestServiceServer.createServer(restTemplate)
        val restClient = RestClient.builder(restTemplate).build()

        client = ResendClient(
            apiKey = apiKey,
            apiUrl = apiUrl,
            maxRetryAttempts = 3,
            retryDelayMs = 0L,
            restClient = restClient
        )
    }

    @Test
    fun `sendEmail should succeed on first attempt`() {
        mockServer.expect(requestTo("$apiUrl/emails"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer $apiKey"))
            .andRespond(withSuccess("""{"id":"email-123"}""", MediaType.APPLICATION_JSON))

        val result = client.sendEmail("from@test.com", "to@test.com", "Subject", "<p>Body</p>")

        assertTrue(result)
        mockServer.verify()
    }

    @Test
    fun `sendEmail should retry on 5xx and succeed on third attempt`() {
        // First two attempts fail with 500, third succeeds
        repeat(2) {
            mockServer.expect(requestTo("$apiUrl/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError())
        }
        mockServer.expect(requestTo("$apiUrl/emails"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""{"id":"email-456"}""", MediaType.APPLICATION_JSON))

        val result = client.sendEmail("from@test.com", "to@test.com", "Subject", "<p>Body</p>")

        assertTrue(result)
        mockServer.verify()
    }

    @Test
    fun `sendEmail should throw EmailSendException after exhausting all retries on 5xx`() {
        mockServer.expect(ExpectedCount.times(3), requestTo("$apiUrl/emails"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError())

        assertThrows<EmailSendException> {
            client.sendEmail("from@test.com", "to@test.com", "Subject", "<p>Body</p>")
        }

        mockServer.verify()
    }

    @Test
    fun `sendEmail should not retry on 4xx client error and throw immediately`() {
        // Expect exactly ONE request (no retry)
        mockServer.expect(ExpectedCount.once(), requestTo("$apiUrl/emails"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.UNAUTHORIZED)
                    .body("""{"message":"Invalid API key"}""")
                    .contentType(MediaType.APPLICATION_JSON)
            )

        val ex = assertThrows<EmailSendException> {
            client.sendEmail("from@test.com", "to@test.com", "Subject", "<p>Body</p>")
        }

        assertTrue(ex.message!!.contains("401") || ex.message!!.contains("client error"))
        mockServer.verify()
    }

    @Test
    fun `sendEmail should not retry on 422 unprocessable entity`() {
        mockServer.expect(ExpectedCount.once(), requestTo("$apiUrl/emails"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body("""{"message":"Invalid recipient"}""")
                    .contentType(MediaType.APPLICATION_JSON)
            )

        assertThrows<EmailSendException> {
            client.sendEmail("from@test.com", "bad-email", "Subject", "<p>Body</p>")
        }

        mockServer.verify()
    }

    @Test
    fun `sendEmail with single retry should attempt exactly twice before failing`() {
        val singleRetryClient = ResendClient(
            apiKey = apiKey,
            apiUrl = apiUrl,
            maxRetryAttempts = 1,
            retryDelayMs = 0L,
            restClient = RestClient.builder(run {
                val tmpl = RestTemplate()
                MockRestServiceServer.createServer(tmpl).also { server ->
                    server.expect(ExpectedCount.once(), requestTo("$apiUrl/emails"))
                        .andRespond(withServerError())
                }
                tmpl
            }).build()
        )

        assertThrows<EmailSendException> {
            singleRetryClient.sendEmail("from@test.com", "to@test.com", "Subject", "<p>Body</p>")
        }
    }
}
