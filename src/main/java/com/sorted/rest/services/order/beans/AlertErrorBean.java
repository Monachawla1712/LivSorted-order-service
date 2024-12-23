package com.sorted.rest.services.order.beans;

import com.sorted.rest.common.beans.ErrorBean;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The Class AlertErrorBean.
 *
 * @author Mohit
 * @version $Id: $Id
 */
@Data
@NoArgsConstructor
public class AlertErrorBean extends ErrorBean {

	private static final long serialVersionUID = -783601767025088804L;

	private String title;

	private String image;

	private String ctaText;

	private String ctalink;

	private String  heading;

	public AlertErrorBean(String code, String message, String field, String title, String image, String ctaText, String ctalink, String heading) {
		super(code, message, field);
		this.title = title;
		this.image = image;
		this.ctaText = ctaText;
		this.ctalink = ctalink;
		this.heading = heading;
	}
}
