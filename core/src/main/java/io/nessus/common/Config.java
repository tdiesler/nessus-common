package io.nessus.common;

import java.util.List;
import java.util.Map;

import io.nessus.common.service.Service;

public interface Config {

    /**
     * Initializes the config with a mapping from 
     * system property to env variable keys, like this ...
     * 
	 *	Map<String, String> mapping = new LinkedHashMap<>();
	 *	mapping.put("jdbcServerUrl", "JDBC_SERVER_URL");
	 *	mapping.put("jdbcUrl", "JDBC_URL");
	 *	mapping.put("jdbcUser", "JDBC_USER");
	 *	mapping.put("jdbcPassword", "JDBC_PASSWORD");
     */
	void prepare(Map<String, String> mapping);
	
	List<String> getParameterNames();
	
	Parameters getParameters();
    
    <T> T getParameter(String name, Class<T> type);
    
    <T> T getParameter(String name, T defaultValue);
    
    <T> T putParameter(String name, T value);
    
    <T extends Service> void addService(T service);
    
    <T extends Service> T getService(Class<T> type);
    
    void initServices();
    
    void closeServices();
}
