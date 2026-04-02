package org.redpill.alfresco.repo.statistics.scripts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class SitesReport extends DeclarativeWebScript {

	private SiteService siteService;

	@Override
	protected Map<String, Object> executeImpl(final WebScriptRequest req,
			final Status status, final Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		List<SiteInfo> allSites = siteService.listSites(null, null);
		List<Map<String, Serializable>> sitesResult = new ArrayList<Map<String, Serializable>>();
		try {
			for (SiteInfo site : allSites) {
				Map<String, Serializable> siteMap = new HashMap<String, Serializable>();
				siteMap.put("shortName", site.getShortName());
				siteMap.put("title", site.getTitle());

				sitesResult.add(siteMap);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		model.put("recordsReturned", sitesResult.size());
		model.put("totalRecords", sitesResult.size());
		model.put("startIndex", 0);
		model.put("pageSize", sitesResult.size());
		model.put("sites", sitesResult);
		return model;
	}

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

}
