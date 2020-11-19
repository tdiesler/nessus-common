package io.nessus.test.common.rest;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.common.Config;
import io.nessus.common.rest.SSLContextBuilder;
import io.nessus.common.testing.AbstractTest;

public class UntrustedTLSTest extends AbstractTest<Config> {

	final Path keystorePath = Paths.get("target/keystore.jks");
	
	@Override
	public void before() throws Exception {
		keystorePath.toFile().delete();
	}

	@Test
    public void testUntrusted() throws Exception {
        
		Path tls = Paths.get("src/test/resources/tls");
		Path crtPath = tls.resolve("tls.crt");
		Path keyPath = tls.resolve("tls.key");
		
		SSLContext sslContext = new SSLContextBuilder()
				.keystorePath(keystorePath)
				.addCertificate("kermit", crtPath)
				.addPrivateKey("kermit", keyPath)
				.build();
		
		// This effects the entire VM
		
		SSLContext.setDefault(sslContext);
		
		// Verify that we fail to access a resource on jboss.org
		
		try {
			URL resurl = new URL("https://repo1.maven.org/maven2");
			resurl.openStream();
			Assert.fail("SSLHandshakeException expected");
		} catch (SSLHandshakeException ex) {
			// expected
		}
    }   
}
