package org.shirdrn.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.shirdrn.common.DBConfig;
import org.shirdrn.common.MongodbAccesser;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * 导出目的地相关景点的映射关系（根据rank进行排序）：
 * 每行的格式形如：
 * 目的地=>景点1,景点2,景点3
 * 例如：
 * 北京=>天安门,故宫,天坛,颐和园,圆明园,王府井
 * 
 * @author shirdrn
 * @date   2011-12-27
 */
public class DestinationExtendableFromPOIHelper {

	MongodbAccesser accesser;
	DBConfig config;
	Map<String, TreeSet<RankedWord>> mappings = new HashMap<String, TreeSet<RankedWord>>();
	
	public DestinationExtendableFromPOIHelper(DBConfig config) {
		this.config = config;
		accesser = new MongodbAccesser(config);
	}
	
	private DBObject descOrderBy = new BasicDBObject(); 
	@SuppressWarnings("rawtypes")
	public void output(String outputPath) {
		DBCollection collection = accesser.getDBCollection(config.getCollectionName());
		BasicDBObject q = new BasicDBObject();
		q.put("category", "attraction");
		DBCursor cursor = collection.find(q);
//		descOrderBy.put("area", 1);
//		descOrderBy.put("rank", -1);
//		cursor.sort(descOrderBy);
		while (cursor.hasNext()) {
			Map map = cursor.next().toMap();
			String area = (String) map.get("area");
			String attractionKeywords = (String) map.get("keyword");
			if(map.get("rank")==null) {
				continue;
			}
			double rank = (Double) map.get("rank");
			if(area!=null && attractionKeywords!=null && !area.isEmpty() && !attractionKeywords.isEmpty()) {
				TreeSet<RankedWord> set = mappings.get(area);
				if(set==null) {
					set = new TreeSet<RankedWord>();
					mappings.put(area.trim(), set);
				}
				String[] a = attractionKeywords.trim().split(",");
				for(String k : a) {
					if(!k.isEmpty()) {
						set.add(new RankedWord(k.trim(), rank));
					}
				}
			}
		}
		cursor.close();
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(outputPath)));
			Iterator<Map.Entry<String, TreeSet<RankedWord>>> iter = mappings.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, TreeSet<RankedWord>> entry = iter.next();
				writer.write(entry.getKey());
				writer.write("=>");
				int n = 0;
				for(RankedWord attr : entry.getValue()) {
					writer.write(attr.keyword);
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
	
	class RankedWord implements Comparable<RankedWord>{
		String keyword;
		double rank;
		public RankedWord(String keyword, double rank) {
			super();
			this.keyword = keyword;
			this.rank = rank;
		}
		@Override
		public boolean equals(Object obj) {
			RankedWord o = (RankedWord) obj;
			return o.keyword.equals(keyword) && o.rank==rank;
		}
		@Override
		public int compareTo(RankedWord o) {
			if(rank < o.rank) {
				return 1;
			}
			if(rank > o.rank) {
				return -1;
			}
			return 0;
		}
	}
}
