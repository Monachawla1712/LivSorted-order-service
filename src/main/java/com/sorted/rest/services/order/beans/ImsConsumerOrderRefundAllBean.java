package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@Data
public class ImsConsumerOrderRefundAllBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	@NotNull
	private UUID parentOrderId;

	@NotNull
	private Long ticketId;

	@NotNull
	private Long ticketItemId;

	public static ImsConsumerOrderRefundAllBean newInstance() {
		return new ImsConsumerOrderRefundAllBean();
	}
}