package alexiil.mc.mod.load;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 
 * Logging helper for Custom Loading Screen on Legacy Fabric.
 */
public class CLSLog {
    private static Logger log;

    public static Logger log() {
        if (log == null) {
            log = LogManager.getLogger("CustomLoadingScreen");
        }
        return log;
    }

    public static void info(String toLog) {
        log(Level.INFO, toLog);
    }

    public static void warn(String text) {
        log(Level.WARN, text);
    }

    public static void warn(String message, Throwable thrown) {
        log(Level.WARN, message, thrown);
    }

    public static void error(String message) {
        log(Level.ERROR, message);
    }

    public static void error(String message, Throwable t) {
        log(Level.ERROR, message, t);
    }

    public static void trace(String message) {
        log(Level.TRACE, message);
    }

    public static void log(Level level, String text) {
        log().log(level, text);
    }

    public static void log(Level level, String message, Throwable t) {
        log().log(level, message, t);
    }

    @Deprecated
    public static void temp(String text) {
        info(text);
    }
}
