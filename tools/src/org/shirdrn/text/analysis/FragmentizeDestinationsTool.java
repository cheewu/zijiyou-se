package org.shirdrn.text.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.shirdrn.common.DBConfig;
import org.shirdrn.common.MongodbAccesser;

import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.analysis.ComplexAnalyzer;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Article analysis tool.
 * 
 * @author shirdrn
 * @date 2011-12-13
 */
public class FragmentizeDestinationsTool {

	private static final Logger LOG = Logger.getLogger(FragmentizeDestinationsTool.class);
	private Analyzer analyzer;
	private String dictPath;
	private String wordsPath;
	/** 自定义关键字词典：通常是根据需要进行定向整理的（目的地=>景点关键词集合） */
	Map<String, Set<String>> destinationWithAttractions = new HashMap<String, Set<String>>();
	/** 自定义关键字词典：通常是根据需要进行定向整理的（景点关键词集合） */
	private Set<String> attractionWordSet = new HashSet<String>();
	/** 自定义关键字词典：通常是根据需要进行定向整理的（目的地关键词集合） */
	private Set<String> destinationWordSet = new HashSet<String>();
	private DBConfig articleConfig;
	private MongodbAccesser accesser;
	/** 图片标签正则 */
	private static Pattern picturePattern = Pattern.compile("(<img[^>]+>)");
	/** 最小段落内容长度限制 */
	private int globalMinParagraphLength = 3;
	/** 最大段落间隔数限制 */
	private int globalMaxParagraphGap = 3;
	/** 截取片段中的段落数，必须大于1 */
	private int globalMinFragmentParagraphsCount = 3;
	/** 截取最大片段数限制，必须大于1 */
	private int globalMinFragmentSize = 4;
	/** 一个word在一篇文章的片段集合，在整篇文章中的覆盖率：如果大于该值，则选择整篇文章作为一个片段，否则分别取出多个片段 */
	private double globalCoverage = 0.75;
	

	public FragmentizeDestinationsTool(DBConfig articleConfig) {
		this.articleConfig = articleConfig;
		accesser = new MongodbAccesser(articleConfig);
	}

