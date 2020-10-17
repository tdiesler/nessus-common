package io.nessus.common;


import io.nessus.common.service.Service;

public class ConfigSupport<T extends Config> extends LogSupport {

    protected final T config;
    
    public ConfigSupport(T config) {
        AssertArg.notNull(config, "Null config");
        this.config = config;
    }
    
    @Override
    public T getConfig() {
        return config;
    }

    public <S extends Service> S getService(Class<S> type) {
        return config.getService(type);
    }
}
