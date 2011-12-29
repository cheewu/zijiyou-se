package org.shirdrn.solr.solrj;

import junit.framework.TestCase;

import org.shirdrn.solr.solrj.SolrPostServer.MongoConfig;

public class DestinationPosterTest extends TestCase {
	
	DestinationPoster myServer;
	MongoConfig config;
	String url;
	String[] collectionNames;
	
	@Override
	protected void setUp() throws Exception {
		url = "http://106.187.38.163:8080/server/dst/";
		url = "http://192.168.0.197:8080/server/dst/";
		config = new MongoConfig("192.168.0.184", 27017, "fragmentDestinations", "");
		myServer = new DestinationPoster(url, null, config);
		myServer.setMaxCommitCount(500);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testPostUpdate() {
		collectionNames = new String[] {
				"sina",
				"lvping",
				"daodao",
				"go2eu",
				"mafengwo",
				"lotour",
				"17u",
				"sohu",
				"baseSe",
				"bytravel"
		};
		myServer.setCollectionNames(collectionNames);
		myServer.setManualCommit(true);
		myServer.setManualOptimize(true);
		myServer.postUpdate();
	}
	
//	public void testPostDelete() {
//		List<String> strings = new ArrayList<String>();
//		strings.add("4ef051342c4117a38f63ee97");
//		strings.add("4ef051322c4117a38f63ee36");
//		strings.add("4ef051a42c4117a38f63fb51");
//		strings.add("4ef050d92c4117a38f63dda4");
//		strings.add("4ef051fe2c4117a38f640bc8");
//		strings.add("4ef048ef2c4117a38f6207ce");
//		strings.add("4ef049062c4117a38f620e13");
//		strings.add("4ef046f12c4117a38f6185c0");
//		myServer.deleteById(strings);
//		myServer.commit(false, false);
//		myServer.optimize(true, false);
//	}
//	
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	public void testQuery() {
//		Map map = new HashMap();
//		map.put(CommonParams.Q, "法国");
//		map.put(CommonParams.START, "0");
//		map.put(CommonParams.ROWS, "10");
//		map.put(CommonParams.FQ, "word:卢浮宫");
//		SolrParams params = new MapSolrParams(map);
//		List<Map<String, Object>> results = myServer.query(params, new String[] {"_id", "title", "url"});
//		assertEquals(10, results.size());
//	}
}
