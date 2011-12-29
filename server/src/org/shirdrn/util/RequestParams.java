package org.shirdrn.util;

public interface RequestParams {
	
	/** 分类：category，用于过滤、提供payload值  */
	public static final String CATEGORY = "zct";
	/** 季节：season，用于过滤、提供payload值 */
	public static final String SEASON = "ssn";
	/** 分面日期范围：facet date range，用于时间范围分段查询 */
	public static final String FACET_DATE_RANGE = "fdr";
	/** 日期范围：dr，用于按照某个时间范围过滤查询 */
	public static final String DATE_RANGE = "dr";
	
}
