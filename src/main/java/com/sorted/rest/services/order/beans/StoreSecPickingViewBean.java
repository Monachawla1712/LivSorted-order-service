package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class StoreSecPickingViewBean implements Serializable {

	private static final long serialVersionUID = 493517626137997750L;

	private Long id;

	private Integer storeId;

	private String skuCode;

	public static StoreSecPickingViewBean newInstance() {
		return new StoreSecPickingViewBean();
	}
}