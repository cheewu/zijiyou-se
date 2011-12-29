package org.shirdrn.solr.search;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.payloads.AveragePayloadFunction;
import org.apache.lucene.search.payloads.PayloadFunction;
import org.apache.lucene.search.payloads.PayloadTermQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DefaultSolrParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrQueryParser;
import org.apache.solr.util.SolrPluginUtils;
import org.shirdrn.solr.search.ext.DestinationExtendableQueryBooster;
import org.shirdrn.solr.search.ext.ExtendableQueryBooster;
import org.shirdrn.util.ClassLoaderUtils;
import org.shirdrn.util.ConfigParams;
import org.shirdrn.util.RequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For destination query:
 * query parser based Lucene query syntax. We assume the query
 * string is a {@link Term} in our indexes, or are some {@link Term}s with
 * specific operate logic.
 * 
 * @author shirdrn
 * @date 2011-12-28
 */
public class DestinationQParser extends QParser {
	private final Logger LOG = LoggerFactory.getLogger(DestinationQParser.class);
	private SolrQueryParser lparser;
	private QueryType queryType = QueryType.SINGLE_TERM_TEXT;
	/** request contains category parameter? */
	private boolean hasCcategory = false;
	
	private static final PayloadFunction payloadFunction = new AveragePayloadFunction(); 
	private static Map<String, Float> payloadFields = new HashMap<String, Float>();
	private static Map<String, Float> queryFields = new HashMap<String, Float>();
	/** destination query fields */
	private static Map<String, Float> destinationQueryFields = new HashMap<String, Float>();
	/** attraction query fields */
	private static Map<String, Float> attractionQueryFields = new HashMap<String, Float>();
	private static Float catBoost = 1.0f;
	
	private static Float boost = 1.0f;
	private static Float mainBoost = 1.0f;
	private static Float frontBoost = 1.0f;
	private static Float rearBoost = 1.0f;
	/** Initialize certain global configuration items. */
	private static boolean initialized = false;
	private static DestinationExtendableQueryBooster booster;
	private static ExtendedTravelBooster travelBooster;

	public DestinationQParser(String qstr, SolrParams localParams,
			SolrParams params, SolrQueryRequest req) {
		super(qstr, localParams, params, req);
	}
	
	@Override
	public Query parse() throws ParseException {
		String qstr = getString();
		SolrParams solrParams = localParams == null ? params : new DefaultSolrParams(localParams, params);
		// initialize statically and dynamically
		initialize(solrParams);
		
		BooleanQuery query = new BooleanQuery(true);
		
		// for single term text query
		if(queryType==QueryType.SINGLE_TERM_TEXT) {
			String singleTermText = qstr;
			// destination->title,content(should be configured in schema.xml)
			makeDestinationQuery(query, singleTermText);
			
			if(booster != null) {
				BooleanQuery boostedQuery = booster.getExtendedQuery(singleTermText, boost, attractionQueryFields);
				query.add(boostedQuery, BooleanClause.Occur.MUST);
			} else {
				query.add(createTermQuery(singleTermText, queryFields), BooleanClause.Occur.MUST);
			}
			// extend travel keywords
			if(!hasCcategory && travelBooster!=null && travelBooster.keepTravels) {
				travelBooster.extendTravelsQuery();
				query.add(travelBooster.travelsKeywordsQuery, BooleanClause.Occur.MUST);
			}
		} else {
			// for multiple term text query with OR or AND operators
			String multipleTermText = qstr;
			parseMultipleTermText(query, multipleTermText, solrParams);
		}
		
		// 获取高亮显示功能
//		Query parsed = lparser.parse(query.toString());
		Query parsed = query;
		
		// perform payload boost
		extendCategory(solrParams, parsed);
		// add boost functions
		addBoostFunctions((BooleanQuery) parsed, solrParams);
		if(queryType==QueryType.SINGLE_TERM_TEXT) {
			// rewrite payload qquery
			payloadBoost((BooleanQuery) parsed, qstr);
		}
		return parsed;
	}

