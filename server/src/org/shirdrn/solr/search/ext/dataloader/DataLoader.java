package org.shirdrn.solr.search.ext.dataloader;

/**
 * Common data load interface.
 * 
 * @author shirdrn
 * @date   2011-11-22
 */
public interface DataLoader {

	/**
	 * Load data needed from any data source.
	 * e.g. a text file, database, external storage.
	 * @throws Exception
	 */
	public void loadData() throws Exception;
	
	/**
	 * Return data set populated.
	 * @return
	 */
	public Object getLoadedDataSet();
}
