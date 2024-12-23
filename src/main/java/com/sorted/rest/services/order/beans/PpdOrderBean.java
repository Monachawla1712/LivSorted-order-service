package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Lithos Order Request Bean
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Ppd Order Request Bean")
@Data
public class PpdOrderBean implements Serializable {

    private static final long serialVersionUID = 4143679869385600722L;

    @ApiModelProperty(value = " Order Id")
    private UUID id;

    @ApiModelProperty(value = "Order Status", allowEmptyValue = false)
    @NotNull
    private OrderStatus status;

    @ApiModelProperty(value = "Order Items", allowEmptyValue = false)
    @NotNull
    @Valid
    private List<PpdOrderItemBean> orderItems;

    public static PpdOrderBean newInstance() {
        return new PpdOrderBean();
    }

}