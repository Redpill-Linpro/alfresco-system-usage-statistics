package org.redpill.alfresco.repo.statistics.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class GenerateSummaryStatisticsJob implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ClusteredExecuter generateSummaryStatistics = (ClusteredExecuter) context.getJobDetail().getJobDataMap().get("generateSummaryStatistics");

    generateSummaryStatistics.execute();
  }

}
