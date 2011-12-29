package org.shirdrn.solr.solrj;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

/**
 * 基于扩展目的地景点关键词的方法，对文章进行索引
 * 
 * @author shirdrn
 * @date   2011-12-27
 */
public class DestinationArticlePoster extends SolrPostServer {

	private int maxIndexDocCount = Integer.MAX_VALUE;
	
	public DestinationArticlePoster(String url, HttpClient httpClient, MongoConfig mongoConfig) {
		super(url, httpClient, mongoConfig);
	}
	
	@SuppressWarnings("unchecked")
	public void postUpdate() {
		DBCursor cursor = null;
		DBObject q = new BasicDBObject();
		try {
			for (String c : collectionNames) {
				LOG.info("MongoDB collection name: " + c);
				DBCollection collection = MongoHelper.newHelper(mongoConfig).getCollection(c);
				cursor = collection.find(q);
				while(cursor.hasNext()) {
					if(totalCount>maxIndexDocCount) {
						break;
					}
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
		String title = (String) record.get("title");
		String url = (String) record.get("url");
		String spiderName = (String) record.get("spiderName");
		String content = (String) record.get("content");
		String publishDate = formatDatetime((String) record.get("publishDate"));
		List images = (List) record.get("images");
		int pictureCount = 0;
		if(images!=null) {
			pictureCount = images.size();
		}
		
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("_id", id, 1.0f);
		doc.addField("title", title, 1.0f);
		doc.addField("url", url, 1.0f);
		doc.addField("spiderName", spiderName, 1.0f);
		doc.addField("content", content, 1.0f);
		doc.addField("publishDate", publishDate, 1.0f);
		doc.addField("pictureCount", pictureCount, 1.0f);
		return doc;
	}
	
	private static Pattern datetimePattern = Pattern.compile("(\\d{4})[^0-9]*(\\d{1,2})[^0-9]*(\\d{1,2})");
	public String formatDatetime(String orgDatetime){
		String newDatetime = "1901-01-01T01:01:01.001Z";
		if(orgDatetime==null) {
			orgDatetime = newDatetime;
		}
		Matcher matcher = datetimePattern.matcher(orgDatetime);
		if(matcher.find()){
			newDatetime="";
			int totalNum=matcher.groupCount();
			//年
			if(totalNum>=1){
				newDatetime=matcher.group(1);
			}
			else{
				return null;
			}
			//月
			if(totalNum>=2){
				newDatetime+="-"+matcher.group(2);
			}
			else{
				newDatetime+="-1";
			}
			//日
			if(totalNum>=3){
				newDatetime+="-"+matcher.group(3);
			}
			else{
				newDatetime+="-1";
			}
			newDatetime+="T23:59:59Z";
		}
		return newDatetime;
	}

	public void setMaxIndexDocCount(int maxIndexDocCount) {
		this.maxIndexDocCount = maxIndexDocCount;
	}

}
