package com.vijay.jsonwizard.expressions;

import android.util.Log;
import android.util.LruCache;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JsonOrgMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import java.util.EnumSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonExpressionResolver {

    private JSONObject dataObject;
    private DocumentContext dataDocumentContext;
    private ExternalContentLru contentCache;

    public JsonExpressionResolver(JSONObject form) throws JSONException {
        this(form, null);
    }

    public JsonExpressionResolver(JSONObject form, ExternalContentResolver contentResolver)
            throws JSONException {
        if (form.has("data")) {
            dataObject = form.getJSONObject("data");
        } else {
            dataObject = new JSONObject("{}");
        }
        dataDocumentContext = JsonPath.parse(dataObject);
        contentCache = new ExternalContentLru(contentResolver, 10);
    }


    public boolean isValidExpression(String expression) {
        if (expression == null) {
            return false;
        }
        return expression.startsWith("$.") || expression.startsWith("@.");
    }

    private String extractExternalContentReference(String expression) {
        if (!expression.startsWith("@.")) {
            return null;
        }
        int extRefLimit = expression.indexOf("/");
        if (extRefLimit > -1) {
            return expression.substring(2, extRefLimit);
        }
        return null;
    }

    private String extractJsonExpression(String expression) {
        int pos = expression.indexOf("$.");
        return expression.substring(pos);
    }

    public String resolveAsString(String expression, JSONObject instance) throws JSONException {
        JSONArray array = resolveExpression(expression, instance);
        if (array == null || array.length() == 0) {
            return null;
        }
        if (array.isNull(0)) {
            return null;
        }
        return array.getString(0);
    }

    public JSONArray resolveAsArray(String expression, JSONObject instance) throws JSONException {
        JSONArray array = resolveExpression(expression, instance);
        if (array == null || array.length() == 0) {
            return null;
        }
        Object item = array.opt(0);
        if (item instanceof JSONArray) {
            return (JSONArray) item;
        }
        return array;
    }

    private JSONArray resolveExpression(String expression, JSONObject instance) {
        String localExpression = expression;
        String externalReference = extractExternalContentReference(expression);

        DocumentContext localContext = dataDocumentContext;

        if (externalReference != null) {
            localContext = contentCache.get(externalReference);
            if (localContext == null) {
                Log.w("ExpressionResolver", "resolveAsArray: external content " + externalReference
                        + " can not be loaded");
                return null;
            }

            localExpression = extractJsonExpression(expression);
            if (localExpression == null) {
                Log.w("ExpressionResolver",
                        "resolveAsArray: external content expression can not be extracted "
                                + expression);
                return null;
            }
        }

        if (instance != null) {
            localContext.put("$", "current-values", instance);
        }
        JSONArray array = localContext.read(localExpression);

        localContext.delete("current-values");

        return array;
    }

    public boolean existsExpression(String expression, JSONObject instance) throws JSONException {
        String localExpression = expression;
        String externalReference = extractExternalContentReference(expression);

        DocumentContext localContext = dataDocumentContext;

        if (externalReference != null) {
            localContext = contentCache.get(externalReference);
            if (localContext == null) {
                Log.w("ExpressionResolver", "resolveAsArray: external content " + externalReference
                        + " can not be loaded");
                return false;
            }

            localExpression = extractJsonExpression(expression);
            if (localExpression == null) {
                Log.w("ExpressionResolver",
                        "resolveAsArray: external content expression can not be extracted "
                                + expression);
                return false;
            }
        }

        if (instance != null) {
            localContext.put("$", "current-values", instance);
        }
        JSONArray array = new JSONArray();
        try {
            array = localContext.read(localExpression);
        } catch (PathNotFoundException e) {
            Log.d("ExpressionResolver",
                    "existsExpression: checking for missing path " + localExpression);
        }

        localContext.delete("current-values");

        //Check if not null values are present
        if (array != null && array.length() > 0) {
            for (int i = 0; i < array.length(); i++) {
                if (!array.isNull(i)) {
                    return true;
                }
            }
        }

        return false;
    }

    static {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JsonOrgJsonProvider();
            private final MappingProvider mappingProvider = new JsonOrgMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.of(Option.DEFAULT_PATH_LEAF_TO_NULL,
                        Option.ALWAYS_RETURN_LIST);
            }
        });
    }

}
