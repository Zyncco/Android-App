package co.zync.zync.api.callback;

import co.zync.zync.api.ZyncError;

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
