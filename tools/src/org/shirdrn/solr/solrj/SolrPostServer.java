package org.shirdrn.solr.solrj;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

/**
 * Solr server for indexes operations.
 * 
 * @author shirdrn
 * @date   2011-12-20
 */
public abstract class SolrPostServer implements IndexService {

	protected static final Logger LOG = Logger.getLogger(SolrPostServer.class);
	private CommonsHttpSolrServer server; 
	private ResponseParser responseParser;
	
	protected MongoConfig mongoConfig;
	protected String[] collectionNames;
	protected  int maxCommitCount = 100;
	protected boolean manualOptimize = true;

	protected boolean manualCommit = false;
	protected Collection<SolrInputDocument> docContainer = new ArrayList<SolrInputDocument>();
	protected static int totalCount = 0;
	
	public SolrPostServer(String url, HttpClient httpClient, MongoConfig mongoConfig) {
		try {
			if(httpClient==null) {
				server = new CommonsHttpSolrServer(url);
				server.setSoTimeout(500000);  // socket read timeout
				server.setConnectionTimeout(5000);  
				server.setDefaultMaxConnectionsPerHost(10);  
				server.setMaxTotalConnections(100);
				server.setAllowCompression(true);  
				server.setMaxRetries(1); // defaults to 0.  > 1 not recommended. 
			} else {
				server = new CommonsHttpSolrServer(url, httpClient);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		this.mongoConfig = mongoConfig;
		initialize();
	}

	/**
	 * Initialize the {@link CommonsHttpSolrServer}'s basic parameters.
	 */
	private void initialize() {
		if(responseParser!=null) {
			server.setParser(responseParser);
		} else {
			server.setParser(new XMLResponseParser());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void postUpdate() {
		DBCursor cursor = null;
		try {
			for (String c : collectionNames) {
				LOG.info("MongoDB collection name: " + c);
				DBCollection collection = MongoHelper.newHelper(mongoConfig).getCollection(c);
				DBObject q = new BasicDBObject();
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
	
	/**
	 * Detele lucene {@link Document} by IDs.
	 * @param strings
	 */
	public void deleteById(List<String> strings) {
		try {
			server.deleteById(strings);
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Detele lucene {@link Document} by query.
	 * @param query
	 */
	public void deleteByQuery(String query) {
		try {
			server.deleteByQuery(query);
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Query.
	 * @param params
	 * @param fields
	 * @return
	 */
	public List<Map<String, Object>> query(SolrParams params, String[] fields) {
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		try {
			SolrDocumentList documents = server.query(params).getResults();
			Iterator<SolrDocument> iter = documents.iterator();
			while(iter.hasNext()) {
				SolrDocument doc = iter.next();
				Map<String, Object> map = new HashMap<String, Object>();
				for(String field : fields) {
					map.put(field, doc.getFieldValue(field));
				}
				results.add(map);
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		return results;
	}
	
	/**
	 * When controlling the committing action at client side, finally execute committing.
	 * @throws SolrServerException
	 * @throws IOException
	 */
	protected void finallyCommit() throws SolrServerException, IOException {
		if(!docContainer.isEmpty()) {
			server.add(docContainer);
			commit(false, false);
		}
	}
	
	/**
	 * Commit.
	 * @param waitFlush
	 * @param waitSearcher
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public void commit(boolean waitFlush, boolean waitSearcher) {
		try {
			server.commit(waitFlush, waitSearcher);
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * When controlling the optimizing action at client side, finally execute optimizing.
	 * @param waitFlush
	 * @param waitSearcher
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public void optimize(boolean waitFlush, boolean waitSearcher) {
		try {
			server.optimize(waitFlush, waitSearcher);
			commit(waitFlush, waitSearcher);
		} catch (Exception e) {
			LOG.error("Encounter error when optimizing.",  e);
			try {
				server.rollback();
			} catch (SolrServerException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Optimize.
	 * @param optimize
	 * @throws SolrServerException
	 * @throws IOException
	 */
	protected void optimize(boolean optimize) {
		if(optimize) {
			optimize(true, true);
		}
	}

	/**
	 * Add a {@link SolrInputDocument} or collect object and add to the a collection for batch updating
	 * from a mongodb's recored, a Map object.
	 * @param m
	 * @param oneByOne
	 * @throws SolrServerException
	 * @throws IOException
	 */
	protected void add(Map<Object, Object> m, boolean oneByOne) throws SolrServerException, IOException {
		SolrInputDocument doc = createDocument(m);
		if(oneByOne) {
			server.add(doc);
		} else {
			docContainer.add(doc);
			if(docContainer.size()>maxCommitCount) {
				server.add(docContainer);
				server.commit(false, false);
				docContainer = new ArrayList<SolrInputDocument>();
			}
		}
	}
	
	/**
	 * Set {@link ResponseParser}, default value is {@link XMLResponseParser}.
	 * @param responseParser
	 */
	public void setResponseParser(ResponseParser responseParser) {
		this.responseParser = responseParser;
	}

	/**
	 * Pulling document resource from multiple collections of MongoDB.
	 * @param collectionNames
	 */
	public void setCollectionNames(String[] collectionNames) {
		this.collectionNames = collectionNames;
	}
	
	public void setMaxCommitCount(int maxCommitCount) {
		this.maxCommitCount = maxCommitCount;
	}

	public void setManualCommit(boolean manualCommit) {
		this.manualCommit = manualCommit;
	}

	public void setManualOptimize(boolean manualOptimize) {
		this.manualOptimize = manualOptimize;
	}

	/**
	 * Mongo database configuration.
	 * 
	 * @author shirdrn
	 * @date   2011-12-20
	 */
	public static class MongoConfig implements Serializable {
		private static final long serialVersionUID = -3028092758346115702L;
		private String host;
		private int port;
		private String dbname;
		private String collectionName;
		public MongoConfig(String host, int port, String dbname, String collectionName) {
			super();
			this.host = host;
			this.port = port;
			this.dbname = dbname;
			this.collectionName = collectionName;
		}
		@Override
		public boolean equals(Object obj) {
			MongoConfig other = (MongoConfig) obj;
			return host.equals(other.host) && port==other.port
				&& dbname.equals(other.dbname) && collectionName.equals(other.collectionName);
		}
	}
	
	/**
	 * Mongo database utility.
	 * 
	 * @author shirdrn
	 * @date   2011-12-20
	 */
	static class MongoHelper {
		private static Mongo mongo;
		private static MongoHelper helper;
		private MongoConfig mongoConfig;
		private MongoHelper(MongoConfig mongoConfig) {
			super();
			this.mongoConfig = mongoConfig;
		}
		public synchronized static MongoHelper newHelper(MongoConfig mongoConfig) {
			try {
				if(helper==null) {
					helper = new MongoHelper(mongoConfig);
					mongo = new Mongo(mongoConfig.host, mongoConfig.port);
					Runtime.getRuntime().addShutdownHook(new Thread() {
						@Override
						public void run() {
							if(mongo!=null) {
								mongo.close();
							}
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return helper;
		}			
		public DBCollection getCollection(String collectionName) {
			DBCollection c = null;
			try {
				c = mongo.getDB(mongoConfig.dbname).getCollection(collectionName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return c;
		}	
	}
}
