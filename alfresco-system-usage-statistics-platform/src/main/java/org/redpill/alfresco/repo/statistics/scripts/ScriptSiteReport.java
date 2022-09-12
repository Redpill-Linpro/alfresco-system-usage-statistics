package org.redpill.alfresco.repo.statistics.scripts;

import java.util.Date;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.redpill.alfresco.repo.statistics.service.ReportSiteUsage;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class ScriptSiteReport extends BaseScopableProcessorExtension implements InitializingBean {

  protected SiteService siteService;
  protected ReportSiteUsage reportSiteUsage;

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void setReportSiteUsage(ReportSiteUsage reportSiteUsage) {
	  this.reportSiteUsage = reportSiteUsage;
  }

  public long getSiteSize(String shortName) {
    Assert.hasText(shortName, "Invalid site short name '" + shortName + "' for getting size site.");

    return reportSiteUsage.getSiteSize(shortName);
  }

  public int getNumberOfSiteMembers(String shortName) {
    Assert.hasText(shortName, "Invalid site short name '" + shortName + "' for getting size site.");

    return reportSiteUsage.getNumberOfSiteMembers(shortName);
  }

  public Date getLastActivityOnSite(String shortName) {
    Assert.hasText(shortName, "Invalid site short name '" + shortName + "' for getting size site.");

    SiteInfo site = siteService.getSite(shortName);

    try {
      return reportSiteUsage.getLastActivityOnSite(site);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(siteService, "[Assertion failed] - this argument is required; it must not be null");
    Assert.notNull(reportSiteUsage, "[Assertion failed] - this argument is required; it must not be null");
  }

}
