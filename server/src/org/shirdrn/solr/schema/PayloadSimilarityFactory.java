package org.shirdrn.solr.schema;

import org.apache.solr.schema.SimilarityFactory;

/**
 * Payload similarity factory class.
 * 
 * @author shirdrn
 * @date   2011-12-20
 */
public class PayloadSimilarityFactory extends SimilarityFactory {

	@Override
	public PayloadSimilarity getSimilarity() {
		String payloadFields = params.get("payloadFields");
		return new PayloadSimilarity(payloadFields);
	}
}
