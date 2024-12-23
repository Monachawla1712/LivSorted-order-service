package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Request Bean of the Customer cart related information
 *
 * @author mohit
 * @version $Id: $Id
 */
@Data
@ApiModel(description = "Customer Cart Request Bean")
@Builder
public class CartRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	private UUID customerId;

	@NotEmpty
	private String skuCode;

	private Date deliveryDate;

	@NotNull
	@Min(value = 0)
	private Double quantity;

	private Double discountAmount = 0d;

	@NotEmpty
	private String channel;

	private Location location;

	private Integer pieceQty;

	@Min(value = 1)
	private Long addressId;

	private Long time;

	@NotNull
	@Min(value = 1)
	private Integer slotId;

	@NotNull
	@Min(value = 1)
	private Integer societyId;

	private List<OrderItemGradeBean> grades;

	private String notes;

	private String storeId;

	private Boolean isRepeatItem;

	private Boolean isOzoneWashedItem;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class OrderItemGradeBean implements Serializable {

		private static final long serialVersionUID = -5639455709044398725L;

		@NotEmpty
		private String name;

		@NotNull
		private Double quantity;

		private Integer pieces;

		public static OrderItemGradeBean newInstance(){
			return new OrderItemGradeBean();
		}
	}
}
