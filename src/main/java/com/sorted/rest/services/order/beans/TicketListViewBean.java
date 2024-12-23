package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TicketListViewBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private Long id;

	private String requesterEntityType;

	private String requesterEntityId;

	private String requesterEntityCategory;

	private String referenceId;

	private Date lastAddedAt;

	private Integer draftCount;

	private Integer pendingCount;

	private Integer closedCount;

	private Integer cancelledCount;

	private TicketMetadataBean metadata;

	public static TicketListViewBean newInstance() {
		return new TicketListViewBean();
	}
}