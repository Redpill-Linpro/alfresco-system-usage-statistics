package org.redpill.alfresco.repo.statistics.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.query.PagingRequest;
import org.alfresco.service.cmr.activities.ActivityService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class ReportSiteUsage implements InitializingBean {

  private final static Logger LOG = Logger.getLogger(ReportSiteUsage.class);
  private ActivityService _activityService;

  private NodeService _nodeService;

  private FileFolderService _fileFolderService;

  private SiteService _siteService;

  private PersonService _personService;

  public static final String DOCUMENT_LIBRARY = "documentLibrary";

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
   * Get the total size for all files and folders in a site in bytes
   *
   * @param shortName
   *          site short name
   * @return
   */
  public long getSiteSize(String shortName) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("Getting site size for site with shortName: " + shortName);
    }
    long size = 0;
    NodeRef siteNodeRef = _siteService.getSite(shortName).getNodeRef();

    if (siteNodeRef != null && _nodeService.exists(siteNodeRef)) {
      NodeRef dlNodeRef = _fileFolderService.searchSimple(siteNodeRef, DOCUMENT_LIBRARY);

      if (dlNodeRef != null && _nodeService.exists(dlNodeRef)) {
        // List files in document library root and calculate size
        List<FileInfo> listFiles = _fileFolderService.listFiles(dlNodeRef);

        for (FileInfo fileInfo : listFiles) {
          ContentData contentData = fileInfo.getContentData();

          if (contentData == null) {
            continue;
          }

          size = size + contentData.getSize();
        }

        // List all folders and calculate site for all files
        List<FileInfo> allFolders = _fileFolderService.listDeepFolders(dlNodeRef, null);

        for (FileInfo folderInfo : allFolders) {
          NodeRef folderNodeRef = folderInfo.getNodeRef();
          if (folderNodeRef != null && _nodeService.exists(folderNodeRef)) {
            listFiles = _fileFolderService.listFiles(folderNodeRef);

            for (FileInfo fileInfo : listFiles) {
              ContentData contentData = fileInfo.getContentData();

              if (contentData == null) {
                continue;
              }

              size = size + contentData.getSize();
            }
          }
        }
      }

      return size;
    } else {
      LOG.warn("Site could not be found:" + shortName);
      return 0;
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
  
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(_activityService);
    Assert.notNull(_fileFolderService);
    Assert.notNull(_nodeService);
    Assert.notNull(_siteService);
    Assert.notNull(_personService);
  }

}
