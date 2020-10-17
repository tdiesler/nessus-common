package io.nessus.common.service;

import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.event.Level;

public interface LogService extends Service {

    static final ThreadLocal<PrintStream> streamAssociation = new ThreadLocal<>();
    
    public static PrintStream getPrintStream() {
        return streamAssociation.get();
    }
    
    public static PrintStream setPrintStream(PrintStream out) {
        PrintStream prev = streamAssociation.get();
        streamAssociation.set(out);
        return prev;
    }
    
	void log(Logger log, Level level, Throwable th, String msg, Object... args);

	void logError(Logger log, Throwable th, String msg);

	void logError(Logger log, String msg, Object... args);

	void logWarn(Logger log, String msg, Object... args);

	void logInfo(Logger log, String msg, Object... args);

	void logDebug(Logger log, String msg, Object... args);

	void logTrace(Logger log, String msg, Object... args);

}