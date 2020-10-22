package io.nessus.common.testing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;

import io.nessus.common.BasicConfig;
import io.nessus.common.CheckedExceptionWrapper;
import io.nessus.common.Config;
import io.nessus.common.LogSupport;
import io.nessus.common.Parameters;
import io.nessus.common.service.BasicLogService;
import io.nessus.common.service.Service;

public abstract class AbstractTest<T extends Config> extends LogSupport {

    private T config;
    
    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    	if (config != null) {
            config.closeServices();
    	}
    }

    protected <S extends Service> S getService(Class<S> type) {
        return getConfig().getService(type);
    }

    protected String getSimpleName() {
        String name = getClass().getSimpleName();
        if (name.endsWith("Test")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }
    
    protected Path getOutPath() {
        Path outdir = Paths.get("target", getSimpleName());
        outdir.toFile().mkdirs();
        return outdir;
    }
    
    protected PrintStream getPrintStream() throws IOException {
        return getPrintStream(getSimpleName());
    }

    protected PrintStream getPrintStream(String fname) throws IOException {
    	File file = Paths.get("target", fname + ".txt").toFile();
        return new PrintStream(new FileOutputStream(file));
    }
    
	@SuppressWarnings("unchecked")
	protected T createConfig() throws Exception {
        Config config = new BasicConfig(new Parameters());
        config.addService(new BasicLogService());
        return (T) config;
    }
    
    @Override
    public T getConfig() {
        if (config == null) {
            try {
				config = createConfig();
			} catch (Exception ex) {
				throw CheckedExceptionWrapper.create(ex);
			}
            config.initServices();
        }
        return config;
    }
    
    protected void sleepSafe(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // ignore
        }
    }
}
