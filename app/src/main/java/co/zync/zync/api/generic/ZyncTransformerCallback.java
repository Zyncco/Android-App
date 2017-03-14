package co.zync.zync.api.generic;

import co.zync.zync.api.ZyncAPI;
import co.zync.zync.api.ZyncError;
import org.json.JSONObject;

public class ZyncTransformerCallback<T> implements ZyncAPI.ZyncCallback<JSONObject> {
    private final ZyncTransformer<T> transformer;
    private final ZyncAPI.ZyncCallback<T> callback;

    public ZyncTransformerCallback(ZyncAPI.ZyncCallback<T> callback, ZyncTransformer<T> transformer) {
        this.transformer = transformer;
        this.callback = callback;
    }

    @Override
    public void success(JSONObject value) {
        callback.success(transformer.transform(value));
    }

    @Override
    public void handleError(ZyncError error) {
        callback.handleError(error);
    }
}