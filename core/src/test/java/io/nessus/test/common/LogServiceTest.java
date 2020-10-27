package io.nessus.test.common;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.common.BasicConfig;
import io.nessus.common.testing.AbstractTest;

public class LogServiceTest extends AbstractTest<BasicConfig> {

    private final Logger LOG = LoggerFactory.getLogger(LogServiceTest.class);

    @Test
    public void testDefault() throws Exception {

    	logInfo("{}", "hello");
    	
    	// [#2] logError("{}", ex) logs stack trace unexpectedly
    	logInfo("{}", new RuntimeException("hello"));
    	
    	LOG.info("{}", new RuntimeException("hello"));
    }  
}
