package com.example.devops;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GreetingEndpointIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void usesDefaultNameWhenParameterIsAbsent() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/greet", JsonNode.class);

        assertGreeting(response, "Hello, World!");
    }

    @ParameterizedTest
    @CsvSource({
            "Alice, 'Hello, Alice!'",
            "'  Alice  ', 'Hello, Alice!'",
            "小明, 'Hello, 小明!'"
    })
    void greetsNamedRecipient(String name, String expectedMessage) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/greet?name={name}", JsonNode.class, name);

        assertGreeting(response, expectedMessage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\u3000"})
    void rejectsBlankName(String name) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/greet?name={name}", String.class, name);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private void assertGreeting(ResponseEntity<JsonNode> response, String expectedMessage) {
        assertEquals(HttpStatus.OK, response.getStatusCode());
        MediaType contentType = response.getHeaders().getContentType();
        assertNotNull(contentType);
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(contentType));
        assertNotNull(response.getBody());
        assertEquals(expectedMessage, response.getBody().path("message").asText());
    }
}
