package com.sorted.rest.services.order.beans;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreSkuOrderCount {
    private String storeId;
    private String skuCode;
    private Long orderedCrateQty;
}
