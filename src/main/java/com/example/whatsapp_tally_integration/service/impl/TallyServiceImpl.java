package com.example.whatsapp_tally_integration.service.impl;

import com.example.whatsapp_tally_integration.service.TallyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class TallyServiceImpl implements TallyService {

    @Value("${tally.url}")
    private String tallyUrl;

    @Override
    public String getStock(String itemName) {

        try {

            String xml = """
            <ENVELOPE>
              <HEADER>
                <TALLYREQUEST>Export Data</TALLYREQUEST>
              </HEADER>
              <BODY>
                <EXPORTDATA>
                  <REQUESTDESC>
                    <REPORTNAME>Stock Summary</REPORTNAME>
                  </REQUESTDESC>
                </EXPORTDATA>
              </BODY>
            </ENVELOPE>
            """;

            URL url = new URL(tallyUrl);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(xml.getBytes());
            os.flush();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));

            String line;
            StringBuilder response = new StringBuilder();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            System.out.println("Tally Response: " + response.toString());

            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unable to fetch stock";
    }
}
