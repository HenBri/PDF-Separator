package com.chromasplit.service;

public interface PriceStorage {
    double loadBwPrice();
    double loadColorPrice();
    double loadColorBasePrice();
    double loadColorMediumPrice();
    double loadColorHighPrice();
    void saveBwPrice(double price);
    void saveColorPrice(double price);
    void saveColorBasePrice(double price);
    void saveColorMediumPrice(double price);
    void saveColorHighPrice(double price);
}
