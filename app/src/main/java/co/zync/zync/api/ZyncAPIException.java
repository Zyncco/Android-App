package co.zync.zync.api;

// not an actual exception thrown
// exists as a means to easily log
public class ZyncAPIException extends Exception {
    public ZyncAPIException(ZyncError error) {
        super(error.code() + ": " + error.message());
    }
}
