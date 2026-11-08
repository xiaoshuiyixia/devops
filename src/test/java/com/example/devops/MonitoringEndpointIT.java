package com.example.devops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.server.port=0")
@AutoConfigureObservability
class MonitoringEndpointIT {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalManagementPort
    private int managementPort;

    private String managementUrl(String path) {
        return "http://127.0.0.1:" + managementPort + "/actuator/" + path;
    }

    @Test
    void exposesHealthWithoutDetails() {
        var response = restTemplate.getForEntity(managementUrl("health"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\":\"UP\"}", response.getBody());
    }

    @Test
    void exportsJvmCpuAndCompletedHttpRequests() {
        restTemplate.getForEntity("/hello", String.class);
        var response = restTemplate.getForEntity(managementUrl("prometheus"), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("jvm_memory_used_bytes{"));
        assertTrue(body.contains("process_cpu_usage{"));
        assertTrue(body.contains("http_server_requests_seconds_count{"));
        assertTrue(body.contains("uri=\"/hello\""));
        assertTrue(body.contains("application=\"devops-learning\""));
    }

    @Test
    void keepsActuatorOffBusinessPortAndOtherEndpointsUnexposed() {
        assertEquals(HttpStatus.NOT_FOUND,
                restTemplate.getForEntity("/actuator/prometheus", String.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                restTemplate.getForEntity("/actuator/health", String.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                restTemplate.getForEntity(managementUrl("env"), String.class).getStatusCode());
    }
}
