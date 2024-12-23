package com.sorted.rest.services.order.beans;

import com.sorted.rest.common.utils.ParamsUtils;
import lombok.Data;

import java.io.Serializable;

/**
 * Request containing User's Location
 *
 * @author Abhishek
 * @version $Id: $Id
 */
@Data
public class CoinsParamsObject implements Serializable {

	private static final long serialVersionUID = 1632130020507453176L;

	private Integer isOtpEnabled;

	private Double coinsGivenRatio;

	private Double spToCoinsRatio;

	public static CoinsParamsObject newInstance() {
		CoinsParamsObject entity = new CoinsParamsObject();
		entity.setIsOtpEnabled(ParamsUtils.getIntegerParam("IS_OTP_ENABLED", 0)); // 0 based set below
		if (entity.getIsOtpEnabled() == 0) {
			entity.setCoinsGivenRatio(0.2d);
			entity.setSpToCoinsRatio(0.5d);
		} else {
			entity.setCoinsGivenRatio(Double.parseDouble(ParamsUtils.getParam("COINS_GIVEN_RATIO", "0.2")));
			entity.setSpToCoinsRatio(Double.parseDouble(ParamsUtils.getParam("SP_TO_COINS_RATIO", "0.5")));
		}
		return entity;
	}

}
