package org.shirdrn.solr.solrj;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.solr.common.SolrInputDocument;

import com.mongodb.BasicDBObject;

public class AttractionPoster extends SolrPostServer {
	
	public AttractionPoster(String url, HttpClient httpClient, MongoConfig mongoConfig) {
		super(url, httpClient, mongoConfig);
	}

	@Override
	public SolrInputDocument createDocument(Map<Object, Object> record) {
		String id = record.get("_id").toString();
		String articleId = (String) record.get("articleId");
		String title = (String) record.get("title");
		String url = (String) record.get("url");
		String spiderName = (String) record.get("spiderName");
		String fragment = makeFragment((BasicDBObject) record.get("fragment"));
		String keyword = (String) record.get("word");
		int pictureCount = (Integer) record.get("pictureCount");
		int selectedCount = (Integer) record.get("selectedCount");
		int fragmentSize = (Integer) record.get("fragmentSize");
		
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("_id", id, 1.0f);
		doc.addField("articleId", articleId, 1.0f);
		doc.addField("title", title, 1.0f);
		doc.addField("url", url, 1.0f);
		doc.addField("spiderName", spiderName, 1.0f);
		doc.addField("content", fragment, 1.0f);
		doc.addField("keyword", keyword, 1.0f);
		doc.addField("pictureCount", pictureCount);
		doc.addField("coverage", (float)selectedCount/fragmentSize);
		return doc;
	}
	
	@SuppressWarnings("unchecked")
	private String makeFragment(BasicDBObject fragment) {
		StringBuilder builder = new StringBuilder();
		Iterator<Map.Entry<Integer, String>> iter = fragment.toMap().entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<Integer, String> entry = iter.next();
			builder.append(entry.getValue().trim()).append("<br>");
		}
		return builder.toString();
	}

}
