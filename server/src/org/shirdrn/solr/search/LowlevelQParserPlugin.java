package org.shirdrn.solr.search;

import org.apache.lucene.index.Term;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

/**
 * Low level query parser plugin based Lucene query syntax. We assume the query
 * string is a {@link Term} in our indexes, or are some {@link Term}s with
 * specific operate logic.
 * 
 * @author shirdrn
 * @date 2011-11-10
 */
public class LowlevelQParserPlugin extends QParserPlugin {

	@Override
	public void init(@SuppressWarnings("rawtypes") NamedList args) {

	}

	@Override
	public QParser createParser(String qstr, SolrParams localParams,
			SolrParams params, SolrQueryRequest req) {
		return new LowlevelQParser(qstr, localParams, params, req);
	}
}
