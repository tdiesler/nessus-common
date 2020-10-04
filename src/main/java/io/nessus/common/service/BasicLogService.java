package io.nessus.common.service;

import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.event.Level;

public class BasicLogService implements LogService {
    
    static final ThreadLocal<PrintStream> streamAssociation = new ThreadLocal<>();
    
    public static PrintStream getPrintStream() {
        return streamAssociation.get();
    }
    
    public static PrintStream setPrintStream(PrintStream out) {
        PrintStream prev = streamAssociation.get();
        streamAssociation.set(out);
        return prev;
    }
    
    @Override
    public String getType() {
    	return LogService.class.getName();
    }
    
	@Override
	public void log(Logger log, Level level, Throwable th, String msg, Object[] args) {
        
        if (level == Level.ERROR) 
            logError(log, th, msg, args);
        
        else if (level == Level.WARN) 
            logWarn(log, msg, args);
        
        else if (level == Level.INFO) 
            logInfo(log, msg, args);
        
        else if (level == Level.DEBUG) 
            logDebug(log, msg, args);
        
        else if (level == Level.TRACE) 
            logTrace(log, msg, args);
    }
    
    @Override
	public void logError(Logger log, Throwable th, String msg, Object... args) {
        log.error(msg, th);
    }
    
    @Override
	public void logWarn(Logger log, String msg, Object... args) {
        log.warn(msg, args);
    }
    
    @Override
	public void logInfo(Logger log, String msg, Object... args) {
        log.info(msg, args);
    }

    @Override
	public void logDebug(Logger log, String msg, Object... args) {
        log.debug(msg, args);
    }
    
    @Override
	public void logTrace(Logger log, String msg, Object... args) {
        log.trace(msg, args);
    }
    
    public static String format(String msg, Object... args) {
    	if (args.length > 0) {
            msg = msg.replace("%", "%%");
            msg = msg.replace("{}", "%s");
            msg = String.format(msg, args);
    	}
        return msg;
    }
}
