package io.nessus.common.service;

import io.nessus.common.Config;

public interface Service {
 
	default String getType() {
		return getClass().getName();
	}
	
	default void init(Config config) {
	}
}
