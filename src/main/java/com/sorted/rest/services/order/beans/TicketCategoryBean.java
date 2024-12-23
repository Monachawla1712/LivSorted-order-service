package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TicketCategoryBean implements Serializable {

	private static final long serialVersionUID = -7538803140039235801L;

	private Integer id;

	private String label;

	private String description;

	private Integer appVisible;

	private Integer isTerminal;

	private Integer parentId;

	private Integer priority;

	private List<String> onCreateActions;

	private List<String> onUpdateActions;

	public static TicketCategoryBean newInstance() {
		return new TicketCategoryBean();
	}
}