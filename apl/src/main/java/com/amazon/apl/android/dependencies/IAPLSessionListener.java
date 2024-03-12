package com.amazon.apl.android.dependencies;

import androidx.annotation.Nullable;
import com.amazon.apl.android.Session;

/**
 * Interface for handling Log Events from the Log Command and Dev Tools Log Domain.
 */
public interface IAPLSessionListener {

    /**
     * This method will get called, when there is a log to write.
     *
     * @param level The specific {@link Session.LogEntryLevel}; NONE, TRACE, DEBUG, INFO, WARN, ERROR, CRITICAL.
     * @param source The specific {@link Session.LogEntrySource} from where the log is being emitted from.
     * @param message A {@link String} with the message for the log event.
     * @param arguments Any additional arguments that may be attached to the log.
     */
    void write(Session.LogEntryLevel level, Session.LogEntrySource source, String message, @Nullable Object[] arguments);
}
