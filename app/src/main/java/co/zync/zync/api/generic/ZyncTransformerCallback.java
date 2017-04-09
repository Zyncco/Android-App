package co.zync.zync.api.generic;

import co.zync.zync.ZyncApplication;
import co.zync.zync.api.ZyncAPIException;
import co.zync.zync.api.ZyncError;
import co.zync.zync.api.callback.ZyncCallback;
import co.zync.zync.utils.ZyncExceptionInfo;
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