package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * Offer Client Request Object
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@Data
@ApiModel(description = "Apply Franchise Offer Request Bean")
public class OfferClientApplyFranchiseOfferRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	public String code;

	public String storeId;

	public String orderId;

	public OfferClientFranchiseOrderBean order;
}
