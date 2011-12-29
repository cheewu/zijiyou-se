package org.shirdrn.solr.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DestinationSearchAction extends AbstractSearchServlet {

	private static final long serialVersionUID = 4668194491692302325L;

	public DestinationSearchAction() {
		super();
	}
	
	@Override
	public void interceptHttpActions(HttpServletRequest request,
			HttpServletResponse response)
			throws IOException, ServletException {
		String method = request.getMethod();
		if (method.equalsIgnoreCase("get")) {
			// rewrite the request
			request = new DestinationRequestWrapper(request);
		}
	}

}
