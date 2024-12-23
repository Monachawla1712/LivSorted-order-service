package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.List;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Bean to be Containing Order Products update request
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Pack Request Bean")
@Data
public class OrderPackRequestBean implements Serializable {

	private static final long serialVersionUID = 1L;

	List<OrderItemUpdateBean> orderProducts;

}