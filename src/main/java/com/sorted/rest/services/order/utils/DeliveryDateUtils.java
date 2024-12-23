package com.sorted.rest.services.order.utils;

import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.services.order.constants.OrderConstants;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeliveryDateUtils {

	public static Date getDeliveryDate() {
		LocalDateTime now = LocalDateTime.now();
		Date date = Date.valueOf(now.toLocalDate());
		return date;
	}

	public static Date getDeliveryDateForCurrentOrder() {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
		LocalTime localTime = now.toLocalTime();
		String start = ParamsUtils.getParam("CURRENT_MORNING_ORDER_VISIBILITY_END_TIME", "12:00:00");
		if (localTime.isAfter(LocalTime.parse(start))) {
			return Date.valueOf(LocalDate.ofInstant(now.plusDays(1).toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
		} else {
			return Date.valueOf(LocalDate.ofInstant(now.toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
		}
	}

	public Date getDeliveryDateForPreviousOrder(Integer sinceDays) {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
		return Date.valueOf(LocalDate.ofInstant(now.minusDays(sinceDays).toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
	}

	public static Date getCustomDeliveryDate() {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
		LocalTime localTime = now.toLocalTime();
		String start = "12:00:00";
		if (localTime.isAfter(LocalTime.parse(start))) {
			return Date.valueOf(LocalDate.ofInstant(now.plusDays(1).toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
		} else {
			return Date.valueOf(LocalDate.ofInstant(now.toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
		}
	}

	public static java.util.Date getConsumerDeliveryDate(boolean checkForHoliday) {
		java.util.Date deliveryDate = DateUtils.convertDateUtcToIst(DateUtils.addDays(new java.util.Date(), OrderConstants.CONSUMER_ORDER_DAYS_FACTOR));
		if (checkForHoliday) {
			int orderDaysFactor = OrderConstants.CONSUMER_ORDER_DAYS_FACTOR;
			List<LocalDate> holidays = getPublicHolidayDates();
			while (isHoliday(deliveryDate, holidays)) {
				deliveryDate = DateUtils.convertDateUtcToIst(DateUtils.addDays(new java.util.Date(), ++orderDaysFactor));
			}
		}
		return deliveryDate;
	}

	public static java.util.Date getDeliveryDateForCashback() {
		Integer days = ParamsUtils.getIntegerParam("CASHBACK_DAYS_LIMIT", 100);
		LocalDateTime localDate = LocalDateTime.now().plusHours(5).plusMinutes(30).minusDays(days);
		return DateUtils.getDate(DateUtils.SHORT_DATE_FMT, localDate.toString());
	}

	public static java.util.Date getPosDeliveryDate() {
		java.util.Date deliveryDate = DateUtils.convertDateUtcToIst(DateUtils.addDays(new java.util.Date(), OrderConstants.POS_ORDER_DAYS_FACTOR));
		return deliveryDate;
	}

	public static List<LocalDate> getPublicHolidayDates() {
		String holidayParam = ParamsUtils.getParam("CONSUMER_ORDER_HOLIDAYS");
		List<String> holidayDates = List.of(holidayParam.split(","));
		List<LocalDate> dates = new ArrayList<>();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		for (String holidayDate : holidayDates) {
			LocalDate localDate = LocalDate.parse(holidayDate.trim(), dateFormat);
			dates.add(localDate);
		}
		return dates;
	}

	public static boolean isHoliday(java.util.Date deliveryDate, List<LocalDate> holidays) {
		LocalDate deliveryLocalDate = convertToLocalDate(deliveryDate);
		if (CollectionUtils.isEmpty(holidays)) {
			holidays = getPublicHolidayDates();
		}
		return holidays.contains(deliveryLocalDate);
	}

	public static String addDayOfMonthSuffix(String formattedDate) {
		String[] parts = formattedDate.split(", ");
		int day = Integer.parseInt(parts[1]);
		String suffix;
		if (day >= 11 && day <= 13) {
			suffix = "th";
		} else {
			switch (day % 10) {
			case 1:
				suffix = "st";
				break;
			case 2:
				suffix = "nd";
				break;
			case 3:
				suffix = "rd";
				break;
			default:
				suffix = "th";
				break;
			}
		}
		return parts[0] + ", " + day + suffix;
	}

	public static LocalDate convertToLocalDate(java.util.Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}
}
