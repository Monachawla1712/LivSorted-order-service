package com.sorted.rest.services.order.utils;

import com.sorted.rest.services.order.beans.Condition;
import com.sorted.rest.services.order.beans.Conditions;

import java.lang.reflect.Field;
import java.util.function.BiPredicate;
import java.util.HashMap;
import java.util.Map;

public class ConditionFulfillment<T> {

	private final Map<String, BiPredicate<Object, Double>> operatorMap;

	public ConditionFulfillment() {
		operatorMap = new HashMap<>();
		operatorMap.put("equals", (a, b) -> a.equals(b));
		operatorMap.put("greaterThan", (a, b) -> ((Comparable) a).compareTo(b) > 0);
		operatorMap.put("lessThan", (a, b) -> ((Comparable) a).compareTo(b) < 0);
		operatorMap.put("greaterThanInclusive", (a, b) -> ((Comparable) a).compareTo(b) >= 0);
		operatorMap.put("lessThanInclusive", (a, b) -> ((Comparable) a).compareTo(b) <= 0);
	}

	public boolean fulfillConditions(T object, Conditions conditions) {
		for (Condition condition : conditions.getAll()) {
			if (!fulfillCondition(object, condition)) {
				return false;
			}
		}
		return true;
	}

	private boolean fulfillCondition(T object, Condition condition) {
		try {
			Field field = object.getClass().getDeclaredField(condition.getFact());
			field.setAccessible(true);
			Object value = field.get(object);

			BiPredicate<Object, Double> operatorPredicate = operatorMap.get(condition.getOperator());
			if (operatorPredicate == null) {
				throw new IllegalArgumentException("Unsupported operator: " + condition.getOperator());
			}

			return operatorPredicate.test(value, condition.getValue());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("Error accessing field: " + condition.getFact(), e);
		}
	}
}