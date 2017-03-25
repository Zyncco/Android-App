package co.zync.zync.utils;

public class ZyncExceptionInfo {
    // exception causing the issue
    private final Exception ex;
    // when it happened
    private final long timestamp;
    // the action, if available, that the code creating
    // the exception info is trying to do (e.g. "share file")
    private final String action;

    public ZyncExceptionInfo(Exception ex) {
        this(ex, "Unknown");
    }

    public ZyncExceptionInfo(Exception ex, String action) {
        this.ex = ex;
        this.timestamp = System.currentTimeMillis();
        this.action = action;
    }

    public Exception ex() {
        return ex;
    }

    public long timestamp() {
        return timestamp;
    }

    public String action() {
        return action;
    }
}
