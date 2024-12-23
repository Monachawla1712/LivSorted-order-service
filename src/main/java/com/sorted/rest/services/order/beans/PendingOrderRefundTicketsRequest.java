package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class PendingOrderRefundTicketsRequest implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	@NotEmpty
	private List<UUID> orderIds;

}