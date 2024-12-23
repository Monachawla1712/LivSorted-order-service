package com.sorted.rest.services.order.beans;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.services.common.upload.csv.CSVMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkA2cSheetBean implements Serializable, CSVMapping {

	private static final long serialVersionUID = 7400716746876340944L;

	private String itemNotes;

	private String userId;

	public static BulkA2cSheetBean newInstance() {
		return new BulkA2cSheetBean();
	}

	@Override
	public BulkA2cSheetBean newBean() {
		return newInstance();
	}

	@Override
	public String getHeaderMapping() {
		return "userId:user_id,itemNotes:item_notes";
	}

	private List<ErrorBean> errors = new ArrayList<>();

}
