package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Bean to be returned Containing Order Item
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Item Response Bean")
@Data
public class PpdOrderItemBean implements Serializable {

    private static final long serialVersionUID = 2102504245219017738L;

    @ApiModelProperty(value = "Order Item sku code", allowEmptyValue = false)
    @NotEmpty
    private String skuCode;

    @ApiModelProperty(value = "Order Item final quantity", allowEmptyValue = false)
    @NotNull
    private Double finalQuantity;

    private Integer finalPieces;
}
