package com.sorted.rest.services.order.beans;

import com.sorted.rest.common.websupport.base.BaseBean;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashCollectionResponse extends BaseBean implements Serializable {

	private static final long serialVersionUID = -1308966389323020572L;

	private Long id;

	public static CashCollectionResponse newInstance() {
		return new CashCollectionResponse();
	}
}
