package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

@Data
public class FranchiseSRBean {

    private Integer storeId;

    private Integer whId;

    private String skuCode;

    private Integer sortOrder;

    private Integer cratesRequested;

    private Date date;

    private String slot;

    private UUID orderId;
}
