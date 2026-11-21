package com.example.lpsx.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** CloudBase container health probe. This endpoint intentionally lives outside /api JWT protection. */
@RestController
public class HealthController {

    @GetMapping("/__tcb_probe__")
    public Map<String, String> cloudBaseProbe() {
        return Map.of("status", "ok");
    }
}
