package io.nessus.common.main;

import org.kohsuke.args4j.Option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.nessus.common.Config;

@JsonInclude( Include.NON_EMPTY )
public abstract class AbstractOptions {

    @JsonIgnore
    public final String cmd;
    
    protected AbstractOptions(String cmd) {
        this.cmd = cmd;
    }

    @JsonIgnore
    @Option(name = "--help", help = true)
    public boolean help;

    @JsonIgnore
    @Option(name = "--version")
    public boolean version;

    public void initDefaults(Config config) {
        // do nothing
    }
}
