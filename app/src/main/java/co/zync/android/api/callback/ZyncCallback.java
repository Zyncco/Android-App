package co.zync.android.api.callback;

import co.zync.android.api.ZyncError;

/**
 * @author Mazen Kotb
 */
public interface ZyncCallback<T> {
    void success(T value);

    void handleError(ZyncError error);
}
