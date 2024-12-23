package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class WalletAddOrDeductBean implements Serializable {

	private Double amount;

	private String txnType;

	private String txnDetail;

	private String remarks;

	private String walletType = null;

	private Double holdAmount = null;
}
