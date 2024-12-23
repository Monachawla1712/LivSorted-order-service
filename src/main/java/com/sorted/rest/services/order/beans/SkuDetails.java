package com.sorted.rest.services.order.beans;

import lombok.Data;

@Data
public class SkuDetails {
    private String skuCode;
    private Integer orderedCrateQty;

    public static SkuDetails newInstance() {
        return new SkuDetails();
    }
}
