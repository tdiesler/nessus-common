package io.nessus.common.rest;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Application;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.common.Config;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;

// UndertowJaxrsServer is not sufficiently abstracted
public class JaxrsServer extends UndertowJaxrsServer {

    static final Logger LOG = LoggerFactory.getLogger(JaxrsServer.class);
    
	private String hostname;
	private Integer httpPort;
	private Integer httpsPort;
	private SSLContext sslContext;

	public JaxrsServer(Config config) {
    }

    public JaxrsServer withHostname(String hostname) {
		this.hostname = hostname;
		return this;
	}

	public JaxrsServer withHttpPort(int port) {
		this.httpPort = port;
		return this;
	}

	public JaxrsServer withHttpsPort(int port, SSLContext sslContext) {
		this.sslContext = sslContext;
		this.httpsPort = port;
		return this;
	}

	@Override
	public JaxrsServer start() {
		
		Builder builder = Undertow.builder().setHandler(root);
		
		if (httpPort != null) {
			builder.addHttpListener(httpPort, hostname);
		}
		
		if (httpsPort != null) {
			builder.addHttpsListener(httpsPort, hostname, sslContext);
		}
		
		LOG.info("Starting {}", this);
		
		server = builder.build();
		server.start();
		
		return this;
	}

	@Override
	public JaxrsServer setHostname(String hostname) {
		return withHostname(hostname);
	}

	@Override
	public JaxrsServer setPort(int port) {
		return withHttpPort(port);
	}

	@Override
	public JaxrsServer deploy(Application application) {
	    return deploy("/", application);
	}
	
	public JaxrsServer deploy(String contextPath, Application application) {
	    super.deploy(application, contextPath);
	    return this;
	}
	
	public JaxrsServer addPrefixPath(String path, HttpHandler handler) {
		root.addPrefixPath(path, handler);
		return this;
	}
	
	@Override
	public String toString() {
		return String.format("JaxrsServer[host=%s, http=%d, https=%d, ssl=%s]", hostname, httpPort, httpsPort, sslContext);
	}
}
