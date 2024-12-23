package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Request containing User's Location
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "location")
@Data
public class Location implements Serializable {

	private static final long serialVersionUID = 1632130020507453176L;

	@ApiModelProperty(value = "latitude of stores", allowEmptyValue = true)
	@DecimalMax(value = "999.9999999")
	@DecimalMin(value = "0.0000001")
	@JsonProperty("lat")
	private String latitude;

	@ApiModelProperty(value = "longitude of societies", allowEmptyValue = true)
	@DecimalMax(value = "999.9999999")
	@DecimalMin(value = "0.0000001")
	@JsonProperty("long")
	private String longitude;

}
