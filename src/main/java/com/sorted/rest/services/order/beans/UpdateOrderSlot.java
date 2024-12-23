package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@Data
@ApiModel(description = "Update cart slot request bean")
public class UpdateOrderSlot implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotNull
	private UUID customerId;

	@NotNull
	private Integer slotId;

	@NotNull
	private Integer societyId;
}