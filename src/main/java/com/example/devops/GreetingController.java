package com.example.devops;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class GreetingController {

    @GetMapping(value = "/greet", produces = MediaType.APPLICATION_JSON_VALUE)
    public GreetingResponse greet(@RequestParam(value = "name", required = false) String name) {
        String recipient = name == null ? "World" : name.strip();
        if (recipient.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank");
        }
        return new GreetingResponse("Hello, " + recipient + "!");
    }

    public record GreetingResponse(String message) {
    }
}
