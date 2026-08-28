package com.farmtohome.api.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

  @GetMapping({"/", "/api", "/api/v1"})
  public ResponseEntity<Map<String, Object>> root() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", "UP");
    body.put("service", "Farm To Home API");
    body.put("version", "1.0.0");
    body.put("message", "Backend is online and running successfully.");
    body.put("timestamp", System.currentTimeMillis());
    return ResponseEntity.ok(body);
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    return ResponseEntity.ok(Map.of("status", "UP"));
  }
}
