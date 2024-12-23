package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.List;

@Data
public class GameBean {

	private Boolean offerEligible = false;

	private Boolean gamePlayEligible = false;

	private String message;

	private Integer gameStage;

	private List<String> disallowedSkus;

	public static GameBean newInstance() {
		return new GameBean();
	}

}
