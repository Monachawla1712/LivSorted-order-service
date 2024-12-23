package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * Order Slot Metadata
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Slot Metadata")
@Data
public class OrderSlotMetadata implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private Double freeDeliveryAbove = 0d;
}
