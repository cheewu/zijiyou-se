package org.shirdrn.solr.search;

import org.apache.lucene.index.Term;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

/**
 * For destination: low level query parser plugin based Lucene query syntax. We assume the query
 * string is a {@link Term} in our indexes, or are some {@link Term}s with
 * specific operate logic.
 * 
 * @author shirdrn
 * @date 2011-12-27
 */
public class DestinationQParserPlugin extends QParserPlugin {

	@Override
	public void init(NamedList args) {

	}

	@Override
	public QParser createParser(String qstr, SolrParams localParams,
			SolrParams params, SolrQueryRequest req) {
		return new DestinationQParser(qstr, localParams, params, req);
	}
}
