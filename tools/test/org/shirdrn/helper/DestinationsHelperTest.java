package org.shirdrn.helper;

import junit.framework.TestCase;

import org.shirdrn.common.DBConfig;

public class DestinationsHelperTest extends TestCase {

	DestinationsHelper helper;
	
	@Override
	protected void setUp() throws Exception {
		DBConfig config = new DBConfig("192.168.0.184", 27017, "tripfm", "Region");
		helper = new DestinationsHelper(config);
	}
	
	@Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
	}
	
	public void testOutput() {
		helper.output("E:\\words-destinations.dic");
	}
}
