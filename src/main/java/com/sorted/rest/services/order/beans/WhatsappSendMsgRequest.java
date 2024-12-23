package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class WhatsappSendMsgRequest implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	private List<WhatsappSendMsgSingleRequest> messageRequests = new ArrayList<>();

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class WhatsappSendMsgSingleRequest implements Serializable {

		private String url;

		List<String> params;

		private UUID userId;

		private String phoneNumber;

		private String templateName;

		Map<String, String> fillers;

		private String messageType;
	}
}
