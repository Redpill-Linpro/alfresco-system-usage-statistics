package org.redpill.alfresco.repo.statistics.service;

import java.util.Date;

public class UserLoginDetails {

  private String userName;

  private String fullName;

  private int logins = 0;

  private Date lastActivity;

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public int getLogins() {
    return logins;
  }

  public void setLogins(int logins) {
    this.logins = logins;
  }

  public Date getLastActivity() {
    return lastActivity;
  }

  public void setLastActivity(Date lastActivity) {
    this.lastActivity = lastActivity;
  }

}
