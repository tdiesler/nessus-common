package io.nessus.common.service;

import org.slf4j.Logger;
import org.slf4j.event.Level;

public interface LogService extends Service {

	void log(Logger log, Level level, Throwable th, String msg, Object... args);

	void logError(Logger log, Throwable th, String msg);

	void logError(Logger log, String msg, Object... args);

	void logWarn(Logger log, String msg, Object... args);

	void logInfo(Logger log, String msg, Object... args);

	void logDebug(Logger log, String msg, Object... args);

	void logTrace(Logger log, String msg, Object... args);

}