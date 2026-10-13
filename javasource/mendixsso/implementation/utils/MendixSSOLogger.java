package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.logging.LogLevel;

import java.io.PrintWriter;
import java.io.StringWriter;

import static mendixsso.proxies.constants.Constants.getLogNode;

public class MendixSSOLogger {

    private static final ILogNode LOGGER = Core.getLogger(getLogNode());

    private MendixSSOLogger() {
    }

    public static void info(String message, Object... args) {
        log(LogLevel.INFO, message, args);
    }

    public static void info(Throwable t, String message, Object... args) {
        log(LogLevel.INFO, t, message, args);
    }

    public static void warn(String message, Object... args) {
        log(LogLevel.WARNING, message, args);
    }

    public static void warn(Throwable t, String message, Object... args) {
        log(LogLevel.WARNING, t, message, args);
    }

    public static void error(String message, Object... args) {
        log(LogLevel.ERROR, message, args);
    }

    public static void error(Throwable t, String message, Object... args) {
        log(LogLevel.ERROR, t, message, args);
    }

    public static void debug(String message, Object... args) {
        if (LOGGER.isDebugEnabled()) {
            log(LogLevel.DEBUG, message, args);
        }
    }

    public static void debug(Throwable t, String message, Object... args) {
        if (LOGGER.isDebugEnabled()) {
            log(LogLevel.DEBUG, t, message, args);
        }
    }

    private static void log(LogLevel level, String message, Object... args) {
        log(level, null, message, args);
    }

    private static void log(LogLevel level, Throwable t, String message, Object... args) {
        String formatted = format(message, args);
        if (t != null) {
            formatted += "\nStackTrace:\n" + getStackTrace(t);
        }
        LOGGER.log(level, formatted);
    }

    private static String format(String message, Object... args) {
        return args == null || args.length == 0 ? message : String.format(message, args);
    }

    private static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
