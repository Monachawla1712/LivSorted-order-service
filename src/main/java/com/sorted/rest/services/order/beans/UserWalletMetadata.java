package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserWalletMetadata implements Serializable {

	private static final long serialVersionUID = 8376574842543219203L;

	private Double minOutstanding;

	public static UserWalletMetadata newInstance() {
		return new UserWalletMetadata();
	}
}
