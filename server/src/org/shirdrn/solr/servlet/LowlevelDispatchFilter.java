package org.shirdrn.solr.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.FastWriter;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.BinaryQueryResponseWriter;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.servlet.SolrRequestParsers;
import org.apache.solr.servlet.cache.HttpCacheHeaderUtil;
import org.apache.solr.servlet.cache.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copy {@link SolrDispatchFilter}, and make some modifications.
 * 
 * @author shirdrn
 * @date 2011-11-11
 */
public class LowlevelDispatchFilter implements Filter {
	final Logger log = LoggerFactory.getLogger(LowlevelDispatchFilter.class);
	protected CoreContainer cores;
	protected String abortErrorMessage = null;
	protected final Map<SolrConfig, SolrRequestParsers> parsers = new WeakHashMap<SolrConfig, SolrRequestParsers>();
	protected String coreName = null;
	private static final Charset UTF8 = Charset.forName("UTF-8");

	public void init(FilterConfig config) throws ServletException {
		CoreContainer.Initializer init = new CoreContainer.Initializer();
		try {
			this.cores = init.initialize();
			log.info("user.dir=" + System.getProperty("user.dir"));
		} catch (Throwable t) {
			// catch this so our filter still works
			log.error("Could not start Solr. Check solr/home property", t);
			SolrConfig.severeErrors.add(t);
			SolrCore.log(t);
		}
	}

