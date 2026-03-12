package com.example.whatsapp_tally_integration.controller;

import com.example.whatsapp_tally_integration.service.MessageProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @Autowired
    private MessageProcessorService messageProcessorService;

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    @GetMapping
    public String verifyWebhook(
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token) {

        if (verifyToken.equals(token)) {
            return challenge;
        }

        return "Invalid token";
    }

    @PostMapping
    public ResponseEntity<?> receiveMessage(@RequestBody Map<String, Object> payload) {

        System.out.println("Incoming Webhook Payload: " + payload);

        messageProcessorService.processMessage(payload);

        return ResponseEntity.ok().build();
    }
}