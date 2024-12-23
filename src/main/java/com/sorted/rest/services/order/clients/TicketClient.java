package com.sorted.rest.services.order.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.order.beans.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "tickets", url = "${client.tickets.url}", configuration = { FeignCustomConfiguration.class })
public interface TicketClient {
	@PostMapping(value = "/tickets/internal/pending-ticket-orders")
	PendingOrderRefundTicketsResponse getPendingTicketsOrderIds(@RequestBody PendingOrderRefundTicketsRequest request);

	@GetMapping(value = "/tickets/reference-id/{id}")
	TicketBean fetchTicketByReferenceId(@PathVariable String id);
}