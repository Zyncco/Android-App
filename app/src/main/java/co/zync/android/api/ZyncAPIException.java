package co.zync.android.api;

// not an actual exception thrown
// exists as a means to easily log
public class ZyncAPIException extends Exception {
    private final ZyncError error;

    public ZyncAPIException(ZyncError error) {
        super(error.code() + ": " + error.message());
        this.error = error;
    }

    public ZyncError error() {
        return error;
    }
}
