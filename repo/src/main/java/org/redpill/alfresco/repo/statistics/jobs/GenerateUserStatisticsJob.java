package org.redpill.alfresco.repo.statistics.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


public class GenerateUserStatisticsJob implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    ClusteredExecuter generateUserStatistics = (ClusteredExecuter) context.getJobDetail().getJobDataMap().get("generateUserStatistics");

    generateUserStatistics.execute();
  }

}
