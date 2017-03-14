package co.zync.zync.api.generic;

import org.json.JSONObject;

public class ZyncNullTransformer<T> implements ZyncTransformer<T> {
    @Override
    public T transform(JSONObject obj) {
        return null;
    }
}
