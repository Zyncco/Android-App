package co.zync.zync.api.callback;

import co.zync.zync.api.ZyncError;

/**
 * @author Mazen Kotb
 */
public interface ZyncCallback<T> {
    void success(T value);

    void handleError(ZyncError error);
}
