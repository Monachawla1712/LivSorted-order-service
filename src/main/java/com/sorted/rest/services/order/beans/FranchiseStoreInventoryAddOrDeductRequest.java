package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class FranchiseStoreInventoryAddOrDeductRequest implements Serializable {

    private static final long serialVersionUID = 8949302740350182245L;

    private Integer quantity;

    private Double moq;

    private String storeId;

    public static FranchiseStoreInventoryAddOrDeductRequest newInstance() {
        return new FranchiseStoreInventoryAddOrDeductRequest();
    }

}
