package org.redpill.alfresco.repo.statistics.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.activities.ActivityService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.site.SiteMemberInfo;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class ReportSiteUsage implements InitializingBean {

  private final static Logger LOG = LoggerFactory.getLogger(ReportSiteUsage.class);
  private ActivityService _activityService;

  private NodeService _nodeService;

  private FileFolderService _fileFolderService;

  private SiteService _siteService;

  private PersonService _personService;
  private static final int UUID_LENGTH = 36;

  public static final String DOCUMENT_LIBRARY = "documentLibrary";
  private static final QName SMART_FOLDER_ASPECT = QName.createQName("http://www.alfresco.org/model/content/smartfolder/1.0", "smartFolder");
  private static final QName SMART_FOLDER_CHILD_ASPECT = QName.createQName("http://www.alfresco.org/model/content/smartfolder/1.0", "smartFolderChild");

  public void setPersonService(PersonService personService) {
    this._personService = personService;
  }

  public void setActivityService(ActivityService activityService) {
    _activityService = activityService;
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setFileFolderService(FileFolderService fileFolderService) {
    _fileFolderService = fileFolderService;
  }

  public void setSiteService(SiteService siteService) {
    _siteService = siteService;
  }

  /**
   * Holds the combined result of a single document library traversal.
   */
  public static class SiteSizeAndLastModified {
    public final long siteSize;
    public final Date lastModified;

    public SiteSizeAndLastModified(long siteSize, Date lastModified) {
      this.siteSize = siteSize;
      this.lastModified = lastModified;
    }
  }

  /**
   * Traverse the document library once and return both total size (bytes) and
   * the date of the most recently modified document.
   *
   * @param shortName site short name
   * @return combined result; never null
   */
  public SiteSizeAndLastModified getSiteSizeAndLastModified(String shortName) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting site size and last modified date for site: " + shortName);
    }
    NodeRef siteNodeRef = _siteService.getSite(shortName).getNodeRef();
    if (siteNodeRef == null || !_nodeService.exists(siteNodeRef)) {
      LOG.warn("Site could not be found: " + shortName);
      return new SiteSizeAndLastModified(0, null);
    }
    NodeRef dlNodeRef = _fileFolderService.searchSimple(siteNodeRef, DOCUMENT_LIBRARY);
    if (dlNodeRef == null || !_nodeService.exists(dlNodeRef)) {
      return new SiteSizeAndLastModified(0, null);
    }

    long size = 0;
    Date lastModified = null;

    List<FileInfo> listFiles = _fileFolderService.listFiles(dlNodeRef);
    for (FileInfo fileInfo : listFiles) {
      ContentData contentData = fileInfo.getContentData();
      if (contentData != null) {
        size += contentData.getSize();
      }
      Date modified = fileInfo.getModifiedDate();
      if (modified != null && (lastModified == null || modified.after(lastModified))) {
        lastModified = modified;
      }
    }

    List<NodeRef> folders = new ArrayList<>();
    getDeepFolders(dlNodeRef, folders);
    for (NodeRef folder : folders) {
      if (folder != null && _nodeService.exists(folder)
              && !_nodeService.hasAspect(folder, SMART_FOLDER_ASPECT)
              && !_nodeService.hasAspect(folder, SMART_FOLDER_CHILD_ASPECT)) {
        listFiles = _fileFolderService.listFiles(folder);
        for (FileInfo fileInfo : listFiles) {
          ContentData contentData = fileInfo.getContentData();
          if (contentData != null) {
            size += contentData.getSize();
          }
          Date modified = fileInfo.getModifiedDate();
          if (modified != null && (lastModified == null || modified.after(lastModified))) {
            lastModified = modified;
          }
        }
      }
    }

    return new SiteSizeAndLastModified(size, lastModified);
  }

  /**
   * Get the total size for all files and folders in a site in bytes
   *
   * @param shortName site short name
   * @return
   * @deprecated Use {@link #getSiteSizeAndLastModified(String)} to avoid a redundant traversal.
   */
  @Deprecated
  public long getSiteSize(String shortName) {
    return getSiteSizeAndLastModified(shortName).siteSize;
  }

  public void getDeepFolders(NodeRef nodeRef, List<NodeRef> folders) {
    try {
      List<ChildAssociationRef> children = _nodeService.getChildAssocs(nodeRef);
      for(ChildAssociationRef child : children) {
        NodeRef childRef = child.getChildRef();
        if(_nodeService.getType(childRef).equals(ContentModel.TYPE_FOLDER)) {
          folders.add(childRef);
          if (childRef.getId().length() > UUID_LENGTH) {
            continue;
          }
          getDeepFolders(childRef, folders);
        }
      }
    } catch (Exception e) {
      LOG.error("Error processing node: " + nodeRef + " with exception: " + e);
    }
  }

  /**
   * Get the number of members of a site
   *
   * @param siteShortName
   *          site short name
   * @return
   */
  public int getNumberOfSiteMembers(String siteShortName) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting number of site members for site: " + siteShortName);
    }
    Map<String, String> listMembers = _siteService.listMembers(siteShortName, null, null, 0, true);

    return listMembers.size();

  }

  /**
   * Returns a list of site managers
   *
   * @param siteShortName
   * @return
   */
  public List<Map<String, Serializable>> getSiteManagers(String siteShortName) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting site managers for site: " + siteShortName);
    }
    List<Map<String, Serializable>> result = new ArrayList<Map<String, Serializable>>();

    Map<String, String> listMembers = _siteService.listMembers(siteShortName, null, "SiteManager", 0, true);
    Set<String> keySet = listMembers.keySet();
    for (String userName : keySet) {
      NodeRef personNodeRef = _personService.getPersonOrNull(userName);
      if (personNodeRef != null) {
        PersonInfo person = _personService.getPerson(personNodeRef);

        Map<String, Serializable> personMap = new HashMap<String, Serializable>();
        personMap.put("userName", person.getUserName());
        personMap.put("fullName", person.getFirstName() + " " + person.getLastName());
        result.add(personMap);
      }

    }
    return result;

  }

  /**
   * Get last activity on site (date)
   *
   * @param site
   *          site info object
   * @return the last activity date
   * @throws Exception
   */
  public Date getLastActivityOnSite(SiteInfo site) throws Exception {
    return (getLastActivityOnSite(site.getShortName()));
  }

  public Date getLastActivityOnSite(String siteShortName) throws Exception {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting last activity for site: " + siteShortName);
    }
    List<String> siteFeedEntries = _activityService.getSiteFeedEntries(siteShortName);

    JSONParser p = new JSONParser();

    if (siteFeedEntries.size() == 0) {
      return null;
    } else {
      Date lastDate = null;

      for (String feedEntry : siteFeedEntries) {
        JSONObject parsedObject = (JSONObject) p.parse(feedEntry);

        if (parsedObject != null) {
          String stringDate = (String) parsedObject.get("postDate");

          Date entryDate = ISO8601DateFormat.parse(stringDate);

          if (lastDate == null || entryDate.compareTo(lastDate) > 0) {
            lastDate = entryDate;
          }
        }
      }

      return lastDate;
    }
  }

  public List<Map<String, Object>> getAllSites() {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Get list of all sites");
    }
    List<SiteInfo> allSites = _siteService.listSites(null, null);
    List<Map<String, Object>> sitesResult = new ArrayList<Map<String, Object>>();
    try {
      for (SiteInfo site : allSites) {
        Map<String, Object> siteMap = new HashMap<String, Object>();
        siteMap.put("shortName", site.getShortName());
        siteMap.put("title", site.getTitle());

        sitesResult.add(siteMap);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return sitesResult;
  }

  /**
   * Returns a list of all users on a site with email
   *
   * @param siteShortName
   * @return
   */
  public List<Map<String, Serializable>> getAllUsersOnSite(String siteShortName) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting all users on site: " + siteShortName);
    }
    List<Map<String, Serializable>> result = new ArrayList<Map<String, Serializable>>();

    Map<String, String> listMembers = _siteService.listMembers(siteShortName, null, null, 0, true);
    Set<String> keySet = listMembers.keySet();
    for (String userName : keySet) {
      NodeRef personNodeRef = _personService.getPersonOrNull(userName);
      if (personNodeRef != null) {
        PersonInfo person = _personService.getPerson(personNodeRef);

        Map<String, Serializable> personMap = new HashMap<String, Serializable>();
        personMap.put("userName", person.getUserName());
        personMap.put("fullName", person.getFirstName() + " " + person.getLastName());
        personMap.put("email", _nodeService.getProperty(personNodeRef, ContentModel.PROP_EMAIL));
        result.add(personMap);
      }

    }
    return result;

  }

  public String getFullName(String siteShortName) {
    String result = null;
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting all users on site: " + siteShortName);
    }

    Map<String, String> listMembers = _siteService.listMembers(siteShortName, null, null, 0, true);
    Set<String> keySet = listMembers.keySet();
    for (String userName : keySet) {
      NodeRef personNodeRef = _personService.getPersonOrNull(userName);
      if (personNodeRef != null) {
        PersonInfo person = _personService.getPerson(personNodeRef);
        result = (person.getFirstName() + " " + person.getLastName());
      }
    }
    return result;
  }
  public String getUserId(String siteShortName) {
    String result = null;
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting all users on site: " + siteShortName);
    }

    Map<String, String> listMembers = _siteService.listMembers(siteShortName, null, null, 0, true);
    Set<String> keySet = listMembers.keySet();
    for (String userName : keySet) {
      NodeRef personNodeRef = _personService.getPersonOrNull(userName);
      if (personNodeRef != null) {
        PersonInfo person = _personService.getPerson(personNodeRef);
        result = (person.getUserName());
      }
    }
    return result;
  }
  public String getEmail(String siteShortName) {
    String result = null;
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting all users on site: " + siteShortName);
    }

    Map<String, String> listMembers = _siteService.listMembers(siteShortName, null, null, 0, true);
    Set<String> keySet = listMembers.keySet();
    for (String userName : keySet) {
      NodeRef personNodeRef = _personService.getPersonOrNull(userName);
      
      if (personNodeRef != null) {
        result = (_nodeService.getProperty(personNodeRef, ContentModel.PROP_EMAIL).toString());
      }
    }
    return result;
  }
  
  /**
   * Get the date of the most recently modified document in the site's document library.
   *
   * @param siteShortName site short name
   * @return the date of the last document modification, or null if the site has no documents
   * @deprecated Use {@link #getSiteSizeAndLastModified(String)} to avoid a redundant traversal.
   */
  @Deprecated
  public Date getLastDocumentModified(String siteShortName) {
    return getSiteSizeAndLastModified(siteShortName).lastModified;
  }

  /**
   * Returns all members of a site with their roles.
   *
   * @param siteShortName site short name
   * @return list of maps with userName, fullName and role
   */
  public List<Map<String, Serializable>> getSiteMembersWithRoles(String siteShortName) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting members with roles for site: " + siteShortName);
    }
    List<Map<String, Serializable>> result = new ArrayList<>();
    List<SiteMemberInfo> membersInfo = _siteService.listMembersInfo(siteShortName, null, null, 0, true);
    for (SiteMemberInfo memberInfo : membersInfo) {
      String userName = memberInfo.getMemberName();
      NodeRef personNodeRef = _personService.getPersonOrNull(userName);
      if (personNodeRef != null) {
        PersonInfo person = _personService.getPerson(personNodeRef);
        Map<String, Serializable> memberMap = new HashMap<>();
        memberMap.put("userName", person.getUserName());
        memberMap.put("fullName", person.getFirstName() + " " + person.getLastName());
        memberMap.put("role", memberInfo.getMemberRole());
        result.add(memberMap);
      }
    }
    return result;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(_activityService, "_activityService is required");
    Assert.notNull(_fileFolderService, "_fileFolderService is required");
    Assert.notNull(_nodeService, "_nodeService is required");
    Assert.notNull(_siteService, "_siteService is required");
    Assert.notNull(_personService, "_personService is required");
  }

}
