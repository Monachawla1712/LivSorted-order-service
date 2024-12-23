package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSearchRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	private List<String> storeIds;

	private String city;
}
