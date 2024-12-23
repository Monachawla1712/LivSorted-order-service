package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.List;

@Data
public class UserMappedStoreDetails {

	private int id;

	private String name;

	private String address;

	private int active;

	private String storeType;

	private int isSrpStore;

	private Integer prevId;

	private Integer whId;

	private String storeCategory;

	private String storeDeliveryType;

	private List<String> assets;

	private List<String> storeImages;

	private List<String> storeVideos;

	private String openTime;

	private String growthCategory;

	private String growthTheme;

	private String status;

}
