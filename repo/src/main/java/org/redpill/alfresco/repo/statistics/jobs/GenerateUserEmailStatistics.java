package org.redpill.alfresco.repo.statistics.jobs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.query.PagingRequest;
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
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.redpill.alfresco.repo.statistics.service.ReportSiteUsage;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GenerateUserEmailStatistics extends ClusteredExecuter {

	private static final Logger LOG = Logger.getLogger(GenerateUserEmailStatistics.class);

	protected ReportSiteUsage reportSiteUsage;
	protected Repository repository;
	protected FileFolderService fileFolderService;
	protected NodeService nodeService;
	protected SearchService searchService;
	protected NamespaceService namespaceService;
	protected ContentService contentService;
	protected SiteService siteService;
	protected PersonService personService;

	public final String XPATH_DATA_DICTIONARY = "/app:company_home/app:dictionary";
	public final String FOLDER_NAME_REDPILL_LINPRO = "Redpill-Linpro";
	public final String FOLDER_NAME_STATISTICS = "Statistics";
	public final String FOLDER_NAME_USER_EMAIL_STATISTICS = "UserEmailStatistics";
	protected final PagingRequest pagingRequest = new PagingRequest(Integer.MAX_VALUE);

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	public void setReportSiteUsage(ReportSiteUsage reportSiteUsage) {
		this.reportSiteUsage = reportSiteUsage;
	}

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

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	@Override
	protected String getJobName() {
		return "Generate Site Statistics";
	}

	@Override
	protected void executeInternal() {
		LOG.debug("Starting generation of User Email statistics");
		AuthenticationUtil.runAs(new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				RetryingTransactionHelper transactionHelper = _transactionService.getRetryingTransactionHelper();
				transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
					@Override
					public Void execute() throws Throwable {

						// First get all sites in repo
						List<SiteInfo> allSites = siteService.listSites(null, null);
						ArrayList<Object> result = new ArrayList<>();

						// Loop through all sites and get all members of every
						// site
						for (SiteInfo site : allSites) {
							Map<String, String> siteMembersMap = siteService
									.listMembers(siteService.getSiteShortName(site.getNodeRef()), null, null, 0);

							List<String> siteMembers = new ArrayList<>(siteMembersMap.keySet());

							// Loop through all members on a site and get the
							// information
							List<Map> siteResult = new ArrayList<>();
							for (String member : siteMembers) {

								Map<String, String> jsonMember = new HashMap<>();

								NodeRef memberNodeRef = personService.getPersonOrNull(member);
								if (memberNodeRef != null) {

									Map<QName, Serializable> properties = nodeService.getProperties(memberNodeRef);

									String role = siteService.getMembersRole(site.getShortName(), member);
									String userId = (String) properties.get(ContentModel.PROP_USERNAME);
									String firstName = (String) properties.get(ContentModel.PROP_FIRSTNAME);
									String lastName = (String) properties.get(ContentModel.PROP_LASTNAME);
									String email = (String) properties.get(ContentModel.PROP_EMAIL);

									jsonMember.put("role", role);
									jsonMember.put("siteTitle", site.getTitle());
									jsonMember.put("siteShortName", site.getShortName());
									jsonMember.put("userId", userId);
									jsonMember.put("fullName", firstName + " " + lastName);
									jsonMember.put("email", email);

									siteResult.add(jsonMember);

								}

							}
							siteResult.sort(new Comparator<Map>() {

								@Override
								public int compare(Map o1, Map o2) {
									String role1 = (String) o1.get("role");
									String role2 = (String) o2.get("role");

									if (role1.equals(role2)) {
										return 0;
									} else if ("SiteManager".equals(role1)) {
										return -1;
									} else if ("SiteManager".equals(role2)) {
										return 1;
									} else if ("SiteCollaborator".equals(role1) && !"SiteCollaborator".equals(role2)) {
										return -1;
									} else if ("SiteCollaborator".equals(role2)) {
										return 1;
									} else if ("SiteContributor".equals(role1) && !"SiteContributor".equals(role2)) {
										return -1;
									} else if ("SiteContributor".equals(role2)) {
										return 1;
									}
									return 1;
								}
							});

							result.addAll(siteResult);
						}

						String json = createJson(result);
						storeResult(json);
						return null;
					}
				}, false, false);
				return null;
			}
		}, AuthenticationUtil.SYSTEM_USER_NAME);
		LOG.debug("Finished generating user email statistics");
	}

	public String createJson(Object obj) {
		Gson gson = new GsonBuilder().create();
		String json = gson.toJson(obj);
		return json;
	}

	protected void storeResult(String json) {
		List<NodeRef> selectResult = searchService.selectNodes(repository.getRootHome(), XPATH_DATA_DICTIONARY, null,
				namespaceService, false);
		NodeRef dataDictionaryNodeRef = selectResult.get(0);
		if (LOG.isTraceEnabled()) {
			LOG.trace("Looked up data dictionary: " + dataDictionaryNodeRef);
		}
		NodeRef rlNodeRef = nodeService.getChildByName(dataDictionaryNodeRef, ContentModel.ASSOC_CONTAINS,
				FOLDER_NAME_REDPILL_LINPRO);
		if (rlNodeRef == null) {
			rlNodeRef = fileFolderService
					.create(dataDictionaryNodeRef, FOLDER_NAME_REDPILL_LINPRO, ContentModel.TYPE_FOLDER).getNodeRef();
		}

		NodeRef statisticsNodeRef = nodeService.getChildByName(rlNodeRef, ContentModel.ASSOC_CONTAINS,
				FOLDER_NAME_STATISTICS);
		if (statisticsNodeRef == null) {
			statisticsNodeRef = fileFolderService.create(rlNodeRef, FOLDER_NAME_STATISTICS, ContentModel.TYPE_FOLDER)
					.getNodeRef();
		}

		NodeRef userStatisticsNodeRef = nodeService.getChildByName(statisticsNodeRef, ContentModel.ASSOC_CONTAINS,
				FOLDER_NAME_USER_EMAIL_STATISTICS);
		if (userStatisticsNodeRef == null) {
			userStatisticsNodeRef = fileFolderService
					.create(statisticsNodeRef, FOLDER_NAME_USER_EMAIL_STATISTICS, ContentModel.TYPE_FOLDER)
					.getNodeRef();
		}

		int year = Calendar.getInstance().get(Calendar.YEAR);

		NodeRef yearNodeRef = nodeService.getChildByName(userStatisticsNodeRef, ContentModel.ASSOC_CONTAINS,
				Integer.toString(year));
		if (yearNodeRef == null) {
			yearNodeRef = fileFolderService
					.create(userStatisticsNodeRef, Integer.toString(year), ContentModel.TYPE_FOLDER).getNodeRef();
		}
		String name = "user-email-statistics-" + System.currentTimeMillis() + ".json";
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
		// Assert.notNull(reportSiteUsage);
		// Assert.notNull(repository);
		Assert.notNull(contentService);
		Assert.notNull(fileFolderService);
		Assert.notNull(nodeService);
		Assert.notNull(namespaceService);
		Assert.notNull(searchService);
		Assert.notNull(siteService);
		Assert.notNull(personService);
		LOG.debug("Initialized " + GenerateUserEmailStatistics.class.getName());
	}

}
