package org.shirdrn.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.shirdrn.helper.MongodbHelper;

import com.mongodb.DBCollection;

public class MongodbAccesser {
	
	protected MongodbHelper helper = MongodbHelper.newHelper();
	protected  DBConfig dbConfig;
	private static Map<String, DBCollection> CACHE = Collections.synchronizedMap(new HashMap<String, DBCollection>());
	
	public MongodbAccesser(DBConfig dbConfig) {
		if(dbConfig!=null) {
			this.dbConfig = dbConfig;
		}
	}
	
	public DBCollection getDBCollection(String collectionName) {
		synchronized (CACHE) {
			DBCollection collection = CACHE.get(collectionName);
			if(collection==null) {
				collection = helper.getCollection(
						dbConfig.getHost(), dbConfig.getPort(), dbConfig.getDbname(), collectionName);
				CACHE.put(collectionName, collection);
			}
		}
		return CACHE.get(collectionName);
	}
}
