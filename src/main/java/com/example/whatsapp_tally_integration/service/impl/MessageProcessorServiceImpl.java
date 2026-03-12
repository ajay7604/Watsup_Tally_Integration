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

        if(body.toLowerCase().contains("stock")){

            String item = body.replace("stock","").trim();

            String stock = tallyService.getStock(item);
            System.out.println("Fetching stock from Tally for item: " + item);

            whatsAppService.sendMessage(from , stock);


        }
    }
}








//public class MessageProcessorServiceImpl implements MessageProcessorService {
//
//
//    @Autowired
//    private TallyService tallyService;
//
//    @Autowired
//    private WhatsAppService whatsAppService;
//
//    @Override
//    public void processMessage(Map<String, Object> payload) {
//        try {
//
//            Map entry = (Map) ((List) payload.get("entry")).get(0);
//            Map changes = (Map) ((List) entry.get("changes")).get(0);
//            Map value = (Map) changes.get("value");
//
//            List messages = (List) value.get("messages");
//
//            if (messages == null) return;
//
//            Map message = (Map) messages.get(0);
//
//            String from = (String) message.get("from");
//            Map textObj = (Map) message.get("text");
//
//            String messageText = (String) textObj.get("body");
//
//            System.out.println("Message received: " + messageText);
//
//            if (messageText.startsWith("stock")) {
//
//                String item = messageText.replace("stock", "").trim();
//
//                String stock = tallyService.getStock(item);
//
//                whatsAppService.sendMessage(from, stock);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    }

