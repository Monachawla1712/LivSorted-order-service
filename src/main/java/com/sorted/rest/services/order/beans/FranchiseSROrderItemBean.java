package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class FranchiseSROrderItemBean {

    @NotEmpty
    private String skuCode;

    private Double weightPicked;

    private Double weightReceived;

    private Integer cratePicked;

    private Integer crateReceived;

    private Integer deliveryNumber;
}
