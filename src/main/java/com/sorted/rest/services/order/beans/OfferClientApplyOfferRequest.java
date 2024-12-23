package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Offer Client Request Object
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@Data
@ApiModel(description = "Apply Offer Request Bean")
public class OfferClientApplyOfferRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	public String code;

	public OfferClientOrderBean order;
}
