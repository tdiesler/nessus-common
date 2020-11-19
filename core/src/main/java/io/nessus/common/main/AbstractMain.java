package io.nessus.common.main;

import static io.nessus.common.BasicConfig.redactValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.nessus.common.CheckedExceptionWrapper;
import io.nessus.common.Config;
import io.nessus.common.ConfigSupport;

public abstract class AbstractMain<T extends Config, O extends AbstractOptions> extends ConfigSupport<T> {

    private static String implVersion;
    private static String implBuild;

    public AbstractMain(T config) {
        super(config);
    }

    protected abstract O createOptions();

    public final void start(String... args) {
        
        try {
            
            startInternal(args);
            
        } catch (Exception ex) {
            
            logError(ex);
        }
    }

    @SuppressWarnings("unchecked")
	protected void prepare(Map<String, String> mapping, O options) {

		// Override with env vars then with system props
		
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
				logDebug("Opt {}: {}", key, redactValue(key, value));
				config.putParameter(en.getKey(), value);
			}
		}
		
        // Log the initial configuration
		
        getConfig().getParameters().toMap().entrySet().stream()
            .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
            .forEach(en -> { 
            	String key = en.getKey();
            	Object value = en.getValue();
				logInfo("{}: {}", key, redactValue(key, value)); 
            });
    }

	protected void startInternal(String... args) throws Exception {
		
		O options = parseArguments(args);
		
		prepare(new LinkedHashMap<>(), options);
		
		doStart(options);
	}
    
    protected abstract void doStart(O options) throws Exception;

    @SuppressWarnings("unchecked")
    protected O parseArguments(String... args) throws CmdLineException {
        
        O options = createOptions();
        CmdLineParser parser = new CmdLineParser(options);

        // Obtain version information from the manifest that contains the options class
        Class<O> clazz = (Class<O>) options.getClass();
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

    protected void helpScreen(O options) {
        System.err.println(options.cmd + " [options...]");
        CmdLineParser parser = new CmdLineParser(options);
        parser.printUsage(System.err);
    }
}
