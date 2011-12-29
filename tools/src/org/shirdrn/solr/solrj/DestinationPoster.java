package org.shirdrn.solr.solrj;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.QueryOperators;

public class DestinationPoster extends SolrPostServer {

	public DestinationPoster(String url, HttpClient httpClient, MongoConfig mongoConfig) {
		super(url, httpClient, mongoConfig);
	}
	
	@SuppressWarnings("unchecked")
	public void postUpdate() {
		DBCursor cursor = null;
		DBObject q = new BasicDBObject();
		// 选择文章包含景点关键词个数大于1的记录
//		DBObject q = new BasicDBObject("dstAttractionCount", new BasicDBObject(QueryOperators.GT, 1));
		try {
			for (String c : collectionNames) {
				LOG.info("MongoDB collection name: " + c);
				DBCollection collection = MongoHelper.newHelper(mongoConfig).getCollection(c);
				cursor = collection.find(q);
				while(cursor.hasNext()) {
					try {
						Map<Object, Object> m = cursor.next().toMap();
						if(manualCommit) {
							add(m, true);
						} else {
							add(m, false);
						}
						++totalCount;
						LOG.info("Add fragment: NO.=" + totalCount + ", _id=" + m.get("_id").toString() + ", spiderName=" + m.get("spiderName"));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				cursor.close();
			}
			LOG.info("Add totalCount: " + totalCount);
			finallyCommit();
			optimize(manualOptimize);
		} catch (MongoException e) {
			e.printStackTrace();
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		String dstAttractions = (String) record.get("dstAttractions");
		int selectedCount = (Integer) record.get("selectedParagraphCount");
		int fragmentSize = (Integer) record.get("fragmentSize");
		
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("_id", id, 1.0f);
		doc.addField("articleId", articleId, 1.0f);
		doc.addField("title", title, 1.0f);
		doc.addField("url", url, 1.0f);
		doc.addField("spiderName", spiderName, 1.0f);
		doc.addField("content", fragment, 1.0f);
		doc.addField("keyword", keyword, 1.0f);
		doc.addField("includeAttractions", dstAttractions, 1.0f);
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
