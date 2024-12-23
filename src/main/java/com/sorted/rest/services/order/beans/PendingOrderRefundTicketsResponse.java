package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class PendingOrderRefundTicketsResponse implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private List<UUID> orderIds;
}