package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SocietyListItemBean {

	private static final long serialVersionUID = 7475151543581543540L;

	private Long id;
	private String name;
	private Double latitude;
	private Double longitude;
	private List<TowerItemBean> tower;
	private String storeId;
	private SocietyMetadata metadata;

	@Data
	public static class TowerItemBean implements Serializable {
		private String towerName;
		private Integer floorCount;
		private Integer deliverySequence;
	}

	@Data
	public static class SocietyMetadata implements Serializable {

		private Integer houseHolds;

		private List<Integer> validSlots;

		private Boolean isPrepaid = Boolean.FALSE;
	}

}
