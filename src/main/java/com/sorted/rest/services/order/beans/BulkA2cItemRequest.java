package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class BulkA2cItemRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotEmpty
	private String skuCode;

	@NotNull
	@Min(value = 0)
	private Double quantity;

	private Integer pieceQty;

	private List<CartRequest.OrderItemGradeBean> grades;

}
