package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * Request Bean of the Cart to Address Mapping
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@Data
@ApiModel(description = "Customer Cart Request Bean")
public class CartAddressMappingRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotEmpty
	private String displayOrderId;

	@NotEmpty
	private Long addressId;

}
