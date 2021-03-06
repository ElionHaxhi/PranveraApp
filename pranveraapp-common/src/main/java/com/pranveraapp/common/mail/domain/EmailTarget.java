package com.pranveraapp.common.mail.domain;

import java.io.Serializable;

public interface EmailTarget extends Serializable{

	public String getEmailAddress();
    public void setEmailAddress(String emailAddress);
    public String[] getCCAddresses();
    public void setCCAddresses(String[] ccAddresses);
    public String[] getBCCAddresses();
    public void setBCCAddresses(String[] BCCAddresses);
}
