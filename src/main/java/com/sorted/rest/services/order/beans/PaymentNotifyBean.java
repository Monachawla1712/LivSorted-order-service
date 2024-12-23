package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class PaymentNotifyBean implements Serializable {

	private static final long serialVersionUID = -8129150877695828452L;

	private String paymentMode;

	private String orderId;

	private String txTime;

	private String referenceId;

	private String type;

	private String txMsg;

	private String signature;

	private String orderAmount;

	private String txStatus;

	private String paymentGateway;
	
	private List<Object> paymentGatewayResponse;

	public static PaymentNotifyBean newInstance() {
		return new PaymentNotifyBean();
	}
}
