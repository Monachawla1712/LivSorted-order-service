package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "Bulk Invoice Generation Request")
public class BackofficeBulkInvoiceRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotNull
	private java.sql.Date date;

	@NotEmpty
	private List<String> storeIds;
}
