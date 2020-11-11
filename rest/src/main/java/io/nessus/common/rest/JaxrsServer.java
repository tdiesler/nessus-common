package io.nessus.common.rest;

import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Application;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.common.Config;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * An extension to the RestEasy server
 * that adds TLS and root handler security
 * 
 * @author tdiesler@redhat.com
 */
public class JaxrsServer extends UndertowJaxrsServer {

    static final Logger LOG = LoggerFactory.getLogger(JaxrsServer.class);
    
	private String hostname;
	private Integer httpPort;
	private Integer httpsPort;
	private SSLContext sslContext;
	private HandlerWrapper securityWrapper;
	
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

	public JaxrsServer withRootSecurity(HandlerWrapper wrapper) {
		this.securityWrapper = wrapper;
		return this;
	}

	@Override
	public JaxrsServer start() {
		
		Builder builder = Undertow.builder();
		
		HttpHandler handler = root;
		
		if (securityWrapper != null) 
			handler = securityWrapper.wrap(handler);
		
		builder.setHandler(handler);
		
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
	
	@Override
	public JaxrsServer deploy(DeploymentInfo di) {
		if (di.getInitialSecurityWrapper() == null) {
			di.setInitialSecurityWrapper(securityWrapper);
		}
		super.deploy(di);
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
	
	public static class BasicSecurityWrapper implements HandlerWrapper {

		private final IdentityManager identityManager;
		private final boolean required;

		public BasicSecurityWrapper(IdentityManager identityManager, boolean required) {
			this.identityManager = identityManager;
			this.required = required;
		}

		@Override
		public HttpHandler wrap(HttpHandler rootHandler) {
	        HttpHandler handler = rootHandler;
	        handler = new AuthenticationCallHandler(handler);
	        if (required) handler = new AuthenticationConstraintHandler(handler);
	        handler = new AuthenticationMechanismsHandler(handler, Arrays.asList(new BasicAuthenticationMechanism("MyRealm")));
	        handler = new SecurityContextAssociationHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
			return handler;
		}
	}
	
	/**
	 * Reuses an already authenticated context
	 */
	static class SecurityContextAssociationHandler extends SecurityInitialHandler {

		SecurityContextAssociationHandler(AuthenticationMode authenticationMode, IdentityManager identityManager, HttpHandler next) {
			super(authenticationMode, identityManager, next);
		}
		
		@Override
		public SecurityContext createSecurityContext(HttpServerExchange exchange) {
			SecurityContext context = exchange.getSecurityContext();
			if (context != null && context.isAuthenticated()) {
				return context;
			}
			return super.createSecurityContext(exchange);
		}
	}
}
