package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class TicketItemBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private Long id;

	private Long ticketId;

	private List<String> attachments;

	private String status;

	private String platform;

	private String remarks;

	private TicketResolutionDetailsBean details;

	private TicketCategoryNode category;

	private Date createdAt;

	private Date modifiedAt;

	public static TicketItemBean newInstance() {
		return new TicketItemBean();
	}

	@Data
	public static class TicketCategoryNode {

		private Integer id;

		private String label;

		private String description;

		private Integer parentId;

		private List<TicketCategoryNode> children;
	}
}