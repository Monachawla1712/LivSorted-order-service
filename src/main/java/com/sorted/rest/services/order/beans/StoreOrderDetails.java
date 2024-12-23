package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.List;

@Data
public class StoreOrderDetails {
    private Integer storeId;

    private List<SkuDetails> skuDetails;

    public static StoreOrderDetails newInstance() {
        return new StoreOrderDetails();
    }
}
