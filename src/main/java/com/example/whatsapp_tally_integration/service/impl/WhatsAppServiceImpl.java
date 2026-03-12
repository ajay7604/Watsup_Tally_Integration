package com.example.whatsapp_tally_integration.service.impl;

import com.example.whatsapp_tally_integration.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    @Value("${whatsapp.token}")
    private String token;

    @Value("${whatsapp.phone-id}")
    private String phoneId;

    @Override
    public void sendMessage(String to, String message) {
        try {
            URL url = new URL("https://graph.facebook.com/v18.0/" + phoneId + "/messages");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String body = """
            {
              "messaging_product": "whatsapp",
              "to": "%s",
              "type": "text",
              "text": {
                "body": "%s"
              }
            }
            """.formatted(to, message);

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes());
            os.flush();

            // Print response for debugging
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while((line = br.readLine()) != null){
                System.out.println("WhatsApp API Response: " + line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}