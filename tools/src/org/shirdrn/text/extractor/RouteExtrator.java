package org.shirdrn.text.extractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.shirdrn.common.DBConfig;
import org.shirdrn.common.MongodbAccesser;

import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.analysis.ComplexAnalyzer;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

public class RouteExtrator {
	private Set<String> destination = new HashSet<String>();
	private Analyzer analyzer;
	
	public RouteExtrator(DBConfig dictLoaderConfig, String dictPath) {
		DestinationAccesser accesser = new DestinationAccesser(dictLoaderConfig);
		accesser.load();
		analyzer = new ComplexAnalyzer(Dictionary.getInstance(dictPath));
	}
	
	public void outputDictionay(String output) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(output)));
			Iterator<String> iter = destination.iterator();
			while(iter.hasNext()) {
				writer.write(iter.next().trim());
				writer.write("\n");
			}
			writer.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	int maxRouteOutputCount = 5000;
	public void runWork(DBConfig articleConfig) {
		DestinationAccesser accesser = new DestinationAccesser(articleConfig);
		DBCollection c = accesser.getDBCollection(articleConfig.getCollectionName());
		DBCursor cursor = c.find();
		BufferedWriter writer = null;
		int counter = 0;
		while(cursor.hasNext()) {
			Map map = cursor.next().toMap();
			Article a = mapToArticle(map);
			if(a.getContent().length()<1000) {
				continue;
			}
			try {
				if(writer==null) {
					File file = new File(String.valueOf(System.currentTimeMillis()) + ".txt");
					System.out.println("file: " + file.getAbsolutePath());
					writer = new BufferedWriter(new FileWriter(file));
				}
				List<String> dsts = extractRoute(a.getContent());
				if(dsts.isEmpty()) {
					continue;
				}
				if(dsts.contains("北京") && dsts.contains("巴黎")) {
					String s = format(dsts, a.getId());
					writer.write(s);
					writer.write("\n");
					if(++counter==maxRouteOutputCount) {
						writer.close();	
						writer = null;
						counter = 0;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String format(List<String> dsts, String id) {
		StringBuilder sb = new StringBuilder(id + ": ");
		for(int i=0; i<dsts.size()-1; i++) {
			sb.append(dsts.get(i).trim()).append("->");
		}
		sb.append(dsts.get(dsts.size()-1));
		return sb.toString();
	}

	private Article mapToArticle(Map map) {
		Article a = new Article();
		// _id
		a.setId(map.get("_id").toString());
		// title
		a.setTitle(InformationExtractor.extractContent(getString(map, "title")));
		// content
		a.setContent(InformationExtractor.extractContent(getString(map, "content")));
		// url
		a.setUrl(getString(map, "originUrl"));
		// publishDate
		a.setPubDate(InformationExtractor.formatDatetime(getString(map, "publishDate")));
		// category
//		a.setCategory(getString(map, "category"));
		// spirderName
		a.setSpiderName(getString(map, "spiderName"));
		// keywords
//		a.setKeywords(getString(map, "keywords"));
		return a;
	}
	
	private String getString(Map map, String key) {
		String value = (String)map.get(key);
		if(value!=null) {
			return value.trim();
		} else {
			return "";
		}
	}
	
	class Article {
		private String id = "";
		private String title = "";
		private String content = "";
		private String url = "";
		private String pubDate = "";
		private String category = "";
		private String spiderName = "";
		private String keywords = "";
		private String collectionName = "";
		private String wuqiKeywords = "";
		public Article() {
			super();
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getPubDate() {
			return pubDate;
		}
		public void setPubDate(String pubDate) {
			this.pubDate = pubDate;
		}
		public String getCategory() {
			return category;
		}
		public void setCategory(String category) {
			this.category = category;
		}
		public String getSpiderName() {
			return spiderName;
		}
		public void setSpiderName(String spiderName) {
			this.spiderName = spiderName;
		}
		public String getKeywords() {
			return keywords;
		}
		public void setKeywords(String keywords) {
			this.keywords = keywords;
		}
		public String getCollectionName() {
			return collectionName;
		}
		public void setCollectionName(String collectionName) {
			this.collectionName = collectionName;
		}
		public String getWuqiKeywords() {
			return wuqiKeywords;
		}
		public void setWuqiKeywords(String wuqiKeywords) {
			this.wuqiKeywords = wuqiKeywords;
		}
	}

	
	static class InformationExtractor {
		private static Pattern imgInHTMLPattern = Pattern.compile("<img([^>]*)\\s*>| ");
		private static Pattern badWordPattern = Pattern.compile("[\\w\\s]");
		private static Pattern datetimePattern = Pattern.compile("(\\d{4})[^0-9]*(\\d{1,2})[^0-9]*(\\d{1,2})");
		
		public static String extractContent(String content) {
			content = imgInHTMLPattern.matcher(content).replaceAll("");
			content = badWordPattern.matcher(content).replaceAll("");
			return purgeJsonSpecifiedCharacters(content.trim());
		}
		
		public static String formatDatetime(String orgDatetime){
			String newDatetime = "1901-01-01T01:01:01.001Z";
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
			return purgeJsonSpecifiedCharacters(newDatetime);
		}
		
		private static String purgeJsonSpecifiedCharacters(String input) {
			int length = input.length();
			StringBuffer buf = new StringBuffer(length+128);
			for (int i=0; i<length; i++){
				char curChar = input.charAt(i);
				switch (curChar){
				case '\"':
					buf.append("\\\"");
					break;
				case '\\':
					buf.append("\\\\");
					break;
				case '/':
					buf.append("\\/");
					break;
				case '\b':
					buf.append("\\b");
					break;
				case '\f':
					buf.append("\\f");
					break;
				case '\n':
					buf.append("\\n");
					break;
				case '\r':
					buf.append("\\r");
					break;
				case '\t':
					buf.append("\\t");
					break;
				default:
					buf.append(curChar);
				}
			}
			return buf.toString();
		}
	}
	
	class DestinationAccesser extends MongodbAccesser {
		public DestinationAccesser(DBConfig dbConfig) {
			super(dbConfig);
		}
		public void load() {
			DBCollection collection = helper.getCollection(
					dbConfig.getHost(), dbConfig.getPort(), dbConfig.getDbname(), "Region");
			DBCursor cursor = collection.find(new BasicDBObject("category", "destination"));
			while(cursor.hasNext()) {
				Map map = cursor.next().toMap();
				destination.add((String) map.get("name"));
			}
			cursor.close();
		}
	}
	
	public List<String> extractRoute(String content) {
		List<String> list = new ArrayList<String>();
		Reader reader = new StringReader(content);
		TokenStream ts = analyzer.tokenStream("", reader);
		ts.addAttribute(CharTermAttribute.class);
		try {
			while(ts.incrementToken()) {
				CharTermAttributeImpl attr =  (CharTermAttributeImpl) ts.getAttribute(CharTermAttribute.class);
				String word = attr.toString().trim();
				if(destination.contains(word) 
						&& (list.isEmpty() || (!list.isEmpty() && !list.get(list.size()-1).equals(word)))) {
					list.add(word);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	public static void main(String[] args) {
		DBConfig config = new DBConfig("192.168.0.184", 27017, "tripfm", "");
		RouteExtrator r = new RouteExtrator(config, "E:\\words-dst.dic");
//		r.output("E:\\words-dst.dic");
//		String[] a = r.extractRoute("澳门之行：我去了高雄，然后又在北京，不是，是景德镇啊——我在耶路撒冷忽然想起了在南京的日子，绝对没有纽约爽");
//		for(String word : a) {
//			System.out.println(word);
//		}
		DBConfig articleConfig = new DBConfig("192.168.0.184", 27017, "page", "Article");
		r.runWork(articleConfig);
	}

}
