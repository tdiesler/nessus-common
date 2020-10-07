package io.nessus.common.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.nessus.common.BasicConfig;
import io.nessus.common.CheckedExceptionWrapper;
import io.nessus.common.Config;
import io.nessus.common.ConfigSupport;

public abstract class AbstractMain<C extends Config, T extends AbstractOptions> extends ConfigSupport<C> {

    private static String implVersion;
    private static String implBuild;

    @SuppressWarnings("unchecked")
	public AbstractMain(URL cfgurl) throws IOException {
        this((C) new BasicConfig(cfgurl));
    }

    public AbstractMain(C config) {
        super(config);
    }

    protected abstract T createOptions();

    public final void start(String... args) {
        
        try {
            
            startInternal(args);
            
        } catch (Exception ex) {
            
            logError(ex);
        }
    }

    @SuppressWarnings("unchecked")
	protected void prepare(Map<String, String> mapping, T options) {

		BiFunction<String, String, String> logval = (k, v) -> {
			if (v == null) return null;
			boolean ispw = k.toLowerCase().contains("pass");
			v = ispw && v.length() > 0  ? "*****" : v;
			return v;
		};
		
		// Override with env vars 
		// then with system props
		
		config.prepare(mapping);
		
		// Override with options
		
		Map<String, String> optsmap = Collections.emptyMap();
		try {
			ObjectMapper mapper = new ObjectMapper();
			String content = mapper.writeValueAsString(options);
			optsmap = mapper.readValue(content, LinkedHashMap.class);
		} catch (JsonProcessingException ex) {
			throw CheckedExceptionWrapper.create(ex);
		}
		
		for (Entry<String, String> en : optsmap.entrySet()) {
			String key = en.getKey();
			String value = en.getValue();
			if (value != null) {
				logDebug("Opt {}: {}", key, logval.apply(key, value));
				config.putParameter(en.getKey(), value);
			}
		}
    }

	protected void startInternal(String... args) throws Exception {
		
		T options = parseArguments(args);
		
		prepare(new LinkedHashMap<>(), options);
		
		doStart(options);
	}
    
    protected abstract void doStart(T options) throws Exception;

    @SuppressWarnings("unchecked")
    protected T parseArguments(String... args) throws CmdLineException {
        
        T options = createOptions();
        CmdLineParser parser = new CmdLineParser(options);

        // Obtain version information from the manifest that contains the options class
        Class<T> clazz = (Class<T>) options.getClass();
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (classPath.startsWith("jar")) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 2) + JarFile.MANIFEST_NAME;
            try (InputStream ins = new URL(manifestPath).openStream()) {
                Manifest manifest = new Manifest(ins);
                Attributes attribs = manifest.getMainAttributes();
                implVersion = attribs.getValue("Implementation-Version");
                implBuild = attribs.getValue("Implementation-Build");
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        try {
            parser.parseArgument(args);
        } catch (CmdLineException ex) {
            helpScreen(options);
            throw ex;
        }

        if (options.help) {
            helpScreen(options);
            System.exit(0);
        }

        if (options.version) {
            System.out.println(getVersionString());
            System.exit(0);
        }

        options.initDefaults(getConfig());
        return options;
    }

    public static String getImplVersion() {
        return implVersion;
    }

    public static String getImplBuild() {
        return implBuild;
    }

    public static String getVersionString() {
        if (implVersion != null && implVersion.endsWith("SNAPSHOT"))
            return String.format("%s (%s)", implVersion, implBuild);
        else
            return String.format("%s (%s)", implVersion, implBuild);
    }

    protected void helpScreen(T options) {
        System.err.println(options.cmd + " [options...]");
        CmdLineParser parser = new CmdLineParser(options);
        parser.printUsage(System.err);
    }
}
