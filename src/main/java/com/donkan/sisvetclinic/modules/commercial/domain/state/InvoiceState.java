package com.donkan.sisvetclinic.modules.commercial.domain.state;

public interface InvoiceState {

    String getCode();

    boolean canIssue();

    boolean canVerify();

    InvoiceState markPendingVerification();

    InvoiceState accept();

    InvoiceState reject();
}