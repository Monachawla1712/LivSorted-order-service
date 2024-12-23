package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class ImsFranchiseOrderRefundBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	@NotNull
	private UUID parentOrderId;

	@NotEmpty
	@Valid
	private List<ImsFranchiseOrderRefundItemBean> refundOrderItems;

	@NotNull
	private Long ticketId;

	@NotNull
	private Long ticketItemId;

	public static ImsFranchiseOrderRefundBean newInstance() {
		return new ImsFranchiseOrderRefundBean();
	}
}