package org.shirdrn.solr.schema;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.search.DefaultSimilarity;

/**
 * Similarity based on Payload augmentation.
 * 
 * @author shirdrn
 * @date   2011-12-20
 */
public class PayloadSimilarity extends DefaultSimilarity {

	private static final long serialVersionUID = 111111001L;
	private Map<String, Float> payloadFields = new HashMap<String, Float>();

	public PayloadSimilarity() {
		super();
	}

	public PayloadSimilarity(String pldFields) {
		super();
		if (pldFields != null && pldFields.length() > 0) {
			String[] fields = pldFields.split(" ");
			for (String field : fields) {
				if(field.indexOf("^")!=-1) {
					String[] a = field.trim().split("\\^");
					this.payloadFields.put(a[0], parseFloat(a[1], 1.0f));
				} else {
					this.payloadFields.put(field.trim(), 1.0f);
				}
			}
		}
	}
	
	private float parseFloat(String s, float defaultValue) {
		float value = defaultValue;
		try {
			value = Float.parseFloat(s);
		} catch (NumberFormatException e) {}
		return value;
	}

	@Override
	public float scorePayload(int docId, String fieldName, int start, int end, byte[] payload, int offset, int length) {
		float boostValue = 1.0f;
		if (this.payloadFields.keySet().contains(fieldName) && length > 0) {
			boostValue = 1 + this.payloadFields.get(fieldName) * PayloadHelper.decodeFloat(payload, offset);
		} else {
			boostValue = super.scorePayload(docId, fieldName, start, end, payload, offset, length);
		}
		return boostValue;
	}
}
