package io.nessus.test.common.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.net.ssl.SSLContext;

import org.junit.Test;

import io.nessus.common.AssertState;
import io.nessus.common.Config;
import io.nessus.common.rest.SSLContextBuilder;
import io.nessus.common.testing.AbstractTest;

public class TrustedTLSTest extends AbstractTest<Config> {

	final Path keystorePath = Paths.get("target/keystore.jks");
	
	@Override
	public void before() throws Exception {
		keystorePath.toFile().delete();
	}

	@Test
    public void testTrusted() throws Exception {
        
		Path tls = Paths.get("src/test/resources/tls");
		Path pemPath = tls.resolve("jboss-org.pem");
		Path crtPath = tls.resolve("tls.crt");
		Path keyPath = tls.resolve("tls.key");
		
		SSLContext sslContext = new SSLContextBuilder()
				.keystorePath(keystorePath)
				.addCertificate("kermit", crtPath)
				.addPrivateKey("kermit", keyPath)
				.addPem("jboss", pemPath)
				.build();
		
		// This effects the entire VM
		
		SSLContext.setDefault(sslContext);
		
		// Verify that we can access a resource from jboss.org
		
		URL resurl = new URL("https://repository.jboss.org/nexus/content/repositories/public-jboss/io/nessus/nessus-common/1.2.1/nessus-common-1.2.1.pom");
		
		try (Reader in = new InputStreamReader(resurl.openStream())) {
			BufferedReader br = new BufferedReader(in);
			String line = br.readLine();
			AssertState.isTrue(line.startsWith("<?xml"));
		}
    }   
}
