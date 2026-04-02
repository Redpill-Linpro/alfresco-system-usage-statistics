package org.redpill.alfresco.repo.statistics.service;

import java.util.List;

import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Collect statistics about: - Number of users - Number of active users (logged
 * in in the last 6 months - Number of documents in repository - Number of sites
 *
 * @author Marcus Svartmark
 *
 */
public class SummaryReport implements InitializingBean {

  protected ReportSiteUsage reportSiteUsage;
  protected ActiveUsers activeUsers;
  protected SearchService searchService;
  protected SiteService siteService;
  protected PersonService personService;
  protected long defaultDaysBackTrackingLimit = 30;

  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }

  public void setDaysBackTrackingLimit(int daysBackTrackingLimit) {
    this.defaultDaysBackTrackingLimit = daysBackTrackingLimit;
  }

  public void setReportSiteUsage(ReportSiteUsage reportSiteUsage) {
    this.reportSiteUsage = reportSiteUsage;
  }

  public void setActiveUsers(ActiveUsers activeUsers) {
    this.activeUsers = activeUsers;
  }

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  /**
   * Get the total number of registered users
   *
   * @return
   */
  public long getNumberOfUsersStatistic() {
    return personService.countPeople();
  }

  /**
   * Fetch the number of active users in the system during the last x months
   * (default 30 days)
   *
   * @return
   */
  public long getNumberOfActiveUsersStatistic() {
    List<UserLoginDetails> activeUsersResult = activeUsers.getActiveUsers(defaultDaysBackTrackingLimit);
    return activeUsersResult.size();
  }

  /**
   * Fetch the total number of documents in the system
   *
   * @return
   */
  public long getNumberOfDocumentsStatistic() {
    String query = "TYPE: \"cm:content\"";
    SearchParameters sp = new SearchParameters();
    sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
    sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
    sp.setMaxItems(0);
    sp.setQuery(query);
    ResultSet result = searchService.query(sp);
    return result.getNumberFound();
  }

  /**
   * Fetch the total number of sites
   *
   * @return
   */
  public long getNumberOfSitesStatistic() {
    List<SiteInfo> sites = siteService.listSites(null, null);
    return sites.size();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(activeUsers);
    Assert.notNull(personService);
    Assert.notNull(reportSiteUsage);
    Assert.notNull(searchService);
    Assert.notNull(siteService);
  }
}
