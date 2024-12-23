package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderStatus;
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
public class FranchiseOrderListBean implements Serializable {

	private static final long serialVersionUID = -4464420110328768366L;

	@ApiModelProperty(value = " Order Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
	@Null
	private UUID id;

	@ApiModelProperty(value = "Display Order ID", allowEmptyValue = false)
	@NotNull
	private String displayOrderId;

	@ApiModelProperty(value = "Order Status", allowEmptyValue = false)
	@NotNull
	private FranchiseOrderStatus status;

	@ApiModelProperty(value = " submitted At ", readOnly = true, allowEmptyValue = true, notes = "Not Required in input field.")
	@Null
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "IST")
	private Date submittedAt;

	@ApiModelProperty(value = " final Bill amount ", readOnly = true, allowEmptyValue = true, notes = "Not Required in input field.")
	@Null
	private Double finalBillAmount;

	@ApiModelProperty(value = " total Item Count ", readOnly = true, allowEmptyValue = true, notes = "Not Required in input field.")
	@Null
	private Integer itemCount;

	@ApiModelProperty(value = "invoice", allowEmptyValue = true)
	private InvoiceResponseBean invoice;

	@ApiModelProperty(value = "challan Url", allowEmptyValue = true)
	private String challanUrl;

	@ApiModelProperty(value = "parent order id", allowEmptyValue = true)
	private UUID parentOrderId;

	@ApiModelProperty(value = "parent order", allowEmptyValue = true)
	private RefundParentOrderResponseBean parentOrder;

	@ApiModelProperty(value = "refund type", allowEmptyValue = false)
	private String refundType;

	@ApiModelProperty(value = "delivery date ", allowEmptyValue = false)
	@NotNull
	private Date deliveryDate;

	@ApiModelProperty(value = "store id", allowEmptyValue = false)
	private String storeId;

	@ApiModelProperty(value = "slot", allowEmptyValue = false)
	private String slot;

	@ApiModelProperty(value = "Extra Fee Details", allowEmptyValue = true)
	private ExtraFeeDetail extraFeeDetails;

	public static FranchiseOrderListBean newInstance() {
		return new FranchiseOrderListBean();
	}
}