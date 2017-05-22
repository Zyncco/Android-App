package co.zync.android.api.generic;

import org.json.JSONObject;

/**
 * @author Mazen Kotb
 */
public interface ZyncTransformer<T> {
    T transform(JSONObject obj) throws Exception;
}