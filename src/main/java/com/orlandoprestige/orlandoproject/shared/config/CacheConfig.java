package com.orlandoprestige.orlandoproject.shared.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "catalogPages",
                "productById",
                "productBySku",
                "productLists",
                "inventoryItems",
                "inventoryItemById",
                "inventoryItemByProductId",
                "inventoryMovements",
                "inventoryDailySummary",
                "warehouseStocks",
                "warehouseDefinitions",
                "purchaseOrderLists",
                "purchaseOrderById",
                "warehouseSalesSummary",
                "warehouseSalesDetails"
        );
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}