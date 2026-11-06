package com.example.devops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloControllerTest {

    @Test
    void returnsGreeting() {
        assertEquals("Hello, DevOps!", new HelloController().hello());
    }
}
