package com.example.whatsapp_tally_integration.service;

public interface TallyService {
    String getStock(String item);

    String getAllStock();

    String updateStock(String item, double qty);
}
