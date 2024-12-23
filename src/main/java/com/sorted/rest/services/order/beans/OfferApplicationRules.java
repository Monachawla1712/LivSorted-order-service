package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class OfferApplicationRules implements Serializable {

    private static final long serialVersionUID = 8556242705816775840L;

    private Event event;

    private Conditions conditions;

}

