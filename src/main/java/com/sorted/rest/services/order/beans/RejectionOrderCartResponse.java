package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sorted.rest.common.beans.ErrorBean;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(description = "Rejection Cart Request Bean")
@Data
public class RejectionOrderCartResponse implements Serializable {

	private static final long serialVersionUID = 6369074118582415131L;

	private RejectionOrderResponseBean data;

	/**
	 * true implies successful execution
	 */
	private boolean status;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String message;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private ErrorBean error;

	public RejectionOrderCartResponse error(String error, String errorMessage) {
		this.error = new ErrorBean(error, errorMessage);
		this.message = errorMessage;
		this.status = false;
		return this;
	}

	public void setData(RejectionOrderResponseBean data) {
		this.data = data;
		this.status = true;
	}
}
