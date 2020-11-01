package io.nessus.common.service;

import io.nessus.common.Config;

public interface Service {
 
	@SuppressWarnings("unchecked")
	default <T extends Service> Class<T> getType() {
		return (Class<T>) getClass();
	}
	
	default void init(Config config) {
	}

	default void close() {
	}
}
