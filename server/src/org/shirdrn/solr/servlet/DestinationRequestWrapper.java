package org.shirdrn.solr.servlet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.shirdrn.util.RequestParams;

/**
 * Rewrite HttpServletRequest parameters if necessary.
 * 
 * @author shirdrn
 * @date   2011-11-11
 */
public class DestinationRequestWrapper extends HttpServletRequestWrapper {

	public DestinationRequestWrapper(HttpServletRequest request){
		super(request);
	}
	
	/**
	 * 拦截重构参数获取方法，对solr参数进行重构
	 */
	@SuppressWarnings("unchecked")
	public Map<String,String[]> getParameterMap(){
		Map<String,String[]> map = super.getParameterMap();
		Map<String,String[]> newMap = new HashMap<String, String[]>();
		Set<String> keys = map.keySet();
		boolean hasDR = false;
		String[] fqValues = new String[0];
		for(String key : keys){
			String[] values = map.get(key);
			if(key.equals(RequestParams.SEASON)){
				if(values.length<1) {
					continue;
				}
				fqValues = extendArrayValue(fqValues, "season:" + values[0]);
				newMap.put(key, values); // payload for boost
			} else if(key.equals(RequestParams.CATEGORY)){
				if(values.length<1) {
					continue;
				}
				fqValues = extendArrayValue(fqValues, "category:" + values[0]);
				newMap.put(key, values); // payload for boost
			} else if(key.equals(RequestParams.FACET_DATE_RANGE)) {
				if(values.length<1) {
					continue;
				}
				facetDateRange(values[0], null, newMap);
			} else if(key.equals(RequestParams.DATE_RANGE)) {
				hasDR = true;
			} else {
				newMap.put(key, values);
			}
		}
		
		// date range filter
		if(hasDR) {
			fqValues = extendArrayValue(fqValues, "publishDate:[NOW-" + map.get(RequestParams.DATE_RANGE)[0] + "MONTH TO NOW]");
		}
		
		if(fqValues.length>0) {
			newMap.put("fq", fqValues);
		}
		
		return newMap;
	}
	
	private String[] extendArrayValue(String[] previous, String value) {
		String[] tmp = previous;
		String[] next = new String[1+tmp.length];
		for(int i=0; i<next.length-1; i++) {
			next[i] = tmp[i];
		}
		next[next.length-1] = value;
		return next;
	}
	
	// datetime unit
	static Map<String, String> dtUnit = new HashMap<String, String>();
	static {
		dtUnit.put("d", "DAY");
		dtUnit.put("m", "MONTH");
		dtUnit.put("y", "YEAR");
	}
	
	/**
	 * 处理按时间范围查询参数，暂时只使用时间单位u=MONTH
	 * e.g. fdr=1,3,6,12,120&u=m
	 * @param fdr	facet date range
	 * @param u		unit of datetime
	 * @param newMap
	 */
	private void facetDateRange(String fdr, String u, Map<String, String[]> newMap) {
		if(u==null || u.isEmpty()) {
			u = "m";
		}
		fdr = fdr.toLowerCase();
		String[] fdra = fdr.split(",");
		newMap.put("facet", new String[]{"true"});
		newMap.put("facet.range", new String[] {"publishDate"});
		String[] facetQuery = new String[fdra.length];
		for (int i = 0; i < fdra.length; i++) {
			facetQuery[i] = "publishDate:[NOW-" + fdra[i] + dtUnit.get(u.toLowerCase()) + " TO NOW]";
		}
		newMap.put("facet.query", facetQuery);
		newMap.put("f.publishDate.facet.range.start", new String[] {"NOW-10YEAR"});
		newMap.put("f.publishDate.facet.range.end", new String[] {"NOW"});
		newMap.put("f.publishDate.facet.range.gap", new String[] {"+1YEAR"});
		newMap.put("f.publishDate.facet.range.other", new String []{"after", "before"});
	}
}
