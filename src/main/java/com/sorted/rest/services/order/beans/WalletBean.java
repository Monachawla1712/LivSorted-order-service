package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants.WalletStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class WalletBean implements Serializable {

	private String entityId;

	private double amount;

	private double loyaltyCoins;

	private Double creditLimit;

	private WalletStatus status;

	private Double lastOrderOutstanding;

	private UserWalletMetadata metadata;

	public static WalletBean newInstance() {
		return new WalletBean();
	}
}
