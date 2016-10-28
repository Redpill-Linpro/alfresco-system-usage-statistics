package org.redpill.alfresco.repo.statistics.jobs;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.repo.statistics.service.ReportSiteUsage;

public class GenerateUserEmailStatisticsTest {

	private Mockery context;

	protected SiteService siteService;
	protected PersonService personService;
	protected AuthorityService authorityService;
	protected NodeService nodeService;
	protected FileFolderService fileFolderService;
	protected SearchService searchService;
	protected NamespaceService namespaceService;
	protected ContentService contentService;
	protected JobLockService jobLockService;
	protected TransactionService transactionService;
	protected ReportSiteUsage reportSiteUsage;
	protected Repository repository;

	private GenerateUserEmailStatistics generateUserEmailStatistics = new GenerateUserEmailStatistics();

	private List<String> sortedList = new ArrayList<>();

	@Before
	public void setUp() throws Exception {
		context = new Mockery();

		siteService = context.mock(SiteService.class);
		personService = context.mock(PersonService.class);
		nodeService = context.mock(NodeService.class);
		contentService = context.mock(ContentService.class);
		namespaceService = context.mock(NamespaceService.class);
		searchService = context.mock(SearchService.class);
		fileFolderService = context.mock(FileFolderService.class);
		jobLockService = context.mock(JobLockService.class);
		transactionService = context.mock(TransactionService.class);
		//reportSiteUsage = context.mock(ReportSiteUsage.class);
		// repository = context.mock(Repository.class);

		generateUserEmailStatistics.setSiteService(siteService);
		generateUserEmailStatistics.setPersonService(personService);
		generateUserEmailStatistics.setNodeService(nodeService);
		generateUserEmailStatistics.setNamespaceService(namespaceService);
		generateUserEmailStatistics.setContentService(contentService);
		generateUserEmailStatistics.setSearchService(searchService);
		generateUserEmailStatistics.setFileFolderService(fileFolderService);
		generateUserEmailStatistics.setJobLockService(jobLockService);
		generateUserEmailStatistics.setTransactionService(transactionService);
		generateUserEmailStatistics.setReportSiteUsage(reportSiteUsage);
		generateUserEmailStatistics.setRepository(repository);

		generateUserEmailStatistics.afterPropertiesSet();

		sortedList.add("SiteManager");
		sortedList.add("SiteManager");
		sortedList.add("SiteCollaborator");
		sortedList.add("SiteContributor");
		sortedList.add("SiteContributor");
		sortedList.add("SiteContributor");
		sortedList.add("SiteConsumer");

	}

	@Test
	public void testSortedList() {
		List<String> testList = new ArrayList<>();
		final RetryingTransactionHelper transHelper = new TestTransactionHelper();
		context.checking(new Expectations() {
			{
				oneOf(transactionService).getRetryingTransactionHelper();
				will(returnValue(transHelper));
			}
		});

		generateUserEmailStatistics.executeInternal();

		assertTrue(sortedList.equals(testList));
	}
	
	public class TestTransactionHelper extends RetryingTransactionHelper {
		public <R> R doInTransaction(RetryingTransactionCallback<R> cb, boolean readOnly, boolean requiresNew) {
			try {
				return cb.execute();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

}
