package io.nessus.common.rest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.common.AssertArg;
import io.nessus.common.AssertState;

public class SSLContextBuilder {

	static final Logger LOG = LoggerFactory.getLogger(SSLContextBuilder.class);
	
	private String keystoreType = KeyStore.getDefaultType();
	private char[] keystorePassword = "changeit".toCharArray();
	private Path keystorePath;
	private List<KeyMaterial> privKeyMaterials = new ArrayList<>();
	private List<KeyMaterial> certMaterials = new ArrayList<>();
	private List<KeyMaterial> pemMaterials = new ArrayList<>();
	
	static class KeyMaterial {
		final String alias;
		final Path path;
		KeyMaterial(String alias, Path path) {
			this.alias = alias;
			this.path = path;
		}
	}
	
	public SSLContextBuilder keystorePath(Path keysPath) {
		this.keystorePath = keysPath;
		return this;
	}
	
	public SSLContextBuilder addPem(String alias, Path pemPath) {
		this.pemMaterials.add(new KeyMaterial(alias, pemPath));
		return this;
	}
	
	public SSLContextBuilder addCertificate(String alias, Path certPath) {
		this.certMaterials.add(new KeyMaterial(alias, certPath));
		return this;
	}
	
	public SSLContextBuilder addPrivateKey(String alias, Path privKeyPath) {
		this.privKeyMaterials.add(new KeyMaterial(alias, privKeyPath));
		return this;
	}
	
	public SSLContextBuilder keystoreType(String keysType) {
		this.keystoreType = keysType;
		return this;
	}
	
	public SSLContextBuilder keystorePassword(String keysPassword) {
		this.keystorePassword = keysPassword.toCharArray();
		return this;
	}
	
