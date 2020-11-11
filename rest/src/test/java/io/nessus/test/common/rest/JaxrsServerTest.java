package io.nessus.test.common.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.ws.rs.GET;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.common.Config;
import io.nessus.common.ConfigSupport;
import io.nessus.common.LogSupport;
import io.nessus.common.Parameters;
import io.nessus.common.rest.AbstractIdentityManager;
import io.nessus.common.rest.JaxrsServer;
import io.nessus.common.rest.JaxrsServer.BasicSecurityWrapper;
import io.nessus.common.rest.SSLContextBuilder;
import io.nessus.common.testing.AbstractTest;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class JaxrsServerTest extends AbstractTest<Config> {

	@Test
    @SuppressWarnings("unchecked")
    public void testUnsecure() throws Exception {
        
        JaxrsServer jaxrsServer = new JaxrsServer(getConfig())
                .withHostname("0.0.0.0")
                .withHttpPort(8080)
                .deploy("/api", new MyApplication(getConfig()))
                .addPrefixPath("/", new PrefixHandler(getConfig()));
        
        jaxrsServer.start();
        try {
        	
            String rooturl = "http://localhost:8080";
            String apiurl = rooturl + "/api/foo";
            
            BufferedReader br = new BufferedReader(new InputStreamReader(new URL(rooturl).openStream()));
            Assert.assertEquals("Hello Kermit", br.readLine().replace("\"", ""));
            
            br = new BufferedReader(new InputStreamReader(new URL(apiurl).openStream()));
            Assert.assertEquals("{result:true}", br.readLine().replace("\"", ""));
            
            Client client = ClientBuilder.newClient();
			Response res = client.target(apiurl)
            		.request().get();
            
            Map<String, Object> resmap = res.readEntity(Map.class);
            Assert.assertEquals("{result=true}", new Parameters(resmap).toString());
            
        } finally {
			jaxrsServer.stop();
		}
    }   

	@Test
    @SuppressWarnings("unchecked")
    public void testSecure() throws Exception {
        
		Path tls = Paths.get("src/test/resources/tls");
		Path crtPath = tls.resolve("tls.crt");
		Path keyPath = tls.resolve("tls.key");
		
		SSLContext sslContext = new SSLContextBuilder()
				.keystorePath(Paths.get("target/keystore.jks"))
				.addCertificate("test", crtPath)
				.addPrivateKey("test", keyPath)
				.build();
		
		SSLContext.setDefault(sslContext);
		
        IdentityManager idm = getIdentityManager();
		BasicSecurityWrapper requiredSecurity = new BasicSecurityWrapper(idm, true);
		
        JaxrsServer jaxrsServer = new JaxrsServer(getConfig())
                .withHostname("0.0.0.0")
                .withHttpsPort(8443, sslContext)
                .withRootSecurity(requiredSecurity)
                .deploy("/api", new MyApplication(getConfig()))
                .addPrefixPath("/", new PrefixHandler(getConfig()));
        
        jaxrsServer.start();
        try {
        	
            Client client = ClientBuilder.newBuilder()
            		.sslContext(sslContext)
            		.build();
            
            // Authenticated request
            
            byte[] bytes = "userOne:passwordOne".getBytes();
			String encoded = Base64.getEncoder().encodeToString(bytes);
			
            Response res = client.target("https://localhost:8443/api/foo")
            		.request().header("Authorization", "Basic " + encoded).get();
            
            Map<String, Object> resmap = res.readEntity(Map.class);
            Assert.assertEquals("true", resmap.get("result"));
            
            res = client.target("https://localhost:8443/")
            		.request().header("Authorization", "Basic " + encoded).get();
            
            Assert.assertEquals(200, res.getStatus());
            Assert.assertEquals("Hello Kermit", res.readEntity(String.class));
                        
            // Unauthenticated request
            
            res = client.target("https://localhost:8443/api/foo")
            		.request().get();
            
            Assert.assertEquals(401, res.getStatus());
            
            res = client.target("https://localhost:8443/")
            		.request().get();
            
            Assert.assertEquals(401, res.getStatus());
                        
        } finally {
			jaxrsServer.stop();
		}
    }   

	private IdentityManager getIdentityManager() {
		
		IdentityManager idm = new AbstractIdentityManager<Config>(getConfig()) {
        	
            Map<String, char[]> users = new HashMap<>(2);
            {
                users.put("userOne", "passwordOne".toCharArray());
                users.put("userTwo", "passwordTwo".toCharArray());
            }

			@Override
			public boolean verify(Account account, Credential credential) {
				char[] expected = users.get(account.getPrincipal().getName());
				char[] provided = ((PasswordCredential) credential).getPassword();
				return Arrays.equals(expected, provided);
			}
        };
        
		return idm;
	}   

    public static class MyApplication extends Application {
    	
    	private static MyApplication INSTANCE;
    	
    	private final Config config;
    	
    	public MyApplication(Config config) {
    		this.config = config;
    		INSTANCE = this;
    	}

    	public static MyApplication getInstance() {
    		return INSTANCE;
    	}

    	public Config getConfig() {
    		return config;
    	}
    	
    	@Override
    	public Set<Class<?>> getClasses() {
    		Set<Class<?>> classes = new HashSet<>();
    		classes.add(FooResource.class);
    		return Collections.unmodifiableSet(classes);
    	}
    }

    @javax.ws.rs.Path("/foo") 
    public static class FooResource extends LogSupport {
    	
    	private MyApplication app;
    	
    	public FooResource() {
    		app = MyApplication.getInstance();
    	}

    	@Override
    	public Config getConfig() {
    		return app.getConfig();
    	}

    	@GET
    	public Response getStuff() {
    		Parameters result = Parameters.fromString("{result=true}");
    		return Response.ok(result.toMap(), MediaType.APPLICATION_JSON).build();
    	}
    }

    static class PrefixHandler extends ConfigSupport<Config> implements HttpHandler {

        public PrefixHandler(Config config) {
            super(config);
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            exchange.getResponseSender().send("Hello Kermit");
        }
    }
}
