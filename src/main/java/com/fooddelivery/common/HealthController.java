package com.fooddelivery.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Collections.singletonMap("status", "UP"));
    }
}
