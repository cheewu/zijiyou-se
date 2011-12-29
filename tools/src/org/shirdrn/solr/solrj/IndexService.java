package org.shirdrn.solr.solrj;

import java.util.Map;

import org.apache.solr.common.SolrInputDocument;

public interface IndexService {

	/**
	 * Create a {@link SolrInputDocument} object according to your demand.
	 * @param record
	 * @return
	 */
	public SolrInputDocument createDocument(Map<Object, Object> record);
}
