package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
@Data
public class StoreResponse implements Serializable {

	private static final long serialVersionUID = 493517626137997750L;

	private Integer id;

	private String extStoreId;

	private String name;

	private String address;

	private String storeType;

	private Integer isSrpStore;

	private StoreMetadata metadata;

	@Data
	public static class StoreMetadata implements Serializable {

		private String gstNumber;

		private String panNumber;
	}
}
