package io.nessus.common.testing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.nessus.common.BasicConfig;
import io.nessus.common.Config;
import io.nessus.common.LogSupport;
import io.nessus.common.Parameters;
import io.nessus.common.service.BasicLogService;
import io.nessus.common.service.Service;

public abstract class AbstractTest extends LogSupport {

    private Config config;
    
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
    
	protected Config createConfig() {
        Config config = new BasicConfig(new Parameters());
        config.addService(new BasicLogService());
        return config;
    }
    
    @Override
    public Config getConfig() {
        if (config == null) {
            config = createConfig();
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
