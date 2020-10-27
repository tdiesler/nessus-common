package io.nessus.common.service;

import io.nessus.common.Config;
import io.nessus.common.ConfigSupport;

public abstract class AbstractBasicService<T extends Config> extends ConfigSupport<T> implements Service {

    protected AbstractBasicService(T config) {
        super(config);
    }
}
