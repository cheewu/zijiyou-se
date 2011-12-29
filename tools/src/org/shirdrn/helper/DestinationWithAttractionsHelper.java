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

public class DestinationWithAttractionsHelper {

	MongodbAccesser accesser;
	DBConfig config;
	Map<String, Set<String>> destinationWithAttractions = new HashMap<String, Set<String>>();
	
	public DestinationWithAttractionsHelper(DBConfig config) {
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
				Set<String> set = destinationWithAttractions.get(area);
				if(set==null) {
					set = new HashSet<String>();
					destinationWithAttractions.put(area.trim(), set);
				}
				String[] a = attractionKeywords.trim().split(",");
				for(String k : a) {
					set.add(k.trim());
				}
			}
		}
		cursor.close();
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(outputPath)));
			Iterator<Map.Entry<String, Set<String>>> iter = destinationWithAttractions.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, Set<String>> entry = iter.next();
				writer.write(entry.getKey());
				writer.write("=");
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