	/**
	 * 初始化：初始化Lucene分析器，并加载词典
	 */
	public void initialize() {
		analyzer = new ComplexAnalyzer(Dictionary.getInstance(dictPath));
		try {
			BufferedReader reader = new BufferedReader(new FileReader(this.wordsPath));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (!line.isEmpty()) {
					if(line.indexOf("=")!=-1) {
						String[] daa = line.split("=");
						String destination = daa[0];
						Set<String> set = destinationWithAttractions.get(destination);
						if(set==null) {
							set = new HashSet<String>();
							destinationWordSet.add(destination.trim());
							destinationWithAttractions.put(destination.trim(), set);
						}
						String[] aa = daa[1].split(",");
						for(String attr : aa) {
							attractionWordSet.add(attr.trim());
							set.add(attr.trim());
						}
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 分析程序入口主驱动方法
	 * 
	 * @param conditions
	 * @param config
	 */
	public void runWork(Map<String, Object> conditions, DBConfig config) {
		MongodbAccesser newAccesser = new MongodbAccesser(config);
		DBCollection newCollection = newAccesser.getDBCollection(config.getCollectionName());

		DBCollection collection = accesser.getDBCollection(articleConfig.getCollectionName());
		DBObject q = new BasicDBObject();
		if (conditions != null && !conditions.isEmpty()) {
			q = new BasicDBObject(conditions);
		}
		DBCursor cursor = collection.find(q);
		StringBuffer words = new StringBuffer();
		while (cursor.hasNext()) {
			try {
				DBObject result = cursor.next();
				// if(!result.get("_id").toString().equals("4ed31f1ef776481ed002e777"))
				// {
				// continue;
				// }

				// if(!result.get("_id").toString().equals("4ed3198cf776481ed0023c20"))
				// {
				// continue;
				// }

				ParagraphsAnalyzer pa = new ParagraphsAnalyzer(result);
				List<LinkedHashMap<String, Object>> all = pa.analyze();
				if (all !=null && !all.isEmpty()) {
					for (LinkedHashMap<String, Object> m : all) {
						newCollection.insert(new BasicDBObject(m));
						LOG.info("Insert: " + m.get("articleId") + ", " + m.get("destination") + ", " + m.get("fragmentSize") + "/" + m.get("paragraphCount") + ", " + m.get("selected"));
					}
				} else {
					// LOG.info("Skip: " + result.get("_id") + ", " +
					// result.get("title"));
				}
				words.delete(0, words.length());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		cursor.close();
	}

	public void setWordsPath(String wordsPath) {
		this.wordsPath = wordsPath;
	}

	public void setDictPath(String dictPath) {
		this.dictPath = dictPath;
	}

	public void setGlobalMinParagraphLength(int globalMinParagraphLength) {
		this.globalMinParagraphLength = globalMinParagraphLength;
	}

	public void setGlobalMaxParagraphGap(int globalMaxParagraphGap) {
		this.globalMaxParagraphGap = globalMaxParagraphGap;
	}

	public void setGlobalMinFragmentParagraphsCount(int globalMinFragmentParagraphsCount) {
		this.globalMinFragmentParagraphsCount = globalMinFragmentParagraphsCount;
	}

	public void setGlobalMinFragmentSize(int globalMinFragmentSize) {
		this.globalMinFragmentSize = globalMinFragmentSize;
	}

	public void setGlobalCoverage(double globalCoverage) {
		this.globalCoverage = globalCoverage;
	}

	class ParagraphsAnalyzer {
		/** 一条记录 */
		private DBObject result;

		// 下面集合中涉及到Term的，是基于给定的数据（如手工整理的景点或目的地）过滤得到的
		/** LinkedHashMap<文章段落序号, 文章段落内容> */
		private LinkedHashMap<Integer, String> paragraphMap = new LinkedHashMap<Integer, String>();
		/** LinkedHashMap<文章段落序号, 段落分词后目的地Term组> */
		private LinkedHashMap<Integer, Set<String>> paragraphDestinationTermMap = new LinkedHashMap<Integer, Set<String>>();
		/** LinkedHashMap<文章段落序号, 段落分词后景点关键词Term组> */
		private LinkedHashMap<Integer, Set<String>> paragraphAttractionTermMap = new LinkedHashMap<Integer, Set<String>>();
		/** 文章全部段落分词后得到的目的地Term及其TF集合 */
		private Map<String, IntCounter> allDestinationTermsCountMap = new HashMap<String, IntCounter>();
		/** 文章全部段落分词后得到的景点关键词Term及其TF集合 */
		private Map<String, IntCounter> allAttractionTermsCountMap = new HashMap<String, IntCounter>();
		/** Map<文章段落编号, 段落长度> */
		private Map<Integer, Integer> paragraphLenMap = new HashMap<Integer, Integer>();

		// 一些限制参数配置，考虑局部可以动态调整，如果外部进行了全局设置，可以根据局部来动态调整
		/** 最小段落内容长度限制 */
		private int minParagraphLength = 3;
		/** 最大段落间隔数限制：必须大于2 */
		private int maxParagraphGap = 2;
		/** 截取片段中的段落数，必须大于1 */
		private int minFragmentParagraphsCount = 3;
		/** 截取最大片段数限制，必须大于1 */
		private int minFragmentSize = 5;
		/** 一个word在一篇文章的片段集合，在整篇文章中的覆盖率：如果大于该值，则选择整篇文章作为一个片段，否则分别取出多个片段 */
		private double coverage = 0.75;
		
		/** 文章内容长度 */
		private int contentLength;
		

		public ParagraphsAnalyzer(DBObject result) {
			super();
			this.result = result;
			this.minParagraphLength = globalMinParagraphLength;
			this.maxParagraphGap = globalMaxParagraphGap;
			this.minFragmentParagraphsCount = globalMinFragmentParagraphsCount;
			this.minFragmentSize = globalMinFragmentSize;
			this.coverage = globalCoverage;
			beforeAnalysis();
		}

		private void beforeAnalysis() {
			// TODO
			// 这里可做一些预分析工作：根据一篇文章的整体段落情况，确定全局适应的参数，如minParagraphLength、maxParagraphGap
			// 文章段落的连贯性可以考虑（maxParagraphGap），如果两个段落之间文字数量小于设定的阙值则认为是连贯的，可以直接作为一个相关的片段抽取出来

			// TODO
			// 可以根据预分析结果，适当调整局部minParagraphLength、maxParagraphGap、minFragmentSize、coverage

		}

		public List<LinkedHashMap<String, Object>> analyze() {
			String content = (String) result.get("content");
			if(content==null) {
				return null;
			}
			String[] paragraphs = content.split("\n+");
			int paragraphIdCounter = 0;
			for (int i = 0; i < paragraphs.length; i++) {
				if (!paragraphs[i].isEmpty()) {
					int len = paragraphs[i].trim().length();
					contentLength += len;
					paragraphLenMap.put(paragraphIdCounter, len);
					paragraphMap.put(paragraphIdCounter, paragraphs[i].trim());
					if (paragraphs[i].trim().length() > minParagraphLength) {
						analyzeParagraph(paragraphIdCounter, paragraphs[i].trim());
					}
					paragraphIdCounter++;
				}
			}

			// 每一个段落分别处理完成后，综合个段落数据信息，进行综合分析
			// 此时，可用的数据集合如下：
			// 1. paragraphMap 整篇文章各个段落：Map<文章段落序号, 文章段落内容>
			// 2. allTermsCountMap 文章全部段落分词后得到的Term及其TF集合
			// 3. paragraphTermMap 整篇文章各个段落：Map<文章段落序号, 段落分词后Term组>
			// 4. paragraphLenMap 整篇文章各个段落：Map<文章段落序号, 段落长度>

			Map<String, Object> raw = compute(paragraphs.length);
			// 处理抽取出来的片段，组织好后存储到数据库
			List<LinkedHashMap<String, Object>> forStore = new ArrayList<LinkedHashMap<String, Object>>();
			List<Fragment> fragmentList = (List<Fragment>) raw.remove("fragment");
			if (fragmentList != null) {
				for (Fragment frag : fragmentList) {
					LinkedHashMap<String, Object> record = new LinkedHashMap<String, Object>();

					record.put("articleId", result.get("_id").toString());
					record.put("title", result.get("title"));
					record.put("url", result.get("url"));
					record.put("spiderName", result.get("spiderName"));
					record.put("publishDate", result.get("publishDate"));
					record.put("destination", frag.word);
					
					String attractions = makeAttractionKeywords(frag.word);
					record.put("attractions", attractions);
					
					String selectedAttractions = selectAttractions(frag);
					record.put("selectedAttractionCount", frag.selectedAttractionCount);
					record.put("selectedAttractions", selectedAttractions);
					
					String selectedDstAttractions = selectDstAttractions(frag);
					record.put("dstAttractionCount", frag.selectedDstAttractionCount); // 目的地所包含的属于该目的地的景点数
					record.put("dstAttractions", selectedDstAttractions); // 目的地所包含的属于该目的地的景点集合

					StringBuffer selectedIdBuffer = new StringBuffer();
					for (int paragraphId : frag.paragraphIdList) {
						selectedIdBuffer.append(paragraphId).append(" ");
					}
					record.put("paragraphCount", raw.get("paragraphCount"));
					record.put("fragmentSize", frag.end - frag.start + 1);
					record.put("selectedParagraphCount", frag.paragraphIdList.size());
					record.put("selected", selectedIdBuffer.toString().trim());

					// 将连续的段落内容拼接在一起
					LinkedHashMap<Integer, String> selected = new LinkedHashMap<Integer, String>();
					int start = frag.start;
					int end = frag.end;
					int pictureCount = 0;
					for (int i = start; i <= end; i++) {
						pictureCount += countPictures(paragraphMap.get(i));
						selected.put(i, paragraphMap.get(i));
					}
					record.put("pictureCount", pictureCount);
					record.put("fragment", selected);

//					record.put("paragraphs", raw.get("paragraphs"));

					// 最终需要存储的记录集合
					forStore.add(record);
				}
			}
			return forStore;
		}
		
		/**
		 * 计算一个片段中包含该目的地景点关键字的集合：景点属于该目的地
		 * @param frag
		 * @return
		 */
		private String selectDstAttractions(Fragment frag) {
			Set<String> set = new HashSet<String>();
			StringBuffer selected = new StringBuffer();
			for(String att : frag.selectedAttractionSet) {
				if(!set.contains(att) && destinationWithAttractions.get(frag.word).contains(att)) {
					set.add(att);
					selected.append(att).append(",");
				}
			}
			frag.selectedDstAttractionSet = set;
			frag.selectedDstAttractionCount = set.size();
			String s = selected.toString().trim();
			// 去掉结尾的逗号
			return s.substring(0, s.length()-1);
		}
		
		/**
		 * 计算一个片段中包含景点关键字的集合：景点可能不属于该目的地
		 * @param frag
		 * @return
		 */
		private String selectAttractions(Fragment frag) {
			Set<String> set = new HashSet<String>();
			StringBuffer selected = new StringBuffer();
			for (int i = frag.start; i <= frag.end; i++) {
				Set<String> atts = paragraphAttractionTermMap.get(i);
				// 某些段落可能不含有任何景点关键词
				if(atts!=null) {
					for(String att : atts) {
						// 去重重复的景点关键词
						if(!set.contains(att)) {
							set.add(att);
							selected.append(att).append(",");
						}
					}
				}
			}
			frag.selectedAttractionSet = set;
			frag.selectedAttractionCount = set.size();
			String s = selected.toString().trim();
			// 去掉结尾的逗号
			return s.substring(0, s.length()-1);
		}
		
		/**
		 * 计算一个目的地包含的全部景点集合
		 * @param word
		 * @return
		 */
		private String makeAttractionKeywords(String word) {
			StringBuffer buffer = new StringBuffer();
			for(String keyword : destinationWithAttractions.get(word)) {
				buffer.append(keyword).append(",");
			}
			String s = buffer.toString().trim();
			// 去掉结尾的逗号
			return s.substring(0, s.length()-1);
		}

		private int countPictures(String string) {
			int n = 0;
			Matcher matcher = picturePattern.matcher(string);
			while(matcher.find() && ++n>=0);
			return n;
		}

		/**
		 * 分析文章的一个段落：为分析整篇文章，准备各个段落相关的元数据
		 * 
		 * @param paragraphId
		 * @param paragraph
		 */
		private void analyzeParagraph(int paragraphId, String paragraph) {
			Set<String> attractionSet = new HashSet<String>();
			Set<String> destinationSet = new HashSet<String>();
			// 对一个段落的文本内容进行分词处理
			Reader reader = new StringReader(paragraph.trim());
			TokenStream ts = analyzer.tokenStream("", reader);
			ts.addAttribute(CharTermAttribute.class);
			try {
				while (ts.incrementToken()) {
					CharTermAttributeImpl attr = (CharTermAttributeImpl) ts.getAttribute(CharTermAttribute.class);
					String word = attr.toString().trim();
					if (word.length() > 1) {
						if(attractionWordSet.contains(word)) {
							if(allAttractionTermsCountMap.containsKey(word)) {
								++allAttractionTermsCountMap.get(word).value;
							} else {
								allAttractionTermsCountMap.put(word, new IntCounter(1));
							}
							attractionSet.add(word);
						}
						if(destinationWordSet.contains(word)) {
							if(allDestinationTermsCountMap.containsKey(word)) {
								++allDestinationTermsCountMap.get(word).value;
							} else {
								allDestinationTermsCountMap.put(word, new IntCounter(1));
							}
							destinationSet.add(word);
						}
					}
				}
				paragraphDestinationTermMap.put(paragraphId, destinationSet);
				paragraphAttractionTermMap.put(paragraphId, attractionSet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private Optimizer optimizer;

		private Map<String, Object> compute(int paragraphCount) {
			// 返回一篇文章中，每个word对应段落的集合
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("paragraphs", paragraphMap);
			result.put("paragraphCount", paragraphCount);
			// 这个列表中的每个Fragment对应着最终需要存储的记录
			List<Fragment> fragmentList = new ArrayList<Fragment>();
			// 如果需要排序，可以在这里按照目的地集合allDestinationTermsCountMap的Term的TF降序排序

			Iterator<Entry<String, IntCounter>> iter = allDestinationTermsCountMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, IntCounter> entry = iter.next();
				// 对每个Term，遍历整篇文章，对Term的分布进行分析
				String destination = entry.getKey();
				IntCounter freq = entry.getValue();
				// 如果需要，可以进行剪枝，降低无用的计算（如根据TF，TF在一篇文章太低，可以直接过滤掉）
				// 这里剪枝条件设置为TF>1
				if (freq.value > 1) {
					// 获取到一个word在一篇文章哪些段落中出现过
					List<Integer> paragraphIdList = choose(destination, paragraphCount);
					// 提取一个word相关的段落集合，应该考虑如下因素：
					// 1. 一篇文章的段落总数
					// 2. 一个word出现的各个段落之间间隔数
					// 3. 获取多少个段落能够以word为中心
					List<Integer> list = new ArrayList<Integer>();
					List<Fragment> temp = new ArrayList<Fragment>();
					if (paragraphIdList.size() >= Math.max(2, minFragmentParagraphsCount)) {
						// 包含段落太少的文章，直接过滤掉

						// 根据包含word的段落序号列表，计算一篇文章中有多个片段的集合
						int previous = paragraphIdList.get(0);
						list.add(previous);
						for (int i = 1; i < paragraphIdList.size(); i++) {
							int current = paragraphIdList.get(i);
							if (current - previous <= maxParagraphGap) {
								list.add(current);
							} else {
								makeFragment(destination, list, temp);
								list = new ArrayList<Integer>();
								list.add(current);
							}
							previous = current;
						}
						if (!list.isEmpty()) {
							makeFragment(destination, list, temp);
						}
					}

					// 一个关键词，在一篇文章中，根据分析后的结果选择抽取的片段集合
					if (!temp.isEmpty()) {
						// 这里对多个片段Fragment的集合进行综合分析、优化处理
						optimizer = new CoverageOptimizer();
						temp = optimizer.optimize(destination, temp, paragraphCount);
						if(temp!=null && !temp.isEmpty()) {
							fragmentList.addAll(temp);
						}
					}
				}
			}
			// 返回抽取到的片段集合
			// 在存储到数据库之前，先要对其进行拆分、组合、优化
			result.put("fragment", fragmentList);
			return result;
		}

		private void makeFragment(String word, List<Integer> list, List<Fragment> temp) {
			Fragment frag = new Fragment(word, list);
			frag.start = list.get(0);
			frag.end = list.get(list.size() - 1);
			temp.add(frag);
		}

		/**
		 * 处理一个Term在整篇文章中的分布情况
		 * 
		 * @param destination
		 * @param paragraphCount
		 * @return
		 */
		private List<Integer> choose(String destination, int paragraphCount) {
			// 遍历每一个段落的景点关键词Term集合paragraphAttractionTermMap，获取包含该word的段落序号集合
			List<Integer> paragraphIdList = new ArrayList<Integer>();
			Set<String> attractionKeywordsPerDestination = destinationWithAttractions.get(destination);
			for (Entry<Integer, Set<String>> entry : paragraphAttractionTermMap.entrySet()) {
				// 迭代每一段的景点关键词
				Set<String> attractionKeywordsPerParagraph = entry.getValue();
				for (String keyword : attractionKeywordsPerDestination) {
					if(attractionKeywordsPerParagraph.contains(keyword)) {
						paragraphIdList.add(entry.getKey()); // 保存段落序号
						break;
					}
				}
			}
			return paragraphIdList;
		}

		class CoverageOptimizer implements Optimizer {

			@Override
			public List<Fragment> optimize(String word, List<Fragment> temp, int paragraphCount) {
				// 考虑一个word在文章中截取的片段，在整篇文章中的覆盖情况
				List<Fragment> selectedFragmentList = null;
				double sum = 0.0;
				for (Fragment f : temp) {
					sum += (f.end - f.start + 1); // 累加片段的长度
				}
				double rate = sum / (double) paragraphCount;
				// 满足覆盖条件
				// TODO 可以基于覆盖条件，再考虑抽取段落在整篇文章中的分布，如果分布均匀，酌情降低覆盖率以选取整篇文章为一个段落
				if (rate >= coverage) {
					selectedFragmentList = updateFragment(word, temp, paragraphCount);
				} else {
					// 不满足覆盖率条件，看一下全部片段的段落集合在整篇文章中的分布情况
					// 考虑分布：将整篇文章分成几个连续片段的集合，看我们计算关键词的得到的片段集合，是否与连续片段集合发生重叠
					Fragment first = temp.get(0);
					Fragment last = temp.get(temp.size()-1);
					// 这个条件有点太弱，如果一篇文章中段落很多，开始部分一段，结尾部分一段，结果就会取整篇文章
					// TODO 所以还要考虑覆盖率，降低覆盖率的条件，但是提高文章边界价差限制，覆盖率初步定为文章段落数量的一半						
					if(rate >= 0.5 && first.paragraphIdList.get(0) - 0 < maxParagraphGap 
							&& paragraphCount - last.paragraphIdList.get(last.paragraphIdList.size() - 1) < maxParagraphGap) {
						return updateFragment(word, temp, paragraphCount);
					}
					
					// 用于优化：如果段落很少，很有可能这篇文章出现关键词段落数覆盖率很低，但是这篇文章确实是讲该关键词表达的主题
					// TODO 是否可以考虑做一个分段函数y = f(x, y, z), x:片段数 y:文章段落数 z:文章文本总长度
					if(temp.size()==1 && paragraphCount<10) {
						return updateFragment(word, temp, paragraphCount);
					}
					
					// TODO 如果一个片段，片段起始段落距离文章首段距离大于maxParagraphGap，可以考虑计算一下这段间隔的文字数量来进行优化
					// 文章末尾段落也可以类似考虑，暂时不做
					// do something
					
					// 需要对各个片段进行筛选
					for (Iterator<Fragment> iter = temp.iterator(); iter.hasNext();) {
						// 片段包含的段落太少
						if (iter.next().paragraphIdList.size() < minFragmentSize) {
							iter.remove();
						}
					}
					return temp;
				}
				// TODO 可以考虑文章段落中出现的多组图片，导致文章段落衔接隔断问题
				return selectedFragmentList;
			}

			private List<Fragment> updateFragment(String word, List<Fragment> temp, int paragraphCount) {
				List<Fragment> selectedFragmentList = new ArrayList<Fragment>();
				List<Integer> list = new ArrayList<Integer>();
				for (Fragment f : temp) {
					list.addAll(f.paragraphIdList);
				}
				Fragment f = new Fragment(word, list);
				f.start = 0;
				f.end = paragraphCount-1;
				selectedFragmentList.add(f);
				return selectedFragmentList;
			}
		}
		
		class DistributionOptimizer implements Optimizer {

			@Override
			public List<Fragment> optimize(String word, List<Fragment> temp, int paragraphCount) {
				List<Fragment> selectedFragmentList = new ArrayList<Fragment>();
				// write your logic
				return selectedFragmentList;
			}
		}
	}

	/**
	 * 片段选择优化器
	 * 
	 * @author shirdrn
	 * @date 2011-12-19
	 */
	interface Optimizer {
		public List<Fragment> optimize(String word, List<Fragment> temp, int paragraphCount);
	}

	/**
	 * 计数器：为了对Map中数据对象计数方便
	 * 
	 * @author shirdrn
	 * @date 2011-12-15
	 */
	class IntCounter {
		Integer value;

		public IntCounter() {
			this(0);
		}

		public IntCounter(Integer value) {
			super();
			this.value = value;
		}

		@Override
		public String toString() {
			return value == null ? "0" : value.toString();
		}
	}

	/**
	 * 文章的一个片段：由一个或多个段落组成
	 * 
	 * @author shirdrn
	 * @date 2011-12-15
	 */
	class Fragment {
		/** 关键词 */
		String word;
		/** 经过分析后，最终确定的片段截取起始段落序号 */
		int start;
		/** 经过分析后，最终确定的片段截取终止段落序号 */
		int end;
		/** 出现word的段落的序号列表 */
		List<Integer> paragraphIdList;
		/** 出现景点关键词集合 */
		Set<String> selectedAttractionSet;
		/** 出现景点关键词的数量 */
		int selectedAttractionCount;
		/** 出现景点关键词集合：景点属于该目的地 */
		Set<String> selectedDstAttractionSet;
		/** 出现景点关键词的数量：景点属于该目的地 */
		int selectedDstAttractionCount;

		public Fragment() {
			super();
		}

		public Fragment(String word, List<Integer> paragraphIdList) {
			super();
			this.word = word;
			this.paragraphIdList = paragraphIdList;
		}

		@Override
		public String toString() {
			return "[word=" + word + ", start=" + start + ", end=" + end + ", paragraphIdList=" + paragraphIdList + "]";
		}
	}
	
	public static void main(String[] args) {
		int i = 0;
		Pattern picturePattern = Pattern.compile("<img[^>]+>");
		Matcher m = picturePattern.matcher("<img >jkjkj<img  >jkl<img >");
		while(m.find() && ++i>=0);
		System.out.println(i);
	}
}
