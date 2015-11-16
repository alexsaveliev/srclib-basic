package com.sourcegraph.toolchain.php.composer;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.sourcegraph.toolchain.php.composer.schema.ComposerSchemaJson;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ComposerConfiguration {

    public static ComposerSchemaJson getConfiguration(File composerLock) throws IOException {
        // when object contains list of strings, we supporting both
        // foo: [bar, baz] and foo: bar
        Type stringListType = new TypeToken<List<String>>() {
        }.getType();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(stringListType, new StringListTypeAdapter())
                .create();
        try (FileReader reader = new FileReader(composerLock)) {
            return gson.fromJson(reader, ComposerSchemaJson.class);
        }
    }

    private static class StringListTypeAdapter implements JsonDeserializer<List<String>> {
        public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) {
            List<String> values = new ArrayList<>();
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    values.add(ctx.deserialize(e, String.class));
                }
            } else if (json.isJsonObject()) {
                values.add(ctx.deserialize(json, String.class));
            } else if (json.isJsonPrimitive()) {
                JsonPrimitive primitive = (JsonPrimitive) json;
                if (primitive.isString()) {
                    values.add(primitive.getAsString());
                } else {
                    throw new RuntimeException("Unexpected JSON type: " + primitive);
                }
            } else {
                throw new RuntimeException("Unexpected JSON type: " + json.getClass());
            }
            return values;
        }
    }
}
