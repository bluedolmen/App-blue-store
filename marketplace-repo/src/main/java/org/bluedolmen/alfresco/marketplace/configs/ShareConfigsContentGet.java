package org.bluedolmen.alfresco.marketplace.configs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.alfresco.repo.web.scripts.content.StreamContent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class ShareConfigsContentGet extends StreamContent {
	
	private static final String TEMPFILE_PREFIX = "share-config";
	private ConfigRepositoryHelper configRepositoryHelper;

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		
		final String modifiedAfter_ = req.getParameter("modifiedAfter");
		final Date modifiedAfter = ConfigRepositoryHelper.parseDate(modifiedAfter_);

		final File file = TempFileProvider.createTempFile(TEMPFILE_PREFIX, ".xml");
		final List<NodeRef> configRefs = configRepositoryHelper.getConfigFileRefs(modifiedAfter);
		configRepositoryHelper.dumpMergedConfigs(configRefs, new FileOutputStream(file));
		
		streamContent(req, res, file);

	}
	
	public void setConfigRepositoryHelper(ConfigRepositoryHelper configRepositoryHelper) {
		this.configRepositoryHelper = configRepositoryHelper;
	}

}
