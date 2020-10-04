package io.nessus.common;

import io.nessus.common.service.Service;

public interface Config {

    Parameters getParameters();
    
    <T> T getParameter(String name, Class<T> type);
    
    <T> T getParameter(String name, T defaultValue);
    
    <T> T putParameter(String name, T value);
    
    <T extends Service> void addService(T service);
    
    <T extends Service> T getService(Class<T> type);
    
    void initServices();
}
