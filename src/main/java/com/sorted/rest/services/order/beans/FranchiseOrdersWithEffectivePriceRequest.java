package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.sql.Date;

@Data
@ApiModel(description = "Franchise Orders with Effective Price Request")
public class FranchiseOrdersWithEffectivePriceRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotNull
	private Date date;

	@NotEmpty
	private List<String> storeIds;
}
