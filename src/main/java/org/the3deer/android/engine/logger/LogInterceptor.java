package org.the3deer.android.engine.logger;

import android.util.Log;

import org.the3deer.android.engine.Model;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Android implementation of the {@link Handler} that intercepts Java Logging messages.
 * 
 * It performs two main tasks:
 * 1. Forwards all logs to Android Logcat.
 * 2. Intercepts WARNING and SEVERE messages and associates them with the current {@link Model}
 *    being processed in the thread (via {@link Model#addMessage(Level, String)}).
 * 
 * @author andresoviedo
 * @author Gemini AI
 */
public class LogInterceptor extends Handler {

    public LogInterceptor() {
        // Default level for the interceptor itself
        setLevel(Level.ALL);
        // Use a simple formatter if one isn't provided
        setFormatter(new SimpleFormatter());
    }

    @Override
    public void publish(final LogRecord record) {
        // pre-condition
        if (record == null || !isLoggable(record)) {
            return;
        }

        // 1. Format the message
        final String formattedMessage = getFormatter().formatMessage(record);
        final String message = record.getThrown() != null ?
                formattedMessage + "\n" + Log.getStackTraceString(record.getThrown()) :
                formattedMessage;

        // 2. Forward to Logcat
        final String tag = record.getLoggerName() != null ? record.getLoggerName() : "JUL";
        final int priority = toLogcatPriority(record.getLevel());
        Log.println(priority, tag, message);

        // 3. Filter WARNING + ERROR (SEVERE) for Model reporting
        if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
            final Model currentModel = Model.CURRENT.get();
            if (currentModel != null) {
                // TODO: add message and show them in the UI
                //currentModel.addMessage(record.getLevel(), message);
            }
        }
    }

    private int toLogcatPriority(Level level) {
        final int val = level.intValue();
        if (val >= Level.SEVERE.intValue()) {
            return Log.ERROR;
        } else if (val >= Level.WARNING.intValue()) {
            return Log.WARN;
        } else if (val >= Level.INFO.intValue()) {
            return Log.INFO;
        } else if (val >= Level.CONFIG.intValue()) {
            return Log.DEBUG;
        } else {
            return Log.VERBOSE;
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