    public SSLContext build() throws IOException, GeneralSecurityException {
        
        KeyStore keystore = loadKeyStore(keystorePath, keystoreType, keystorePassword);
        
        SSLContext sslContext;
        try {
        	
            KeyManager[] keyManagers = buildKeyManagers(keystore, keystorePassword);
            TrustManager[] trustManagers = buildTrustManagers(keystore);
            
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);
        }
        catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new IOException("Unable to create and initialise the SSLContext", ex);
        }

        return sslContext;
    }

    private KeyStore loadKeyStore(Path keystorePath, String keystoreType, char[] keystorePassword) throws IOException, GeneralSecurityException {
		AssertArg.notNull(keystorePath, "Null keystorePath");
		AssertArg.notNull(keystoreType, "Null keystoreType");
		AssertArg.notNull(keystorePassword, "Null keysPassword");
    	
    	KeyStore keystore = KeyStore.getInstance(keystoreType);
    	
    	if (keystorePath.toFile().isFile()) {
    		
    		LOG.info("Loading keystore file: {}", keystorePath);
            
    		try (FileInputStream fis = new FileInputStream(keystorePath.toFile())) {
                keystore.load(fis, keystorePassword);
            }
            
    	} else {
    		
    		LOG.info("Creating keystore ...");
            
    		keystorePath.toFile().getParentFile().mkdirs();
    		
            keystore.load(null, keystorePassword);
    	}
    	
    	int addedMaterials = 0;
    	
        if (!pemMaterials.isEmpty()) {
        	
            Iterator<KeyMaterial> materialIterator = pemMaterials.iterator();
            while (materialIterator.hasNext()) {
            	
            	KeyMaterial pemMaterial = materialIterator.next();
            	
            	Path pemPath = pemMaterial.path;
            	String pemAlias = pemMaterial.alias;
            	
        		LOG.info("Adding pem material: {}", pemPath);
        		
        		Certificate cert = readCertificate(pemPath);
            	if (cert != null) {
                    keystore.setCertificateEntry(pemAlias, cert);
            	}
            	
            	RSAPrivateKey privKey = readPrivateKey(pemPath);
            	if (privKey != null) {
            		PrivateKeyEntry keyEntry = new PrivateKeyEntry(privKey, new Certificate[] { cert });
                    keystore.setEntry(pemAlias, keyEntry, new PasswordProtection(keystorePassword));
            	}
            	
            	materialIterator.remove();
                addedMaterials++;
            }
        }
        
        if (!certMaterials.isEmpty()) {
        	
            Iterator<KeyMaterial> materialIterator = certMaterials.iterator();
            while (materialIterator.hasNext()) {
            	
            	KeyMaterial certMaterial = materialIterator.next();
            	
            	Path crtPath = certMaterial.path;
            	String crtAlias = certMaterial.alias;
            	
        		LOG.info("Adding certificate material: {}", crtPath);
        		
        		Certificate cert = readCertificate(crtPath);
            	AssertState.notNull(cert, "Null certificate");
            	
                keystore.setCertificateEntry(crtAlias, cert);
            	
            	materialIterator.remove();
                addedMaterials++;
            }

            if (!privKeyMaterials.isEmpty()) {
            	
                materialIterator = privKeyMaterials.iterator();
                while (materialIterator.hasNext()) {
                	
                	KeyMaterial privKeyMaterial = materialIterator.next();
                	
                	Path privKeyPath = privKeyMaterial.path;
                	String privAlias = privKeyMaterial.alias;
                	
            		LOG.info("Adding private key material: {}", privKeyPath);
            		
                	RSAPrivateKey privKey = readPrivateKey(privKeyPath);
                	AssertState.notNull(privKey, "Null private key");
                	
                	Certificate cert = keystore.getCertificate(privAlias);
                	AssertState.notNull(cert, "Cannot find certificate for: " + privAlias);
                	
            		PrivateKeyEntry keyEntry = new PrivateKeyEntry(privKey, new Certificate[] { cert });
                    keystore.setEntry(privAlias, keyEntry, new PasswordProtection(keystorePassword));
                	
                	materialIterator.remove();
                    addedMaterials++;
                }
            }
        }
        
        if (addedMaterials > 0) {
        	
    		LOG.info("Storing keystore file: {}", keystorePath);
    		
            try (FileOutputStream fos = new FileOutputStream(keystorePath.toFile())) {
            	keystore.store(fos, keystorePassword);
            }
        }
        
        return keystore;
    }

    private Certificate readCertificate(Path pemPath) throws IOException, GeneralSecurityException {
    	
	    String content = readPemContent(pemPath, "CERTIFICATE");
	    if (content.length() == 0)
	    	return null;
	    
	    byte[] decoded = Base64.getDecoder().decode(content);
	    InputStream bais = new ByteArrayInputStream(decoded);
	 
    	CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return certFactory.generateCertificate(bais);
	}

    @SuppressWarnings("unused")
	private RSAPublicKey readPublicKey(Path pemPath) throws IOException, GeneralSecurityException {
    	
	    String content = readPemContent(pemPath, "PUBLIC KEY");
	    if (content.length() == 0)
	    	return null;
	 
	    byte[] decoded = Base64.getDecoder().decode(content);
	 
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
	    return (RSAPublicKey) keyFactory.generatePublic(keySpec);
	}

    private RSAPrivateKey readPrivateKey(Path pemPath) throws IOException, GeneralSecurityException {
    	
	    String content = readPemContent(pemPath, "PRIVATE KEY");
	    if (content.length() == 0)
	    	return null;
	 
	    byte[] decoded = Base64.getDecoder().decode(content);
	 
	    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
	    return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
	}
    
	private String readPemContent(Path pemPath, String type) throws IOException {

	    String beginMarker = "-----BEGIN " + type + "-----";
	    String endMarker = "-----END " + type + "-----";
		boolean readContent = false;
	    
    	InputStream bais = new ByteArrayInputStream(Files.readAllBytes(pemPath));
    	BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(bais)));
	    StringWriter sw = new StringWriter();
	    
    	String line = br.readLine();
    	while (line != null) {
    		
    		if (!readContent && line.equals(beginMarker)) {
    			readContent = true;
    		}
    		
    		else if (readContent && line.equals(endMarker)) {
    			readContent = false;
    		}
    		
    		else if (readContent) {
    			sw.write(line);
    		}
    		
    		line = br.readLine();
    	}
    	
	    String content = sw.toString();
		return content;
	}

    private KeyManager[] buildKeyManagers(final KeyStore keyStore, char[] keysPassword) throws GeneralSecurityException  {
        String keyAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyAlgorithm);
        keyManagerFactory.init(keyStore, keysPassword);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        return keyManagers;
    }

    private TrustManager[] buildTrustManagers(final KeyStore trustStore) throws IOException, GeneralSecurityException {
        String trustAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustAlgorithm);
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        return trustManagers;
    }
}