package com.example.whatsapp_tally_integration.service.impl;

import com.example.whatsapp_tally_integration.service.TallyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;

@Service
public class TallyServiceImpl implements TallyService {

    @Value("${tally.url}")
    private String tallyUrl;

    @Value("${tally.company-name}")
    private String companyName;

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
                    <STATICVARIABLES>
                      <SVCURRENTCOMPANY>%s</SVCURRENTCOMPANY>
                      <STOCKITEMNAME>%s</STOCKITEMNAME>
                    </STATICVARIABLES>
                  </REQUESTDESC>
                </EXPORTDATA>
              </BODY>
            </ENVELOPE>
            """.formatted(companyName, itemName);

            URL url = new URL(tallyUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(xml.getBytes());
            os.flush();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            String stockQty = extractStockFromXml(response.toString(),itemName);

            if(stockQty.equals("Stock not found")){
                return "Stock not found for item:" + itemName;
            }
            return "Stock for " + itemName + ": " + stockQty;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unable to fetch stock for " + itemName;
    }

    @Override
    public String getAllStock() {

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
                <STATICVARIABLES>
                  <SVCURRENTCOMPANY>%s</SVCURRENTCOMPANY>
                </STATICVARIABLES>
              </REQUESTDESC>
            </EXPORTDATA>
          </BODY>
        </ENVELOPE>
        """.formatted(companyName);

            URL url = new URL(tallyUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(xml.getBytes());
            os.flush();

            BufferedReader br =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            return extractAllStock(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unable to fetch inventory from Tally";
    }

    @Override
    public String updateStock(String itemName , double qty) {

        try {

            String stockText = getStock(itemName);

            if(stockText.contains("Stock not found")) {
                return "Item not found: " + itemName;
            }

            String currentQtyStr = stockText.replaceAll("[^0-9.]", "");
            double currentQty = Double.parseDouble(currentQtyStr);

            double newQty = currentQty + qty;

            if(newQty < 0) {
                return "Not enough stock.\nAvailable: " + currentQty;
            }

            String xml = """
        <ENVELOPE>
          <HEADER>
            <TALLYREQUEST>Import Data</TALLYREQUEST>
          </HEADER>
          <BODY>
            <IMPORTDATA>
              <REQUESTDESC>
                <REPORTNAME>Vouchers</REPORTNAME>
                <STATICVARIABLES>
                  <SVCURRENTCOMPANY>%s</SVCURRENTCOMPANY>
                </STATICVARIABLES>
              </REQUESTDESC>
              <REQUESTDATA>
                <TALLYMESSAGE>
                  <VOUCHER VCHTYPE="Stock Journal" ACTION="Create">
                    <DATE>%s</DATE>

                    <STOCKJOURNALENTRIES.LIST>
                      <STOCKITEMNAME>%s</STOCKITEMNAME>
                      <ACTUALQTY>%s Nos</ACTUALQTY>
                      <BILLEDQTY>%s Nos</BILLEDQTY>
                    </STOCKJOURNALENTRIES.LIST>

                  </VOUCHER>
                </TALLYMESSAGE>
              </REQUESTDATA>
            </IMPORTDATA>
          </BODY>
        </ENVELOPE>
        """.formatted(
                    companyName,
                    java.time.LocalDate.now().toString().replace("-", ""),
                    itemName,
                    qty,
                    qty
            );

            URL url = new URL(tallyUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(xml.getBytes());
            os.flush();

            BufferedReader br =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            while(br.readLine() != null){}

            String action = qty > 0 ? "Added" : "Reduced";

            return """
Stock Updated Successfully

Item : %s
Previous Stock : %s
%s : %s
Current Stock : %s
""".formatted(itemName , currentQty , action , Math.abs(qty) , newQty);

        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return "Failed to update stock";
    }

    private String extractAllStock(String xml) {

        try {

            // Remove invalid XML characters from Tally
            xml = xml.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7F]", "");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList itemNames = doc.getElementsByTagName("DSPDISPNAME");
            NodeList qtyList = doc.getElementsByTagName("DSPCLQTY");

            StringBuilder result = new StringBuilder();

            result.append("Inventory List:\n\n");

            for (int i = 0; i < itemNames.getLength() && i < qtyList.getLength(); i++) {

                String item = itemNames.item(i).getTextContent().trim();

                String qtyText = qtyList.item(i).getTextContent().trim();
                String[] parts = qtyText.split("\\s+");

                result.append(item)
                        .append(" : ")
                        .append(parts[0])
                        .append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unable to parse inventory";
    }

    private String extractStockFromXml(String xml, String itemName) {
        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList itemNames = doc.getElementsByTagName("DSPDISPNAME");
            NodeList qtyList = doc.getElementsByTagName("DSPCLQTY");

            for (int i = 0; i < itemNames.getLength(); i++) {

                String name = itemNames.item(i).getTextContent().trim();

                if (name.equalsIgnoreCase(itemName)) {

                    String qtyText = qtyList.item(i).getTextContent().trim();

                    // Remove item name if appended
                    String[] parts = qtyText.split("\\s+");

                    return parts[0]; // return only quantity
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Stock not found";
    }
}