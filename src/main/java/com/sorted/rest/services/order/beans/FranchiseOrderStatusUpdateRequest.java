package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderStatus;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.SRType;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@ApiModel(description = "Franchise Store Order Status Update")
public class FranchiseOrderStatusUpdateRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotEmpty
	private UUID orderId;

	private SRType type;

	@NotEmpty
	private FranchiseOrderStatus status;

	private List<FranchiseSROrderItemBean> orderItems;

}
