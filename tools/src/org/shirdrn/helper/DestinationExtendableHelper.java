package org.shirdrn.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.shirdrn.common.DBConfig;
import org.shirdrn.common.MongodbAccesser;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

/**
 * 导出目的地相关景点的映射关系：
 * 每行的格式形如：
 * 目的地=>景点1,景点2,景点3
 * 例如：
 * 北京=>天安门,故宫,天坛,颐和园,圆明园,王府井
 * 
 * @author shirdrn
 * @date   2011-12-27
 */
public class DestinationExtendableHelper {

	MongodbAccesser accesser;
	DBConfig config;
	Map<String, Set<String>> mappings = new HashMap<String, Set<String>>();
	
	public DestinationExtendableHelper(DBConfig config) {
		this.config = config;
		accesser = new MongodbAccesser(config);
	}
	
	@SuppressWarnings("unchecked")
	public void output(String outputPath) {
		DBCollection collection = accesser.getDBCollection(config.getCollectionName());
		DBCursor cursor = collection.find(new BasicDBObject("category", "attraction"));
		while (cursor.hasNext()) {
			Map map = cursor.next().toMap();
			String area = (String) map.get("area");
			String attractionKeywords = (String) map.get("keyword");
			if(area!=null && attractionKeywords!=null && !area.isEmpty() && !attractionKeywords.isEmpty()) {
				Set<String> set = mappings.get(area);
				if(set==null) {
					set = new HashSet<String>();
					mappings.put(area.trim(), set);
				}
				String[] a = attractionKeywords.trim().split(",");
				for(String k : a) {
					if(!k.isEmpty()) {
						set.add(k.trim());
					}
				}
			}
		}
		cursor.close();
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(outputPath)));
			Iterator<Map.Entry<String, Set<String>>> iter = mappings.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, Set<String>> entry = iter.next();
				writer.write(entry.getKey());
				writer.write("=>");
				int n = 0;
				for(String attr : entry.getValue()) {
					writer.write(attr);
					if(++n!=entry.getValue().size()) {
						writer.write(",");
					}
				}
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
