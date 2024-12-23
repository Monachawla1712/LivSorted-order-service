package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import lombok.Data;

/**
 * Order level Products Tags
 *
 * @author Mohit
 * @version $Id: $Id
 */
@Data
public class OrderProductMetadata implements Serializable {

	private static final long serialVersionUID = -5081176278171548186L;

	private String color;

	private Double value = 0.0;

	private String name;

}
