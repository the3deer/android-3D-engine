package org.the3deer.engine.logger;

import org.the3deer.engine.Model;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Android implementation of the {@link Handler} that intercepts Java Logging messages.
 * It fills the {@link Model#getMessages()} property if a model is being loaded in the current thread.
 * 
 * Note: This handler does NOT write to Logcat because the Android system usually provides 
 * a default handler that redirects JUL to Logcat. This avoids duplicate logs.
 * 
 * @author andresoviedo
 */
public class LogInterceptor extends Handler {

    public LogInterceptor() {
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
        final String message = getFormatter().formatMessage(record);

        // 2. Intercept and fill Model messages if we are in a loading thread
        final Model currentModel = Model.CURRENT.get();
        if (currentModel != null) {
            currentModel.addMessage(record.getLevel(), message);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
