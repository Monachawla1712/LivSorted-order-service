package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserIdsRequest {
	private List<String> ids;
	public static UserIdsRequest newInstance(){
		return new UserIdsRequest();
	}
}
