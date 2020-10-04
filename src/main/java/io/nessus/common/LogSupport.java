package io.nessus.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import io.nessus.common.service.BasicLogService;
import io.nessus.common.service.LogService;

public abstract class LogSupport {

    protected final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    
    protected abstract Config getConfig();
    
    protected void logError(Throwable th) {
        logError(th, null);
    }

    protected void logError(String msg, Object... args) {
        logError(null, msg, args);
    }

    protected void logError(Throwable th, String msg, Object... args) {
        log(Level.ERROR, th, msg, args);
    }

    protected void logWarn(String msg, Object... args) {
        log(Level.WARN, msg, args);
    }
    
    protected void logInfo() {
        logInfo(" ");
    }
    
    protected void logInfo(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }
    
    protected void logDebug(String msg, Object... args) {
        log(Level.DEBUG, msg, args);
    }

    protected void logTrace(String msg, Object... args) {
        log(Level.TRACE, msg, args);
    }

    protected void log(Level level, String msg, Object... args) {
        log(level, null, msg, args);
    }

    protected void log(Level level, Throwable th, String msg, Object... args) {
        getLogService().log(LOG, level, th, msg, args);
    }

    protected boolean isEnabled(Level level) {
    	if (level == Level.ERROR) return LOG.isErrorEnabled();
    	if (level == Level.WARN) return LOG.isWarnEnabled();
    	if (level == Level.INFO) return LOG.isInfoEnabled();
    	if (level == Level.DEBUG) return LOG.isDebugEnabled();
    	return LOG.isTraceEnabled();
    }
    
    private transient BasicLogService logService;
    
	private LogService getLogService() {
        if (logService == null) {
            logService = getConfig().getService(BasicLogService.class);
            logService.init(getConfig());
        }
        return logService;
    }
}
