package de.ub0r.android.nocloudshare.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import android.net.Uri;

import java.lang.reflect.Type;

/**
 * @author flx
 */
public class GsonFactory {

    private static class UriTypeAdapter implements JsonSerializer<Uri>, JsonDeserializer<Uri> {

        @Override
        public Uri deserialize(final JsonElement jsonElement, final Type type,
                final JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            return Uri.parse(jsonElement.getAsString());
        }

        @Override
        public JsonElement serialize(final Uri uri, final Type type,
                JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(uri.toString());
        }
    }

    public static Gson getInstance() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Uri.class, new UriTypeAdapter());
        return builder.create();
    }
}
