package org.redpill.alfresco.repo.statistics.jobs;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import org.redpill.alfresco.repo.statistics.service.ActiveUsers;
import org.redpill.alfresco.repo.statistics.service.UserLoginDetails;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GenerateUserStatistics extends ClusteredExecuter {

  private static final Logger LOG = Logger.getLogger(GenerateUserStatistics.class);

  protected ActiveUsers activeUsers;
  protected Repository repository;
  protected FileFolderService fileFolderService;
  protected NodeService nodeService;
  protected SearchService searchService;
  protected NamespaceService namespaceService;
  protected ContentService contentService;

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public void setFileFolderService(FileFolderService fileFolderService) {
    this.fileFolderService = fileFolderService;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }

  public void setContentService(ContentService contentService) {
    this.contentService = contentService;
  }

  public final String XPATH_DATA_DICTIONARY = "/app:company_home/app:dictionary";
  public final String FOLDER_NAME_REDPILL_LINPRO = "Redpill-Linpro";
  public final String FOLDER_NAME_STATISTICS = "Statistics";
  public final String FOLDER_NAME_USER_STATISTICS = "UserStatistics";

  public void setActiveUsers(ActiveUsers activeUsers) {
    this.activeUsers = activeUsers;
  }

  @Override
  protected String getJobName() {
    return "Generate User Statistics";
  }

  @Override
  protected void executeInternal() {
    LOG.info("Starting generation of user statistics");
    AuthenticationUtil.runAs(new RunAsWork<Void>() {
      @Override
      public Void doWork() throws Exception {
        RetryingTransactionHelper transactionHelper = _transactionService.getRetryingTransactionHelper();
        transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
          @Override
          public Void execute() throws Throwable {
            Map<String, List<UserLoginDetails>> activeUsersByZone;
            try {
              activeUsersByZone = activeUsers.getActiveUsersByZone();
            } catch (Exception e) {
              throw new RuntimeException(e);
            }

            String json = createJson(activeUsersByZone);
            storeResult(json);
            return null;
          }
        }, false, false);
        return null;
      }
    }, AuthenticationUtil.SYSTEM_USER_NAME);
    LOG.info("Finished generating user statistics");
  }

  public String createJson(Map<String, List<UserLoginDetails>> activeUsersByZone) {
    Gson gson = new GsonBuilder().create();
    String json = gson.toJson(activeUsersByZone);
    return json;
  }

  protected void storeResult(String json) {
    List<NodeRef> selectResult = searchService.selectNodes(repository.getRootHome(), XPATH_DATA_DICTIONARY, null, namespaceService, false);
    NodeRef dataDictionaryNodeRef = selectResult.get(0);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Looked up data dictionary: " + dataDictionaryNodeRef);
    }
    NodeRef rlNodeRef = nodeService.getChildByName(dataDictionaryNodeRef, ContentModel.ASSOC_CONTAINS, FOLDER_NAME_REDPILL_LINPRO);
    if (rlNodeRef == null) {
      rlNodeRef = fileFolderService.create(dataDictionaryNodeRef, FOLDER_NAME_REDPILL_LINPRO, ContentModel.TYPE_FOLDER).getNodeRef();
    }

    NodeRef statisticsNodeRef = nodeService.getChildByName(rlNodeRef, ContentModel.ASSOC_CONTAINS, FOLDER_NAME_STATISTICS);
    if (statisticsNodeRef == null) {
      statisticsNodeRef = fileFolderService.create(rlNodeRef, FOLDER_NAME_STATISTICS, ContentModel.TYPE_FOLDER).getNodeRef();
    }

    NodeRef userStatisticsNodeRef = nodeService.getChildByName(statisticsNodeRef, ContentModel.ASSOC_CONTAINS, FOLDER_NAME_USER_STATISTICS);
    if (userStatisticsNodeRef == null) {
      userStatisticsNodeRef = fileFolderService.create(statisticsNodeRef, FOLDER_NAME_USER_STATISTICS, ContentModel.TYPE_FOLDER).getNodeRef();
    }

    int year = Calendar.getInstance().get(Calendar.YEAR);

    NodeRef yearNodeRef = nodeService.getChildByName(userStatisticsNodeRef, ContentModel.ASSOC_CONTAINS, Integer.toString(year));
    if (yearNodeRef == null) {
      yearNodeRef = fileFolderService.create(userStatisticsNodeRef, Integer.toString(year), ContentModel.TYPE_FOLDER).getNodeRef();
    }
    String name = "user-statistics-" + System.currentTimeMillis() + ".json";
    FileInfo create = fileFolderService.create(yearNodeRef, name, ContentModel.TYPE_CONTENT);
    ContentWriter writer = contentService.getWriter(create.getNodeRef(), ContentModel.PROP_CONTENT, true);
    writer.setMimetype("application/json");
    writer.putContent(json);
    writer.guessEncoding();
    writer.setMimetype("application/json");

  }

  @Override
  public void afterPropertiesSet() throws Exception {
    super.afterPropertiesSet();
    Assert.notNull(activeUsers);
    Assert.notNull(repository);
    Assert.notNull(contentService);
    Assert.notNull(fileFolderService);
    Assert.notNull(nodeService);
    Assert.notNull(namespaceService);
    Assert.notNull(searchService);
    LOG.info("Initialized " + GenerateUserStatistics.class.getName());
  }

}
