package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * Order Slot Response Bean
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@ApiModel(description = "Order Slot Response Bean")
@Data
public class OrderSlotResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private Integer id;

	private String slot;

	private Double fees;

	private String feeMessage;

	private Integer availableCount;

	private Integer remainingCount;

	private Boolean isAvailable = true;

	private Boolean isDefault = false;

	private Integer priority;

	private String eta;

	private OrderSlotMetadata metadata;
}
