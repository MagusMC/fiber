package me.zeroeightsix.fiber.api.serialization;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import me.zeroeightsix.fiber.api.schema.type.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

public class JsonTypeSerializer implements TypeSerializer<JsonObject> {
    @Override
    public void serialize(BooleanConfigType type, JsonObject json) {
        json.put("type", new JsonPrimitive("boolean"));
    }

    @Override
    public void serialize(DecimalConfigType type, JsonObject json) {
        json.put("type", new JsonPrimitive("number"));
        BigDecimal min = type.getMinimum();
        if (min != null) {
            json.put("min", new JsonPrimitive(min));
        }
        BigDecimal maximum = type.getMaximum();
        if (maximum != null) {
            json.put("max", new JsonPrimitive(maximum));
        }
        BigDecimal increment = type.getIncrement();
        if (increment != null) {
            json.put("increment", new JsonPrimitive(increment));
        }
    }

    @Override
    public void serialize(EnumConfigType type, JsonObject json) {
        json.put("type", new JsonPrimitive("enum"));
        JsonArray values = new JsonArray();
        for (String value : type.getValidValues()) {
            values.add(new JsonPrimitive(value));
        }
        json.put("values", values);
    }

    @Override
    public void serialize(ListConfigType<?> type, JsonObject json) {
        json.put("type", new JsonPrimitive("list"));
        JsonObject elementType = new JsonObject();
        type.getElementType().serialize(this, elementType);
        json.put("elementType", elementType);
        json.put("unique", new JsonPrimitive(type.hasUniqueElements()));
        json.put("minSize", new JsonPrimitive(type.getMinSize()));
        json.put("maxSize", new JsonPrimitive(type.getMaxSize()));
    }

    @Override
    public void serialize(MapConfigType<?> type, JsonObject json) {
        json.put("type", new JsonPrimitive("map"));
        JsonObject valueType = new JsonObject();
        type.getValueType().serialize(this, valueType);
        json.put("valueType", valueType);
        json.put("minSize", new JsonPrimitive(type.getMinSize()));
        json.put("maxSize", new JsonPrimitive(type.getMaxSize()));
    }

    @Override
    public void serialize(RecordConfigType type, JsonObject json) {
        json.put("type", new JsonPrimitive("record"));
        JsonArray fields = new JsonArray();
        for (Map.Entry<String, ConfigType<?>> entry : type.getFields().entrySet()) {
            JsonObject field = new JsonObject();
            field.put("name", new JsonPrimitive(entry.getKey()));
            JsonObject fieldType = new JsonObject();
            entry.getValue().serialize(this, fieldType);
            field.put("type", fieldType);
            fields.add(field);
        }
        json.put("fields", fields);
    }

    @Override
    public void serialize(StringConfigType type, JsonObject json) {
        json.put("type", new JsonPrimitive("string"));
        json.put("minLength", new JsonPrimitive(type.getMinLength()));
        json.put("maxLength", new JsonPrimitive(type.getMaxLength()));
        Pattern pattern = type.getPattern();
        if (pattern != null) {
            json.put("pattern", new JsonPrimitive(pattern.toString()));
        }
    }
}
