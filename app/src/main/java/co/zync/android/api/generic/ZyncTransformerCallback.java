package co.zync.android.api.generic;

import co.zync.android.ZyncApplication;
import co.zync.android.api.ZyncAPIException;
import co.zync.android.api.ZyncError;
import co.zync.android.api.callback.ZyncCallback;
import co.zync.android.utils.ZyncExceptionInfo;
import org.json.JSONObject;

public class ZyncTransformerCallback<T> implements ZyncCallback<JSONObject> {
    private final ZyncTransformer<T> transformer;
    private final ZyncCallback<T> callback;

    public ZyncTransformerCallback(ZyncCallback<T> callback, ZyncTransformer<T> transformer) {
        this.transformer = transformer;
        this.callback = callback;
    }

    @Override
    public void success(JSONObject value) {
        T transformed;

        try {
            transformed = transformer.transform(value);
        } catch (Exception ex) {
            handleError(new ZyncError(-6, "Error transforming value: " +
                    ex.getClass().getSimpleName() + ":" + ex.getMessage()));
            return;
        }

        callback.success(transformed);
    }

    @Override
    public void handleError(ZyncError error) {
        ZyncApplication.LOGGED_EXCEPTIONS.add(new ZyncExceptionInfo(new ZyncAPIException(error)));
        callback.handleError(error);
    }
}