package org.shirdrn.text.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.shirdrn.common.DBConfig;

public class FragmentizeDestinationsToolTest extends TestCase {

	String indexPath;
	FragmentizeDestinationsTool tool;
	DBConfig dictLoaderConfig;
	DBConfig articleConfig;
	
	@Override
	protected void setUp() throws Exception {
		articleConfig = new DBConfig("192.168.0.184", 27017, "page", "Articleback1221");
		tool = new FragmentizeDestinationsTool(articleConfig);
	}
	
	public void testArticlesAnalyzeTool() {
		if(true) {
			tool.setWordsPath("E:\\words-destinations-with-attractions.dic");
			tool.setDictPath("E:\\Develop\\eclipse-jee-helios-win32\\workspace\\EasyTool\\dict");
			tool.initialize();
			tool.setGlobalMaxParagraphGap(8);
			tool.setGlobalMinFragmentParagraphsCount(10);
			tool.setGlobalMinParagraphLength(1);
			tool.setGlobalMinFragmentSize(10);
			tool.setGlobalCoverage(0.50);
			
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
				DBConfig config = new DBConfig("192.168.0.184", 27017, "fragmentDst", (String) entry.getValue());
				tool.runWork(conditions, config);
			}
		}
	}
}
