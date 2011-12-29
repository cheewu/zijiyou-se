package org.shirdrn.util;

public interface ConfigParams {
	/** query and init param for query fields */
	public static String QF = "qf";
	/** destination query fields */
	public static String DQF = "dqf";
	/** attraction query fields */
	public static String AQF = "aqf";
	/** query and init param for must match query fields */
	public static String QF_MUST = "qf.must";
	/** payload field param name */
	public  static final String PAYLOAD_FIELDS_PARAM_NAME = "plf";
	
	///////////////////// Category parameters ///////////////
	public static String KEEP_CATEGORY = "cat";
	/** boost for AND or/and OR lowlevel query */
	public static String CATEGORY_BOOST = "cat.boost";
	
	///////////////////// AND OR paramters ///////////////////
	public static String KEEP_AND_OR = "ao";
	public static String MAIN_BOOST = "ao.boost.main";
	public static String FRONT_BOOST = "ao.boost.front";
	public static String REAR_BOOST = "ao.boost.rear";
	
	///////////////////// Travels parameters /////////////////
	public static String KEEP_TRAVELS = "tr";
	public static String TRAVELS_KEYWORDS = "tr.keywords";
	public static String TRAVELS_BOOST = "tr.boost";
	
	///////////////////// Extendable paramters ///////////////
	public static String DATA_LOADER = "ext.dataLoader";
	/** using extendable query booster? */
	public static String KEEP_EXT = "ext";
	/** if using extendable, then getting the extended file content */
	public static String EXT_FILE = "ext.file";
	/** if using extendable, set the word count being extended */
	public static String EXT_MAX_WORD_COUNT = "ext.maxWordCount";
	public static Object CORE_NAME = "coreName";
	
	/** single term text's boost */
	public static String BOOST = "boost";
	
	/** Mongo database configration */
	public static String MONGO_HOST = "mongo.host";
	public static String MONGO_PORT = "mongo.port";
	public static String MONGO_DB = "mongo.db";
	public static String MONGO_COLLECTION = "mongo.collection";
	
	public static String MAX_ATTRACTION_COUNT = "maxAttractionCount";
}
