package org.shirdrn.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.shirdrn.common.DBConfig;
import org.shirdrn.common.MongodbAccesser;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

public class AttractionsHelper {

	MongodbAccesser accesser;
	DBConfig config;
	private Set<String> set = new HashSet<String>();
	
	public AttractionsHelper(DBConfig config) {
		this.config = config;
		accesser = new MongodbAccesser(config);
	}
	
	@SuppressWarnings("unchecked")
	public void output(String outputPath) {
		DBCollection collection = accesser.getDBCollection(config.getCollectionName());
		DBCursor cursor = collection.find(new BasicDBObject("category", "attraction"));
		while (cursor.hasNext()) {
			Map map = cursor.next().toMap();
			set.add((String) map.get("name"));
		}
		cursor.close();
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(outputPath)));
			Iterator<String> iter = set.iterator();
			while (iter.hasNext()) {
				writer.write(iter.next().trim());
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
