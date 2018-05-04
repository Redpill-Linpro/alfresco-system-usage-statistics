package org.redpill.alfresco.repo.statistics.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class ActiveUsers implements InitializingBean {

  private final static Logger LOG = Logger.getLogger(ActiveUsers.class);
  private static final String FULL_NAME_PATH = "/stats-authentication/login/no-error/fullName";
  private static final int AUDIT_QUERY_MAX_RESULT = 500000;
  protected String internalUsersZoneName;

  public static final String INTERNAL_USERS = "internal";
  public static final String EXTERNAL_USERS = "external";

  protected int minLoginsTrackingLimit = 1;
  protected long defaultDaysBackTrackingLimit = 30;

  protected NodeService _nodeService;

  protected PersonService _personService;

  protected AuditService _auditService;

  public void setInternalUsersZoneName(String internalUsersZoneName) {
    this.internalUsersZoneName = internalUsersZoneName;
  }

  public void setDaysBackTrackingLimit(int daysBackTrackingLimit) {
    this.defaultDaysBackTrackingLimit = daysBackTrackingLimit;
  }

  public void setMinLoginsTrackingLimit(int minLoginsTrackingLimit) {
    this.minLoginsTrackingLimit = minLoginsTrackingLimit;
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setPersonService(PersonService personService) {
    _personService = personService;
  }

  public void setAuditService(AuditService auditService) {
    _auditService = auditService;
  }

  public Map<String, List<UserLoginDetails>> getActiveUsersByZone() {
    Map<String, List<UserLoginDetails>> activeUsersByZone = new HashMap<String, List<UserLoginDetails>>();

    List<UserLoginDetails> internalUsers = new ArrayList<UserLoginDetails>();

    List<UserLoginDetails> externalUsers = new ArrayList<UserLoginDetails>();

    List<UserLoginDetails> activeUsers = getActiveUsers();

    for (UserLoginDetails user : activeUsers) {
      NodeRef person = null;

      person = _personService.getPersonOrNull(user.getUserName());
      if (person == null) {
        LOG.warn("Could not find person " + user.getUserName());
        continue;
      }

      List<ChildAssociationRef> parentAssocs = _nodeService.getParentAssocs(person);

      boolean isInternalUser = false;

      for (ChildAssociationRef parentAssoc : parentAssocs) {
        if (ContentModel.ASSOC_IN_ZONE.equals(parentAssoc.getTypeQName())) {
          NodeRef parentRef = parentAssoc.getParentRef();

          String property = (String) _nodeService.getProperty(parentRef, ContentModel.PROP_NAME);

          if (LOG.isDebugEnabled()) {
            LOG.debug("User: " + user.getUserName() + " belongs to zone: " + property);
          }

          if (internalUsersZoneName.equalsIgnoreCase(property)) {
            // Internal user (ldap user)
            isInternalUser = true;
          }
        }
      }

      if (isInternalUser) {
        // Internal user (ldap user)
        internalUsers.add(user);
      } else {
        // External user (alfresco internal user)
        externalUsers.add(user);
      }
    }

    activeUsersByZone.put(INTERNAL_USERS, internalUsers);

    activeUsersByZone.put(EXTERNAL_USERS, externalUsers);

    return activeUsersByZone;
  }

  /**
   * Queries the audit service to get last login for users
   *
   * @return list of user objects
   */
  public List<UserLoginDetails> getActiveUsers() {

    return getActiveUsers(defaultDaysBackTrackingLimit);
  }

  /**
   * Queries the audit service to get last login for users
   *
   * @return list of user objects
   */
  public List<UserLoginDetails> getActiveUsers(long daysBackTrackingLimit) {
    List<UserLoginDetails> resultList = new ArrayList<UserLoginDetails>();

    final Map<String, UserLoginDetails> resultMap = new HashMap<String, UserLoginDetails>();

    AuditQueryCallback callback = new AuditQueryCallback() {

      @Override
      public boolean valuesRequired() {
        return true;
      }

      @Override
      public boolean handleAuditEntry(Long entryId, String applicationName, String user, long time, Map<String, Serializable> values) {
        UserLoginDetails userLoginDetails;

        // If the user has already been found, just increment number of
        // logins and track last activity
        if (resultMap.containsKey(user)) {
          userLoginDetails = resultMap.get(user);

          userLoginDetails.setLogins(userLoginDetails.getLogins() + 1);

          Date date = new Date(time);

          if (date.after(userLoginDetails.getLastActivity())) {
            userLoginDetails.setLastActivity(date);
          }
        } else if (user != null) {
          userLoginDetails = new UserLoginDetails();

          userLoginDetails.setLogins(1);

          Date date = new Date(time);

          userLoginDetails.setLastActivity(date);

          userLoginDetails.setUserName(user);

          userLoginDetails.setFullName((String) values.get(FULL_NAME_PATH));

          resultMap.put(user, userLoginDetails);
        } else {
          return true;
        }
        if (LOG.isDebugEnabled()) {
          LOG.trace("Application name: " + applicationName + ", Values: " + values.toString() + ", Username: " + user + ", Time: " + time + ", FullName: " + (String) values.get(FULL_NAME_PATH));
        }

        return true;
      }

      @Override
      public boolean handleAuditEntryError(Long entryId, String errorMsg, Throwable error) {
        throw new AlfrescoRuntimeException(errorMsg, error);
      }

    };

    AuditQueryParameters parameters = new AuditQueryParameters();
    parameters.setApplicationName("stats-authentication");

    Date now = new Date();

    long msBack = (daysBackTrackingLimit * 24L * 3600L * 1000L);
    long startTime = now.getTime() - msBack;

    parameters.setFromTime(startTime);

    parameters.setToTime(now.getTime());
    LOG.debug("Performing audit query, startTime: " + startTime + " toTime: " + now.getTime() + " msBack: " + msBack);
    _auditService.auditQuery(callback, parameters, AUDIT_QUERY_MAX_RESULT);

    // Filter out users who are not active
    for (UserLoginDetails user : resultMap.values()) {
      if (user.getLogins() >= minLoginsTrackingLimit) {
        resultList.add(user);
      }
    }

    return resultList;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(internalUsersZoneName);
    Assert.notNull(_nodeService);
    Assert.notNull(_personService);
    Assert.notNull(_auditService);
  }

}
