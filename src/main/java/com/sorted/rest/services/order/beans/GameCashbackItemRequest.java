package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Request Bean of the Customer Game Cashback Item
 *
 * @author mohit
 * @version $Id: $Id
 */
@Data
@ApiModel(description = "Game Cashback Item Request Bean")
public class GameCashbackItemRequest implements Serializable {

	private static final long serialVersionUID = -4335170078785526138L;

	@NotEmpty
	private String skuCode;

}
