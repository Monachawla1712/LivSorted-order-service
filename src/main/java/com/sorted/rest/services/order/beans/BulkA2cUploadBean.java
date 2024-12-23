package com.sorted.rest.services.order.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BulkA2cUploadBean extends BulkA2cSheetBean implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotEmpty
	List<BulkA2cItemRequest> items;

}
