package com.example.devops;

import org.junit.jupiter.api.Test;
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
class HelloEndpointIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void servesGreetingOverHttp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/hello", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Hello, DevOps!", response.getBody());
        MediaType contentType = response.getHeaders().getContentType();
        assertNotNull(contentType);
        assertTrue(MediaType.TEXT_PLAIN.isCompatibleWith(contentType));
    }

    @Test
    void returnsNotFoundForUnknownRoute() {
        ResponseEntity<String> response = restTemplate.getForEntity("/missing", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
