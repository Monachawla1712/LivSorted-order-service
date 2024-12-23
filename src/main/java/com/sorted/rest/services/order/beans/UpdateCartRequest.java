package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.validation.constraints.Min;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Request Bean of the Customer cart related information
 *
 * @author mohit
 * @version $Id: $Id
 */
@Data
@ApiModel(description = "Customer Cart Request Bean")
public class UpdateCartRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	private String channel;

	private UUID customerId;

	private Date deliveryDate;

	private String notes;

	@Min(value = 1)
	private Long addressId;

	@Min(value = 1)
	private Integer slotId;

	@Min(value = 1)
	private Integer societyId;

	private Boolean isAutoCheckoutEnabled;

}
