package co.zync.zync.api.generic;

import org.json.JSONObject;

public class NullZyncTransformer<T> implements ZyncTransformer<T> {
    @Override
    public T transform(JSONObject obj) {
        return null;
    }
}
