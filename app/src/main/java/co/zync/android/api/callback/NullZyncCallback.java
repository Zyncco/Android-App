package co.zync.android.api.callback;

import co.zync.android.api.ZyncError;

/**
 * literally does absolutely nothing
  */
public class NullZyncCallback<T> implements ZyncCallback<T> {
    @Override
    public void success(T value) {
    }

    @Override
    public void handleError(ZyncError error) {
    }
}
