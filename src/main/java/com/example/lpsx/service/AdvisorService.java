package com.example.lpsx.service;

import java.util.Map;

public interface AdvisorService {
    Map<String, Object> welcome();
    Map<String, Object> reply(String message, Map<String, Object> context);
}
