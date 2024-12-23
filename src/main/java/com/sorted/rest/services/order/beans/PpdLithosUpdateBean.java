package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;

@Data
public class PpdLithosUpdateBean implements Serializable {
    private String lithosOrderId;
    private String status;
}