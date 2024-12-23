package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
public class RepeatOrderResponse implements Serializable {

	private static final long serialVersionUID = -5531302315962586850L;

	private Long id;

	@NotNull
	private String skuCode;

	@NotNull
	private RepeatOrderPreferences preferences;

	private OrderConstants.RepeatOrderStatus status;

	private Date nextDeliveryDate;

	private DeliverTomorrowDialog repeatOrderSetDialog;

	private DeliverTomorrowDialog deliverTomorrowDialog;

	public static RepeatOrderResponse newInstance() {
		return new RepeatOrderResponse();
	}

	@Data
	public static class DeliverTomorrowDialog {

		private String title;

		private String image;

		private String heading;

		private String message;

		private String cta;
	}
}


