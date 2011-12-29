package org.shirdrn.solr.search.ext;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.core.SolrResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.shirdrn.solr.search.ext.dataloader.DataLoader;
import org.shirdrn.solr.search.ext.dataloader.FileDataLoader;
import org.shirdrn.util.ConfigParams;
import org.shirdrn.util.Pair;



/**
 * Extended words boost dynamically: 
 * Load extended words from given source statically.
 * This class processes the words into adaptive forms 
 * for query process. When using it you can only pass main
 * words ({@link MainWord}) and it returns the extended formation.
 * 
 * @author shirdrn
 * @date   2011-11-10
 */
public class ExtendableQueryBooster {

	private static final Logger LOG = LoggerFactory.getLogger(ExtendableQueryBooster.class);
	private Map<MainWord, List<Pair<String, Float>>> EXT_WORD_SET;
	private DataLoader dataLoader;
	private File extFile;
	
	@SuppressWarnings("unchecked")
	public ExtendableQueryBooster(Map conditions) {
		super();
		String extFilePath = (String) conditions.get(ConfigParams.EXT_FILE);
		String coreName = (String) conditions.get(ConfigParams.CORE_NAME);
		dataLoader = (DataLoader) conditions.get(ConfigParams.DATA_LOADER);
		prepare(extFilePath, coreName);
	}

	@SuppressWarnings("unchecked")
	private void prepare(String extFilePath, String coreName) {
		locateExtFile(extFilePath, coreName);
		try {
			if(dataLoader instanceof FileDataLoader) {
				((FileDataLoader)dataLoader).setExtFile(extFile);
			}
			dataLoader.loadData();
		} catch (Exception e) {
			e.printStackTrace();
		}
		EXT_WORD_SET = (Map<MainWord, List<Pair<String, Float>>>) dataLoader.getLoadedDataSet();
	}

	private void locateExtFile(String extFilePath, String coreName) {
		extFile = new File(extFilePath);
		if(!extFile.exists()) {
			String solrHome = SolrResourceLoader.locateSolrHome();
			extFile = new File(new File(solrHome), coreName + "/conf/" + extFilePath);
			if(!extFile.exists()) {
				LOG.warn("Path \"" + extFile.getAbsolutePath() + "\" is no existence.");
			}
		}
	}
	
	/**
	 * Obtain singleTermText's extended {@link Query}.
	 * @param singleTermText single term text
	 * @param boost single term text's boost value
	 * @param queryFields query fields
	 * @return
	 */
	public BooleanQuery getExtendedQuery(String singleTermText, Float boost, Map<String, Float> queryFields) {
		LOG.debug("singleTermText = " + singleTermText);
		BooleanQuery query = new BooleanQuery(false);
		BooleanQuery bq = new BooleanQuery(false);
		for(Map.Entry<String,Float> field : queryFields.entrySet()) {
			TermQuery q = new TermQuery(new Term(field.getKey(), singleTermText.trim()));
			if(field.getValue()!=null) {
				q.setBoost(boost * field.getValue());
			}
			bq.add(q, BooleanClause.Occur.SHOULD);
		}
		query.add(bq, BooleanClause.Occur.SHOULD);
		
		Set<String> words = new HashSet<String>();
		words.add(singleTermText.trim());
		MainWord mainWord = new MainWord(words);
		List<Pair<String, Float>> ext = EXT_WORD_SET.get(mainWord);
		if(ext!=null) {
			parseQuery(query, ext, queryFields);
		}
		return query;
	}
	
	public BooleanQuery getExtendedQuery(String singleTermText, Map<String, Float> queryFields) {
		return getExtendedQuery(singleTermText, 1.0f, queryFields);
	}
	
	private void parseQuery(BooleanQuery query, List<Pair<String, Float>> ext, Map<String, Float> queryFields) {
		for(Pair<String, Float> pair : ext) {
			BooleanQuery bq = new BooleanQuery(false);
			for(Map.Entry<String,Float> field : queryFields.entrySet()) {
				TermQuery q = new TermQuery(new Term(field.getKey(), pair.getKey()));
				if(field.getValue()!=null) {
					q.setBoost(field.getValue() * pair.getValue());
				}
				bq.add(q, BooleanClause.Occur.SHOULD);
			}
			query.add(bq, BooleanClause.Occur.SHOULD);
		}
	}
	
}
