package org.shirdrn.helper;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class MongodbHelper {
	private static final Logger LOG = Logger.getLogger(MongodbHelper.class);
	private static Mongo mongo;
	private static MongodbHelper instance = null;
	
	public synchronized static MongodbHelper newHelper() {
		if(instance==null) {
			instance = new MongodbHelper();
			LOG.debug("New a singleton Mongo instance.");
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						if(instance!=null) {
							instance.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		LOG.debug("Add a shutdown hook for Mongo.");
		return instance;
	}
	
	public DBCollection getCollection(String host, int port, String dbname, String collectionName) {
		try {
			mongo = new Mongo(host, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}
		DBCollection dbc = mongo.getDB(dbname).getCollection(collectionName);
		return dbc;
	}
	
	public void close() {
		if(mongo!=null) {
			mongo.close();
		}
	}
	
}
