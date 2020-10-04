package io.nessus.common.main;

import org.kohsuke.args4j.Option;

import io.nessus.common.Config;

public abstract class AbstractOptions {

    public final String cmd;
    
    protected AbstractOptions(String cmd) {
        this.cmd = cmd;
    }

    @Option(name = "--help", help = true)
    public boolean help;

    @Option(name = "--version")
    public boolean version;

    public void initDefaults(Config config) {
        // do nothing
    }
}
