// https://github.com/KSashaDF/JSON-to-NBT-Converter/blob/master/converter/JsonToNbt.java
// I hope it's ok to include it here in my package
package empty.sync.emptysync;
// author: https://github.com/KSashaDF
import com.google.gson.*;
import java.util.Map;
import java.util.Objects;

import net.minecraft.nbt.*;

// thank you!

final class NBTConverter {
    public static NbtElement toNbt(JsonElement jsonElement) {
        if (jsonElement instanceof JsonPrimitive jsonPrimitive) {
            if (jsonPrimitive.isBoolean()) {
                boolean value = jsonPrimitive.getAsBoolean();
                if (value)
                    return NbtByte.of(true);
                else
                    return NbtByte.of(false);
            } else if (jsonPrimitive.isNumber()) {
                Number number = jsonPrimitive.getAsNumber();
                if (number instanceof Byte)
                    return NbtByte.of(number.byteValue());
                else if (number instanceof Short)
                    return NbtShort.of(number.shortValue());
                else if (number instanceof Integer)
                    return NbtInt.of(number.intValue());
                else if (number instanceof Long)
                    return NbtLong.of(number.longValue());
                else if (number instanceof Float)
                    return NbtFloat.of(number.floatValue());
                else if (number instanceof Double)
                    return NbtDouble.of(number.doubleValue());
                else
                    return NbtDouble.of(number.doubleValue());

            } else if (jsonPrimitive.isString())
                return NbtString.of(jsonPrimitive.getAsString());
        } else if (jsonElement instanceof JsonArray jsonArray) {
            NbtList nbtList = new NbtList();
            for (JsonElement element : jsonArray)
                nbtList.add(toNbt(element));
            return nbtList;
        } else if (jsonElement instanceof JsonObject jsonObject) {
            NbtCompound nbtCompound = new NbtCompound();
            for (Map.Entry<String, JsonElement> jsonEntry : jsonObject.entrySet())
                nbtCompound.put(jsonEntry.getKey(), toNbt(jsonEntry.getValue()));
            return nbtCompound;
        } else if (jsonElement instanceof JsonNull) {
            return new NbtCompound();
        }
        throw new AssertionError();
    }

    public static NbtCompound toNbtCompound(JsonObject jsonObject) {
        NbtCompound nbtCompound = new NbtCompound();
        for (Map.Entry<String, JsonElement> jsonEntry : jsonObject.entrySet())
            nbtCompound.put(jsonEntry.getKey(), toNbt(jsonEntry.getValue()));
        return nbtCompound;
    }

    public static JsonElement toJson(NbtElement nbtElement) {
        switch (nbtElement.getType()) {
            case NbtElement.BYTE_TYPE:
                return new JsonPrimitive(((NbtByte) nbtElement).byteValue());
            case NbtElement.SHORT_TYPE:
                return new JsonPrimitive(((NbtShort) nbtElement).shortValue());
            case NbtElement.INT_TYPE:
                return new JsonPrimitive(((NbtInt) nbtElement).intValue());
            case NbtElement.LONG_TYPE:
                return new JsonPrimitive(((NbtLong) nbtElement).longValue());
            case NbtElement.FLOAT_TYPE:
                return new JsonPrimitive(((NbtFloat) nbtElement).floatValue());
            case NbtElement.DOUBLE_TYPE:
                return new JsonPrimitive(((NbtDouble) nbtElement).doubleValue());
            case NbtElement.STRING_TYPE:
                return new JsonPrimitive(nbtElement.asString());
            case NbtElement.LIST_TYPE:
                NbtList nbtList = (NbtList) nbtElement;
                JsonArray jsonArray = new JsonArray();
                for (NbtElement element : nbtList)
                    jsonArray.add(toJson(element));
               return jsonArray;
            case NbtElement.COMPOUND_TYPE:
                NbtCompound nbtCompound = (NbtCompound) nbtElement;
                JsonObject jsonObject = new JsonObject();
                for (String key : nbtCompound.getKeys())
                    jsonObject.add(key, toJson(Objects.requireNonNull(nbtCompound.get(key))));
                return jsonObject;
            default:
                throw new AssertionError();
        }
    }
}