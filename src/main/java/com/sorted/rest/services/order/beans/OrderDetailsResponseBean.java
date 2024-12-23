package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean to be returned with Contains Orders with complete details
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Order Details Response Bean")
@Data
public class OrderDetailsResponseBean extends OrderResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "Refund Orders List", allowEmptyValue = true)
	private List<OrderResponseBean> refundOrders = new ArrayList<>();

	@ApiModelProperty(value = "Order Ticket Information", allowEmptyValue = true)
	private TicketBean ticketInfo;

	@ApiModelProperty(value = "order page action", allowEmptyValue = true)
	private OrderConstants.OrderPageAction pageAction = OrderConstants.OrderPageAction.NONE;

	public static OrderDetailsResponseBean newInstance() {
		return new OrderDetailsResponseBean();
	}

}
