package org.shirdrn.text.extractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.shirdrn.common.DBConfig;
import org.shirdrn.helper.MongodbHelper;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class CoordinateExtractor {
	
	private static final Logger LOG = Logger.getLogger(CoordinateExtractor.class);
	private Pattern imageUrlPattern = Pattern.compile("src\\s*=\\s*\"([^>]+)\"");
	private Pattern featurePattern = Pattern.compile("ditu\\.google\\.cn/maps/api/staticmap\\?center");
	private String webpageEncoding = "UTF-8";
	
	/**
	 * 抓取Web网页内容
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private String getWebpageContent(String url) throws IOException {
		StringBuffer content = new StringBuffer();
		try {
			URL u = new URL(url);
			URLConnection connector = u.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(connector.getInputStream(), webpageEncoding));
			String line = "";
			while((line=reader.readLine())!=null) {
				content.append(line);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return content.toString();
	}
	
	int coordsUpdatedCountInDB = 0; // MongoDB中更新成功的坐标计数器
	int missingCoordsCountInDB = 0; // MongoDB里没有坐标的计数器
	int missingCoordsCountInPage = 0; // page没有坐标的计数器
	
	/**
	 * 根据Web网页源代码，抽取地理坐标
	 * @param pageContent
	 * @return
	 */
	private String[] extractCoordinate(String pageContent) {
		String[] coord = null;
		try {
			Matcher m = imageUrlPattern.matcher(pageContent);
			while(m.find()) {
				String image = m.group(1);
				Matcher featureMatcher = featurePattern.matcher(image);
				if(featureMatcher.find()) {
					String[] a = image.split("\\?")[1].split("&");
					coord = a[0].split("=")[1].split(",");
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 返回经纬度数组：经度,纬度
		return coord;
	}
	
	public void runWork(DBConfig config) throws Exception {
		runWork(config, 0);
	}
	
	/**
	 * 提取缺失坐标的记录：缓存id到Set集合中
	 * @param config
	 * @return
	 */
	private Set<ObjectId> populate(DBConfig config) {
		Set<ObjectId> objectIds = new HashSet<ObjectId>();
		DBCollection collection = MongodbHelper.newHelper().getCollection(
				config.getHost(), config.getPort(), config.getDbname(), config.getCollectionName());
		Pattern urlPattern = Pattern.compile("http://www\\.lvping\\.com");
		DBObject query = new BasicDBObject("url", urlPattern);
		DBCursor cur = collection.find(query);
		while (cur.hasNext()) {
			Map map = cur.next().toMap();
			if(map.get("center")==null) {
				Object o = map.get("_id");
				if(o instanceof ObjectId) {
					ObjectId oid = (ObjectId) o;
					objectIds.add(oid);
				}
			}
		}
		cur.close();
		LOG.debug("Ids Count: " + objectIds.size());
		return objectIds;
	}
	
	/**
	 * 执行地理坐标抽取的驱动方法：设置抓起页面时访问外部主机的频率
	 * @param config
	 * @param sleep
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public void runWork(DBConfig config, int sleep) throws Exception {
		DBCollection collection = MongodbHelper.newHelper().getCollection(
				config.getHost(), config.getPort(), config.getDbname(), config.getCollectionName());
		Set<ObjectId> ids = populate(config);
		Iterator<ObjectId> iter = ids.iterator();
		while(iter.hasNext()) {
			ObjectId objectId = iter.next();
			DBObject q = collection.findOne(new BasicDBObject("_id", objectId));
			String url = (String)q.toMap().get("url");
			String[] latLon = null;
			Map center = null;
			try {
				// set sleep interval, in order to avoid visiting webhost frequently
				Thread.sleep(sleep); 
				latLon = extractCoordinate(getWebpageContent(url));
				if(latLon==null) {
					++missingCoordsCountInPage;
					continue;
				}
				center = new HashMap();
				center.put("0", latLon[0]);
				center.put("1", latLon[1]);
			} catch (IOException e) {
				LOG.warn("_id=" + objectId.toString());
				e.printStackTrace();
				continue;
			}
			BasicDBObject o = new BasicDBObject("$set", new BasicDBObject("center", center));
			collection.update(q, o);
			++coordsUpdatedCountInDB;
			LOG.info("Updated: _id=" + objectId.toString() + ", lat=" + latLon[0] + ", lon=" + latLon[1]);
		}
	}
	
	public void setFeaturePattern(Pattern featurePattern) {
		this.featurePattern = featurePattern;
	}
	
	public void setWebpageEncoding(String webpageEncoding) {
		this.webpageEncoding = webpageEncoding;
	}
	
	static int[] randoms;
	static {
		randoms = new int[106];
		int j = 0;
		for (int i = 102; i < 208; i++) {
			randoms[j++] = i;
		}
	}
	
	public static void main(String[] args) {
		String host = "192.168.0.184";
		int port = 27017;
		String dbname = "tripfm";
		String collectionName = "POI";
		DBConfig config = new DBConfig(host, port, dbname, collectionName);
		CoordinateExtractor extractor = new CoordinateExtractor();
		extractor.setWebpageEncoding("GBK");
		extractor.setFeaturePattern(Pattern.compile("ditu\\.google\\.cn/maps/api/staticmap\\?center"));
		Random r = new Random();
		int sleep = randoms[r.nextInt(105)];
		try {
			extractor.runWork(config, sleep);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("coordsUpdatedCountInDB: " + extractor.coordsUpdatedCountInDB);
//		System.out.println("missingCoordsCountInDB: " + extractor.missingCoordsCountInDB);
		System.out.println("missingCoordsCountInPage: " + extractor.missingCoordsCountInPage);

	}

	
}
