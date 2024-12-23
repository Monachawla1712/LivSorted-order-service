package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.util.ArrayList;

@Data
public class RepeatOrderPreferences implements Serializable {

	private static final long serialVersionUID = 3713673830366652451L;

	private Double quantity;

	private ArrayList<DayOfWeek> days;

}
