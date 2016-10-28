package org.redpill.alfresco.repo.statistics.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class GenerateUserEmailStatisticsJob implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ClusteredExecuter generateUserEmailStatistics = (ClusteredExecuter) context.getJobDetail().getJobDataMap().get("generateUserEmailStatistics");

    generateUserEmailStatistics.execute();
  }

}
