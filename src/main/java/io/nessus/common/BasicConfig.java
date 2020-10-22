package io.nessus.common;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.logging.LogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.nessus.common.BasicConfig.ConfigSerializer;
import io.nessus.common.service.BasicLogService;
import io.nessus.common.service.Service;

@JsonSerialize(using = ConfigSerializer.class)
public class BasicConfig implements Config {

    protected final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    
    private final Map<String, Service> services = new LinkedHashMap<>();
    private final Parameters params;
    
    static {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }
    
    public BasicConfig(URL cfgurl) throws IOException {
    	AssertArg.notNull(cfgurl, "Null cfgurl");
    	ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Config cfg = mapper.readValue(cfgurl, BasicConfig.class);
        this.params = cfg.getParameters();
        addService(new BasicLogService());
    }

    @JsonCreator
    public BasicConfig(Map<String, ? extends Object> params) {
        this.params = new Parameters(params);
        addService(new BasicLogService());
    }

    public BasicConfig(Parameters params) {
        this.params = new Parameters(params);
        addService(new BasicLogService());
    }

    @Override
	public void prepare(Map<String, String> mapping) {

		BiFunction<String, String, String> logval = (k, v) -> {
			if (v == null) return null;
			boolean ispw = k.toLowerCase().contains("pass");
			v = ispw && v.length() > 0  ? "*****" : v;
			return v;
		};
		
		// Override with env vars
		
		for (Entry<String, String> en : mapping.entrySet()) {
			String key = en.getKey();
			String value = System.getenv(en.getValue());
			if (value != null) {
				LOG.debug("Env {}: {}", en.getValue(), logval.apply(key, value));
				putParameter(key, value);
			}
		}
		
		// Override with system properties
		
		for (Entry<String, String> en : mapping.entrySet()) {
			String key = en.getKey();
			String value = System.getProperty(key);
			if (value != null) {
				LOG.debug("Sys {}: {}", key, logval.apply(key, value));
				putParameter(key, value);
			}
		}
    }
    
    @Override
    public List<String> getParameterNames() {
        return params.keys();
    }

    @Override
    public Parameters getParameters() {
        return new Parameters(params);
    }

    @Override
    public <T> T getParameter(String name, Class<T> type) {
        return params.get(name, type);
    }

    @Override
    public <T> T getParameter(String name, T defaultValue) {
        return params.get(name, defaultValue);
    }

    @Override
    public <T> T putParameter(String name, T value) {
        return params.put(name, value);
    }

    @Override
    public <T extends Service> void addService(T service) {
    	services.put(service.getType(), service);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Service> T getService(Class<T> type) {
        T result = (T) services.get(type.getName());
        if (result == null) {
            for (Service srv : services.values()) {
                if (type.isAssignableFrom(srv.getClass())) {
                    result = (T) srv;
                    break;
                }
            }
        }
        return result;
    }

    @Override
	public void initServices() {
    	services.values().forEach(srv -> srv.init(this));
	}

    @Override
	public void closeServices() {
    	services.values().forEach(srv -> srv.close());
	}

    @Override
	public int hashCode() {
		return params.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Config)) return false;
		Config other = (Config) obj;
		return params.equals(other.getParameters());
	}

	@Override
	public String toString() {
		return params.toString();
	}

	public static class ConfigSerializer extends JsonSerializer<Config> {

        @Override
        public void serialize(Config value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        	Map<String, Object> map = value.getParameters().toMap();
            jgen.writeObject(map);
        }
    }
}
