package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
@ApiModel(description = "Update cart address request bean")
public class UpdateOrderAddress implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	private UUID customerId;

	private Long addressId;
}