	public void destroy() {
		if (cores != null) {
			cores.shutdown();
			cores = null;
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (abortErrorMessage != null) {
			((HttpServletResponse) response).sendError(500, abortErrorMessage);
			return;
		}

		if (request instanceof HttpServletRequest) {
			HttpServletRequest req = (HttpServletRequest) request;
			HttpServletResponse resp = (HttpServletResponse) response;

			// Intercept the HTTP request, you can rewrite HttpServletRequest
			interceptHttpActions(req, resp, chain);

			SolrRequestHandler handler = null;
			SolrQueryRequest solrReq = null;
			SolrCore core = null;
			try {
				// put the core container in request attribute
				req.setAttribute("org.apache.solr.CoreContainer", cores);
				String path = req.getServletPath();
				if (req.getPathInfo() != null) {
					// this lets you handle /update/commit when /update is a
					// servlet
					path += req.getPathInfo();
				}
				// check for management path
				String alternate = cores.getManagementPath();
				if (alternate != null && path.startsWith(alternate)) {
					path = path.substring(0, alternate.length());
				}
				// unused feature ?
				int idx = path.indexOf(':');
				if (idx > 0) {
					// save the portion after the ':' for a 'handler' path
					// parameter
					path = path.substring(0, idx);
				}
				// otherwise, we should find a core from the path
				idx = path.indexOf("/", 1);
				if (idx > 1) {
					// try to get the corename as a request parameter first
					coreName = path.substring(1, idx);
					core = cores.getCore(coreName);
					if (core != null) {
						path = path.substring(idx);
					}
				}
				if (core == null) {
					coreName = "";
					core = cores.getCore("");
				}
				// With a valid core...
				if (core != null) {
					final SolrConfig config = core.getSolrConfig();
					// get or create/cache the parser for the core
					SolrRequestParsers parser = null;
					parser = parsers.get(config);
					if (parser == null) {
						parser = new SolrRequestParsers(config);
						parsers.put(config, parser);
					}
					// Determine the handler from the url path if not set
					// (we might already have selected the cores handler)
					if (handler == null && path.length() > 1) { // don't match
																// "" or "/" as
																// valid path
						handler = core.getRequestHandler(path);
					}
					// With a valid handler and a valid core...
					if (handler != null) {
						// if not a /select, create the request
						if (solrReq == null) {
							solrReq = parser.parse(core, path, req);
						}
						final Method reqMethod = Method.getMethod(req.getMethod());
						HttpCacheHeaderUtil.setCacheControlHeader(config, resp, reqMethod);
						// unless we have been explicitly told not to, do cache
						// validation
						// if we fail cache validation, execute the query
						if (config.getHttpCachingConfig().isNever304()
								|| !HttpCacheHeaderUtil.doCacheHeaderValidation(solrReq, req, reqMethod, resp)) {
							SolrQueryResponse solrRsp = new SolrQueryResponse();
							/*
							 * even for HEAD requests, we need to execute the
							 * handler to ensure we don't get an error (and to
							 * make sure the correct QueryResponseWriter is
							 * selected and we get the correct Content-Type)
							 */
							this.execute(req, handler, solrReq, solrRsp);
							HttpCacheHeaderUtil.checkHttpCachingVeto(solrRsp, resp, reqMethod);
							QueryResponseWriter responseWriter = core.getQueryResponseWriter(solrReq);
							writeResponse(solrRsp, response, responseWriter, solrReq, reqMethod);
						}
						return; // we are done with a valid handler
					} else {
						req.setAttribute("org.apache.solr.SolrCore", core);
					}
				}
				log.debug("no handler or core retrieved for " + path + ", follow through...");
			} catch (Throwable ex) {
				sendError((HttpServletResponse) response, ex);
				return;
			} finally {
				if (solrReq != null) {
					solrReq.close();
				}
				if (core != null) {
					core.close();
				}
			}
		}
		// Otherwise let the webapp handle the request
		chain.doFilter(request, response);
	}

	private void interceptHttpActions(HttpServletRequest request,
			HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String method = request.getMethod();
		if (method.equalsIgnoreCase("get")) {
			// rewrite the request
			request = new LowlevelRequestWrapper(request);
		}
	}

	private void writeResponse(SolrQueryResponse solrRsp,
			ServletResponse response, QueryResponseWriter responseWriter,
			SolrQueryRequest solrReq, Method reqMethod) throws IOException {
		if (solrRsp.getException() != null) {
			sendError((HttpServletResponse) response, solrRsp.getException());
		} else {
			// Now write it out
			final String ct = responseWriter.getContentType(solrReq, solrRsp);
			// don't call setContentType on null
			if (null != ct) {
				response.setContentType(ct);
			}
			if (Method.HEAD != reqMethod) {
				if (responseWriter instanceof BinaryQueryResponseWriter) {
					BinaryQueryResponseWriter binWriter = (BinaryQueryResponseWriter) responseWriter;
					binWriter.write(response.getOutputStream(), solrReq, solrRsp);
				} else {
					String charset = ContentStreamBase.getCharsetFromContentType(ct);
					Writer out = (charset == null || 
							charset.equalsIgnoreCase("UTF-8")) ? new OutputStreamWriter(response.getOutputStream(), UTF8) : new OutputStreamWriter(response.getOutputStream(), charset);
					out = new FastWriter(out);
					responseWriter.write(out, solrReq, solrRsp);
					out.flush();
				}
			}
		}
	}

	protected void execute(HttpServletRequest req, SolrRequestHandler handler,
			SolrQueryRequest sreq, SolrQueryResponse rsp) {
		sreq.getContext().put("webapp", req.getContextPath());
		sreq.getCore().execute(handler, sreq, rsp);
	}

	protected void sendError(HttpServletResponse res, Throwable ex)
			throws IOException {
		int code = 500;
		String trace = "";
		if (ex instanceof SolrException) {
			code = ((SolrException) ex).code();
		}

		// For any regular code, don't include the stack trace
		if (code == 500 || code < 100) {
			StringWriter sw = new StringWriter();
			ex.printStackTrace(new PrintWriter(sw));
			trace = "\n\n" + sw.toString();
			SolrException.logOnce(log, null, ex);
			// non standard codes have undefined results with various servers
			if (code < 100) {
				log.warn("invalid return code: " + code);
				code = 500;
			}
		}
		res.sendError(code, ex.getMessage() + trace);
	}
}
