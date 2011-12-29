package org.shirdrn.solr.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FullSearchAction extends AbstractSearchServlet {

	private static final long serialVersionUID = 117939465003758435L;

	public FullSearchAction() {
		super();
	}
	
	@Override
	public void interceptHttpActions(HttpServletRequest request,
			HttpServletResponse response)
			throws IOException, ServletException {
		// do something according to your demand
	}


}
