package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.RejectionOrderConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@ApiModel(description = "Franchise Order Response Bean")
@Data
public class RejectionOrderListBean implements Serializable {

	private static final long serialVersionUID = -4464420110328768366L;

	@ApiModelProperty(value = " Order Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
	@Null
	private UUID id;

	@ApiModelProperty(value = "Display Order ID", allowEmptyValue = false)
	@NotNull
	private String displayOrderId;

	@ApiModelProperty(value = "Store ID", allowEmptyValue = false)
	@NotNull
	private String storeId;

	@ApiModelProperty(value = "Order Status", allowEmptyValue = false)
	@NotNull
	private RejectionOrderConstants.RejectionOrderStatus status;

	@ApiModelProperty(value = " submitted At ", readOnly = true, allowEmptyValue = true, notes = "Not Required in input field.")
	@Null
	private Date submittedAt;

	@ApiModelProperty(value = " final Bill amount ", readOnly = true, allowEmptyValue = true, notes = "Not Required in input field.")
	@Null
	private Double finalBillAmount;

	@ApiModelProperty(value = " total Item Count ", readOnly = true, allowEmptyValue = true, notes = "Not Required in input field.")
	@Null
	private Integer itemCount;

	@ApiModelProperty(value = "order type", allowEmptyValue = true)
	private RejectionOrderConstants.RejectionOrderType orderType;

	public static RejectionOrderListBean newInstance() {
		return new RejectionOrderListBean();
	}
}