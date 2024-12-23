package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class WalletStatementBean implements Serializable {
    private Double amount;
    private Double balance;
    private String txnMode;
    private String txnType;
    private String txnDetail;
    private Date createdAt;
    public static WalletStatementBean newInstance() {
        return new WalletStatementBean();
    }
}