	private void makeDestinationQuery(BooleanQuery query, String singleTermText) {
		BooleanQuery bq = new BooleanQuery(true);
		Iterator<Entry<String, Float>> iter = destinationQueryFields.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, Float> entry = iter.next();
			TermQuery q = new TermQuery(new Term(entry.getKey(), singleTermText.trim()));
			q.setBoost(entry.getValue());
			bq.add(q, BooleanClause.Occur.MUST);
		}
		query.add(bq, BooleanClause.Occur.SHOULD);
	}

	private void payloadBoost(BooleanQuery query, String qstr) {
		Iterator<Entry<String, Float>> it = payloadFields.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Float> entry = it.next();
			PayloadTermQuery ptq = new PayloadTermQuery(new Term(entry.getKey(), qstr), payloadFunction);
			ptq.setBoost(entry.getValue());
			query.add(ptq, BooleanClause.Occur.MUST);
		}
	}

	private void extendCategory(SolrParams solrParams, Query parsed) {
		if(hasCcategory) {
			Query plQuery = null;
			if(payloadFields.keySet().contains("category")) {
				plQuery = getPayloadTermQuery(solrParams, RequestParams.CATEGORY, "category");
			} else {
				String termText = solrParams.get(RequestParams.CATEGORY);
				if(termText!=null && !termText.isEmpty()) {
					plQuery = new TermQuery(new Term("category", termText));
				}
			}
			if(plQuery!=null) {
				plQuery.setBoost(catBoost);
				((BooleanQuery) parsed).add(plQuery, BooleanClause.Occur.MUST);
			}
		}
	}
	
	private void initialize(SolrParams solrParams) {
		Initializer initializer = new Initializer();
		initializer.initialize(solrParams);
	}

	protected void addBoostFunctions(BooleanQuery query, SolrParams solrParams)
			throws ParseException {
		String[] boostFuncs = solrParams.getParams(DisMaxParams.BF);
		if (null != boostFuncs && 0 != boostFuncs.length) {
			for (String boostFunc : boostFuncs) {
				if (null == boostFunc || "".equals(boostFunc))
					continue;
				Map<String, Float> ff = SolrPluginUtils.parseFieldBoosts(boostFunc);
				for (String f : ff.keySet()) {
					Query fq = subQuery(f, FunctionQParserPlugin.NAME).parse();
					Float b = ff.get(f);
					if (null != b) {
						fq.setBoost(b);
					}
					query.add(fq, BooleanClause.Occur.SHOULD);
				}
			}
		}
	}
	
	protected PayloadTermQuery getPayloadTermQuery(SolrParams solrParams, String key, String field) {
		PayloadTermQuery plQuery = null;
		String value = solrParams.get(key);
		if(value!=null && !value.isEmpty()) {
			Term term = new Term(field, value);
			plQuery = new PayloadTermQuery(term, payloadFunction);
		}
		return plQuery;
	}

	enum QueryType {
		SINGLE_TERM_TEXT,
		MULTIPLE_TERM_TEXT,
	}
	
	/**
	 * Parse mixing MUST and SHOULD query defined by us, 
	 * e.g. 首都OR北京OR北平AND首博OR首都博物馆
	 * @param query
	 * @param multipleTermText
	 * @param solrParams
	 */
	private void parseMultipleTermText(BooleanQuery query,
			String multipleTermText, SolrParams solrParams) {
		String[] a = multipleTermText.split("\\s*AND\\s*");
		if(a.length>0) {
			BooleanQuery bqAnds = new BooleanQuery(true);
			for (int i = 0; i < a.length; i++) {
				BooleanQuery and = parseOrQuery(a[i]);
				if(i==0) {
					and.setBoost(frontBoost);
				} else {
					and.setBoost(rearBoost);
				}
				bqAnds.add(and, BooleanClause.Occur.MUST);
			}
			bqAnds.setBoost(mainBoost);
			query.add(bqAnds, BooleanClause.Occur.MUST);
		}
	}
	
	private BooleanQuery parseOrQuery(String qsr) {
		BooleanQuery bq = new BooleanQuery(true);
		String[] a = qsr.split("\\s*OR\\s*");
		for(String q : a) {
//			bq.add(booster.getExtendedQuery(q, queryFields), BooleanClause.Occur.SHOULD);
			bq.add(createTermQuery(q, queryFields), BooleanClause.Occur.SHOULD);
		}
		return bq;
	}

	private Query createTermQuery(String q, Map<String, Float> queryFields) {
		BooleanQuery bq = new BooleanQuery(true);
		for(String field : queryFields.keySet()) {
			bq.add(new TermQuery(new Term(field, q)), BooleanClause.Occur.SHOULD);
		}
		return bq;
	}
	
	private void setQueryType(SolrParams solrParams) {
		String q = solrParams.get(CommonParams.Q);
		if(q!=null && (q.indexOf("AND")!=-1 || q.indexOf("OR")!=-1)) {
			queryType = QueryType.MULTIPLE_TERM_TEXT;
		}
	}
	
	/**
	 * Initialize solrconfig.xml parameters statically, 
	 * and initialize the things for a coming request.
	 * @param solrParams
	 */
	private void init(SolrParams solrParams) {
		// set query type
		setQueryType(solrParams);
		// get query fields
		populateQueryFields(solrParams.get(ConfigParams.QF), queryFields);
		if (queryFields.size()==0) {
			queryFields.put(req.getSchema().getDefaultSearchFieldName(), 1.0f);
		}

		String defaultField = getParam(CommonParams.DF);
		if (defaultField == null) {
			defaultField = getReq().getSchema().getDefaultSearchFieldName();
		}
		lparser = new SolrQueryParser(this, defaultField);

		// these could either be checked & set here, or in the
		// SolrQueryParser constructor
		String opParam = getParam(QueryParsing.OP);
		if (opParam != null) {
			lparser.setDefaultOperator("AND".equals(opParam) ? QueryParser.Operator.AND : QueryParser.Operator.OR);
		} else {
			// try to get default operator from schema
			@SuppressWarnings("deprecation")
			QueryParser.Operator operator = getReq().getSchema().getSolrQueryParser(null).getDefaultOperator();
			lparser.setDefaultOperator(null == operator ? QueryParser.Operator.OR : operator);
		}
		
		// has categorize parameter
		String category = solrParams.get(RequestParams.CATEGORY);
		if(category!=null && !category.isEmpty()) {
			hasCcategory = true;
		}
		
		String bst = solrParams.get(ConfigParams.BOOST);
		if(bst!=null && !bst.isEmpty()) {
			boost = parseFloat(bst, boost.floatValue());
		}
	}

	@Override
	public String[] getDefaultHighlightFields() {
		return new String[] { lparser.getField() };
	}
	
	private float parseFloat(String s, float defaultValue) {
		float value = defaultValue;
		if(s!=null) {
			try {
				value = Float.parseFloat(s.trim());
			} catch (Exception e) {}
		}
		return value;
	}
	
	/**
	 * Populate collection with fields and fields' boost value configured.
	 * @param strFields
	 * @param fields
	 */
	private void populateQueryFields(String strFields, Map<String, Float> fields) {
		if(strFields!=null && !strFields.isEmpty()) {
			String[] aPair = strFields.split("\\s+");
			for(String field : aPair) {
				if(strFields.indexOf("^")!=-1) {
					String[] a = field.split("\\^");
					if(a.length==2) {
						fields.put(a[0].trim(), parseFloat(a[1].trim(), 1.0f));
					}
				} else {
					fields.put(field.trim(), 1.0f);
				}
			}
		}
	}
	
	/** Concurrent read-write lock object.  */
	private static Lock locker = new ReentrantLock();
	
	/**
	 * A initializer for preparing instances of configuration.
	 * 
	 * @author shirdrn
	 * @date   2011-11-22
	 */
	class Initializer {
		public void initialize(SolrParams solrParams) {
			try {
				locker.lock();
				if(!initialized) {
					staticInitialize(solrParams);
					initialized = true;
				}
			} catch (Exception e) {
				if(!initialized) {
					throw new RuntimeException("Failed to initialize configuration parameters statically");
				}
			} finally {
				locker.unlock();
			}
			init(solrParams);
		}
		
		/**
		 * Initialize statically only once.
		 * @param solrParams
		 * @throws Exception
		 */
		private void staticInitialize(SolrParams solrParams) {
			// extendable query booster
			initializeExtendableQueryBooster(solrParams);
			// load configured boost value
			boolean isAndOr = solrParams.getBool(ConfigParams.KEEP_AND_OR, false);
			if(isAndOr) {
				mainBoost = parseFloat(solrParams.get(ConfigParams.MAIN_BOOST), mainBoost.floatValue());
				frontBoost = parseFloat(solrParams.get(ConfigParams.FRONT_BOOST), frontBoost.floatValue());
				rearBoost = parseFloat(solrParams.get(ConfigParams.REAR_BOOST), rearBoost.floatValue());
			}
			boolean isCatBoost = solrParams.getBool(ConfigParams.KEEP_CATEGORY, false);
			if(isCatBoost) {
				catBoost = parseFloat(solrParams.get(ConfigParams.CATEGORY_BOOST), catBoost.floatValue());
			}
			// populate fields from configured query parameters.
			populateQueryFields(solrParams.get(ConfigParams.AQF), attractionQueryFields); // attraction query fields
			populateQueryFields(solrParams.get(ConfigParams.DQF), destinationQueryFields); // destination query fields
			populateQueryFields(solrParams.get(ConfigParams.PAYLOAD_FIELDS_PARAM_NAME), payloadFields); // pauload query fields
		}
		
		/**
		 * Initialize the extendable keywords for a query keyword, including
		 * file contents, and {@link ExtendableQueryBooster} instance. And the
		 * extendable travel keywords, refered to {@link ExtendedTravelBooster}
		 * should be parsed.
		 * @param solrParams
		 */
		@SuppressWarnings({ "unchecked" })
		private void initializeExtendableQueryBooster(SolrParams solrParams) {
			boolean isExtendable = solrParams.getBool(ConfigParams.KEEP_EXT, false);
			if (isExtendable) {
				String extFile = solrParams.get(ConfigParams.EXT_FILE, "extfile.txt");
				int maxWordCount = solrParams.getInt(ConfigParams.EXT_MAX_WORD_COUNT, 10);
				String dataLoaderClassName = solrParams.get(ConfigParams.DATA_LOADER, "org.shirdrn.solr.search.ext.dataloader.FileDataLoader");
				try {
					Map conditions = new HashMap();
					conditions.put(ConfigParams.EXT_FILE, extFile);
					conditions.put(ConfigParams.CORE_NAME, req.getCore().getName());
					conditions.put(ConfigParams.DATA_LOADER, ClassLoaderUtils.newInstance(dataLoaderClassName));
					conditions.put(ConfigParams.EXT_MAX_WORD_COUNT, maxWordCount);
					booster = new DestinationExtendableQueryBooster(conditions);
				} catch (Exception e) {
					LOG.warn("New instance of dataLoader, catch exception.");
				}
			}
			if(travelBooster==null) {
				travelBooster = new ExtendedTravelBooster(solrParams);
			}
		}
	}
	
	/**
	 * Extended query based on travel keywords configured in solrconfig.xml
	 * 
	 * @author shirdrn
	 * @date   2011-11-22
	 */
	class ExtendedTravelBooster {
		private Float travelsBoost = 1.0f;
		private BooleanQuery travelsKeywordsQuery;
		private SolrParams solrParams;
		private boolean keepTravels;
		private String keywords;
		
		public ExtendedTravelBooster(SolrParams solrParams) {
			this.solrParams = solrParams;
			this.keepTravels = solrParams.getBool(ConfigParams.KEEP_TRAVELS, false);
			this.keywords = solrParams.get(ConfigParams.TRAVELS_KEYWORDS);
		}
		
		public void extendTravelsQuery() {
			if(!keepTravels || keywords==null || keywords.isEmpty()) {
				return;
			}
			Map<String, Float> travelsKeywords = new HashMap<String, Float>();
			for (String keyword : keywords.trim().split("\\s+")) {
				if (!keyword.isEmpty()) {
					String[] a = keyword.trim().split("\\^");
					try {
						if (keyword.indexOf("^") != -1) {
							travelsKeywords.put(a[0].trim(), Float.parseFloat(a[1].trim()));
						} else {
							travelsKeywords.put(a[0].trim(), 1.0f);
						}
					} catch (NumberFormatException e) {
						travelsKeywords.put(a[0].trim(), 1.0f);
					} catch (Exception e) {
					}
				}
			}
			try {
				travelsBoost = Float.parseFloat(solrParams.get(ConfigParams.TRAVELS_BOOST).trim());
			} catch (Exception e) {}
				 
			// create a extended query
			travelsKeywordsQuery = new BooleanQuery(true);
			Iterator<Entry<String, Float>> iter = travelsKeywords.entrySet().iterator();
			while (iter.hasNext()) {
				BooleanQuery shouldBooleanQuery = new BooleanQuery(true);
				Entry<String, Float> entry = iter.next();
				for (String field : queryFields.keySet()) {
					TermQuery tq = new TermQuery(new Term(field, entry.getKey()));
					tq.setBoost(entry.getValue());
					shouldBooleanQuery.add(tq, BooleanClause.Occur.SHOULD);
				}
				travelsKeywordsQuery.add(shouldBooleanQuery, BooleanClause.Occur.SHOULD);
			}
			travelsKeywordsQuery.setBoost(travelsBoost);
		}
	}
}
