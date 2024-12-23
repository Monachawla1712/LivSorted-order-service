package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.sorted.rest.services.order.constants.OrderConstants.OrderStatus;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Bean to be Containing Order status update request
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order status Update Request Bean")
@Data
public class OrderStatusUpdateRequestBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5407078522677366211L;

	@NotNull
	private OrderStatus status;
}
