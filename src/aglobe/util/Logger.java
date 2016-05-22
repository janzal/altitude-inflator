package aglobe.util;

/**
 * Created by janzaloudek on 19/05/16.
 */
public class Logger {
    public static void logWarning(String message) {
        log(message);
    }

    public static void logWarning(String message, Throwable e) {
        log(message, e);
    }

    public static void logSevere(String message) {
        log(message);
    }

    public static void logSevere(String message, Throwable e) {
        log(message, e);
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public static void log(String message, Throwable e) {
        System.out.println(message + " [" + e.getMessage() + "]");
    }
}
