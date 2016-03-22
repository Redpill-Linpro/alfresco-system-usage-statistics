package org.redpill.alfresco.repo.statistics.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class GenerateSiteStatisticsJob implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ClusteredExecuter generateSiteStatistics = (ClusteredExecuter) context.getJobDetail().getJobDataMap().get("generateSiteStatistics");

    generateSiteStatistics.execute();
  }

}
