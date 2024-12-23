package com.sorted.rest.services.order.beans;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Data
@ToString(callSuper = true)
public class TicketBean extends TicketListViewBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private List<TicketItemBean> items;

	public static TicketBean newInstance() {
		return new TicketBean();
	}
}