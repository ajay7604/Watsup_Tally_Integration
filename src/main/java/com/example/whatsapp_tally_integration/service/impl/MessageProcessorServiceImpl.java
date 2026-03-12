package com.example.whatsapp_tally_integration.service.impl;

import com.example.whatsapp_tally_integration.service.MessageProcessorService;
import com.example.whatsapp_tally_integration.service.TallyService;
import com.example.whatsapp_tally_integration.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MessageProcessorServiceImpl implements MessageProcessorService {

    @Autowired
    private TallyService tallyService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Override
    public void processMessage(Map<String, Object> payload) {

        System.out.println("Incoming Webhook Payload: " + payload);

        Map entry = (Map)((List)payload.get("entry")).get(0);
        Map changes = (Map)((List)entry.get("changes")).get(0);
        Map value = (Map)changes.get("value");

        List messages = (List)value.get("messages");
        if(messages == null) return;

        Map message = (Map)messages.get(0);
        String from = (String)message.get("from");
        Map text = (Map)message.get("text");
        String body = (String)text.get("body");

        System.out.println("Message received: " + body);



        //  HANDLE "stock all"
        if(body.equalsIgnoreCase("stock all")){

            String stock = tallyService.getAllStock();

            System.out.println("Stock ALL response from Tally: " + stock);


            if(stock == null || stock.isEmpty()){
                stock = "Unable to fetch inventory from Tally";
            }

            whatsAppService.sendMessage(from, stock);
            return;
        }
        //  HANDLE "stock rice" etc
        if(body.toLowerCase().startsWith("stock")){

            String item = body.replaceFirst("(?i)stock", "").trim();

            if(item.isEmpty()){
                whatsAppService.sendMessage(from,
                        "Please specify an item name, e.g., 'stock rice'");
                return;
            }

            String stock = tallyService.getStock(item);

            if(stock == null || stock.isEmpty()){
                stock = "Unable to fetch stock from Tally";
            }

            whatsAppService.sendMessage(from , stock);

        }
    }
}