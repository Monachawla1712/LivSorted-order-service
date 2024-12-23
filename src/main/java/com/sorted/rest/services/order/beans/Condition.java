package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class Condition implements Serializable {

    private static final long serialVersionUID = 4655312046699631074L;

    private String fact;

    private Double value;

    private String operator;
}
