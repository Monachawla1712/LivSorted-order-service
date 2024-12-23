package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@ApiModel(description = "Franchise Order Response Bean extending the List Bean")
@Data
public class RefundParentOrderResponseBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = " Order Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
	@Null
	private UUID id;

	@ApiModelProperty(value = "Display Order ID", allowEmptyValue = false)
	@NotNull
	private String displayOrderId;

	@ApiModelProperty(value = "Order Status", allowEmptyValue = false)
	@NotNull
	private FranchiseOrderConstants.FranchiseOrderStatus status;

	@ApiModelProperty(value = " submitted At ", readOnly = true, allowEmptyValue = true, notes = "Not Required in input field.")
	@Null
	private Date submittedAt;

	@ApiModelProperty(value = " final Bill amount ", readOnly = true, allowEmptyValue = true, notes = "Not Required in input field.")
	@Null
	private Double finalBillAmount;

	@ApiModelProperty(value = " total Item Count ", readOnly = true, allowEmptyValue = true, notes = "Not Required in input field.")
	@Null
	private Integer itemCount;

	@ApiModelProperty(value = "delivery date ", allowEmptyValue = false)
	@NotNull
	private Date deliveryDate;

	@ApiModelProperty(value = "Is srp store", allowEmptyValue = false)
	private Integer isSrpStore;

	@ApiModelProperty(value = "store id", allowEmptyValue = false)
	private String storeId;

	@ApiModelProperty(value = "slot", allowEmptyValue = false)
	private String slot;

	public static RefundParentOrderResponseBean newInstance() {
		return new RefundParentOrderResponseBean();
	}
}
