package io.nessus.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import io.nessus.common.service.LogService;

public abstract class LogSupport {

    protected final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    
    public abstract Config getConfig();
    
    public void logError(Throwable th) {
        log(Level.ERROR, th, null);
    }

    public void logError(Throwable th, String msg) {
        log(Level.ERROR, th, msg);
    }

    public void logError(String msg, Object... args) {
        log(Level.ERROR, msg, args);
    }

    public void logWarn(String msg, Object... args) {
        log(Level.WARN, msg, args);
    }
    
    public void logInfo() {
        logInfo(" ");
    }
    
    public void logInfo(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }
    
    public void logDebug(String msg, Object... args) {
        log(Level.DEBUG, msg, args);
    }

    public void logTrace(String msg, Object... args) {
        log(Level.TRACE, msg, args);
    }

    public void log(Level level, String msg, Object... args) {
        log(level, null, msg, args);
    }

    public void log(Level level, Throwable th, String msg, Object... args) {
        getLogService().log(LOG, level, th, msg, args);
    }

    public boolean isEnabled(Level level) {
    	if (level == Level.ERROR) return LOG.isErrorEnabled();
    	if (level == Level.WARN) return LOG.isWarnEnabled();
    	if (level == Level.INFO) return LOG.isInfoEnabled();
    	if (level == Level.DEBUG) return LOG.isDebugEnabled();
    	return LOG.isTraceEnabled();
    }
    
    private LogService logService;
    
	private LogService getLogService() {
        if (logService == null) {
            logService = getConfig().getService(LogService.class);
            logService.init(getConfig());
        }
        return logService;
    }
}
