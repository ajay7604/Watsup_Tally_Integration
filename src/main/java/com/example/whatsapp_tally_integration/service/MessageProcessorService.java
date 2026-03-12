package com.example.whatsapp_tally_integration.service;

import java.util.Map;

public interface MessageProcessorService {
    void processMessage(Map<String, Object> payload);
}
