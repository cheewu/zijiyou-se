package org.shirdrn.solr.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AttractionSearchAction extends AbstractSearchServlet {

	private static final long serialVersionUID = -595528071284296776L;

	public AttractionSearchAction() {
		super();
	}
	
	@Override
	public void interceptHttpActions(HttpServletRequest request,
			HttpServletResponse response)
			throws IOException, ServletException {
		String method = request.getMethod();
		if (method.equalsIgnoreCase("get")) {
			// rewrite the request
			request = new AttractionRequestWrapper(request);
		}
	}
}
