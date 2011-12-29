package org.shirdrn.text.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.shirdrn.common.DBConfig;

public class FragmentizeAttractionsToolTest extends TestCase {

	String indexPath;
	FragmentizeAttractionsTool tool;
	DBConfig dictLoaderConfig;
	DBConfig articleConfig;
	
	@Override
	protected void setUp() throws Exception {
		articleConfig = new DBConfig("192.168.0.184", 27017, "page", "Article");
		tool = new FragmentizeAttractionsTool(articleConfig);
	}
	
//	public void testTravelsAnalyzeToolWithAttractions() {
//		if(true) {
//			tool.setWordsPath("E:\\words-attractions.dic");
//			tool.setDictPath("E:\\Develop\\eclipse-jee-helios-win32\\workspace\\EasyTool\\dict");
//			tool.initialize();
//			Map<String, Object> conditions = new HashMap<String, Object>();
////			conditions.put("spiderName", "sinaSpider");
//			DBConfig config = new DBConfig("192.168.0.184", 27017, "tripfm", "pageAttractions");
//			tool.runWork(conditions, config);
//		}
//	}
	
//	public void testTravelsAnalyzeToolWithDestinations() {
//		if(true) {
//			tool.setWordsPath("E:\\words-destinations.dic");
//			tool.setDictPath("E:\\Develop\\eclipse-jee-helios-win32\\workspace\\EasyTool\\dict");
//			tool.initialize();
//			Map<String, Object> conditions = new HashMap<String, Object>();
////			conditions.put("spiderName", "sinaSpider");
//			DBConfig config = new DBConfig("192.168.0.184", 27017, "tripfm", "pageDestinations");
//			tool.runWork(conditions, config);
//		}
//	}
	
	public void testArticlesAnalyzeTool() {
		if(true) {
//			tool.setWordsPath("E:\\words-destinations-and-attractions.dic");
			tool.setWordsPath("E:\\words-attractions.dic");
			tool.setDictPath("E:\\Develop\\eclipse-jee-helios-win32\\workspace\\EasyTool\\dict");
			tool.initialize();
			tool.setGlobalMaxParagraphGap(5);
			tool.setGlobalMinFragmentParagraphsCount(3);
			tool.setGlobalMinParagraphLength(3);
			tool.setGlobalMinFragmentSize(3);
			tool.setGlobalCoverage(0.65);
			
			Map<String, Object> spiderToCollection = new HashMap<String, Object>();
			spiderToCollection.put("mafengwoSpider", "mafengwo");
			spiderToCollection.put("go2euSpider", "go2eu");
			spiderToCollection.put("daodaoSpider", "daodao");
			spiderToCollection.put("lotourSpider", "lotour");
			spiderToCollection.put("17uSpider", "17u");
			spiderToCollection.put("lvpingSpider", "lvping");
			spiderToCollection.put("sinaSpider", "sina");
			spiderToCollection.put("sohuSpider", "sohu");
			spiderToCollection.put("baseSeSpider", "baseSe");
			spiderToCollection.put("bytravelSpider", "bytravel");
			for (Iterator<Map.Entry<String, Object>> iterator = spiderToCollection.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry<String, Object> entry = iterator.next();
				Map<String, Object> conditions = new HashMap<String, Object>();
				conditions.put("spiderName", entry.getKey());
				DBConfig config = new DBConfig("192.168.0.184", 27017, "fragment", (String) entry.getValue());
				tool.runWork(conditions, config);
			}
		}
	}
}
