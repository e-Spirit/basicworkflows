/*
 * BasicWorkflows Module
 * %%
 * Copyright (C) 2012 - 2025 Crownpeak Technology GmbH - https://www.crownpeak.com
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package to.be.renamed.module.review.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

/**
 * Generates OpenAI-compatible JSON schemas from Java classes.
 * Unlike the SDK's built-in generator, this resolves generic type parameters
 * in Collection fields using {@link Field#getGenericType()}.
 *
 * @since 1.6.0
 */
public final class OpenAiSchemaGenerator {

    private OpenAiSchemaGenerator() {
    }

    public static String generate(final Class<?> type) {
        return createObjectSchema(type).toString();
    }

    private static JsonObject createObjectSchema(final Class<?> type) {
        final JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        final JsonObject properties = new JsonObject();
        final JsonArray required = new JsonArray();

        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (final Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                final String name = field.getName();
                properties.add(name, createFieldSchema(field));
                required.add(name);
            }
            current = current.getSuperclass();
        }

        schema.add("properties", properties);
        schema.add("required", required);
        schema.addProperty("additionalProperties", false);

        return schema;
    }

    private static JsonObject createFieldSchema(final Field field) {
        final Class<?> fieldType = field.getType();

        // Check for @SchemaEnum annotation on String fields
        final SchemaEnum schemaEnum = field.getAnnotation(SchemaEnum.class);
        if (schemaEnum != null && fieldType == String.class) {
            final JsonObject schema = new JsonObject();
            schema.addProperty("type", "string");
            final JsonArray enumValues = new JsonArray();
            for (String value : schemaEnum.value()) {
                enumValues.add(value);
            }
            schema.add("enum", enumValues);
            return schema;
        }

        if (Collection.class.isAssignableFrom(fieldType)) {
            return createArraySchema(field.getGenericType());
        }

        return createTypeSchema(fieldType);
    }

    private static JsonObject createTypeSchema(final Class<?> type) {
        final JsonObject schema = new JsonObject();

        if (type == String.class) {
            schema.addProperty("type", "string");
        } else if (type == boolean.class || type == Boolean.class) {
            schema.addProperty("type", "boolean");
        } else if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class
                || type == BigInteger.class) {
            schema.addProperty("type", "integer");
        } else if (type == float.class || type == Float.class
                || type == double.class || type == Double.class
                || type == BigDecimal.class) {
            schema.addProperty("type", "number");
        } else {
            return createObjectSchema(type);
        }

        return schema;
    }

    private static JsonObject createArraySchema(final Type genericType) {
        final JsonObject schema = new JsonObject();
        schema.addProperty("type", "array");

        if (genericType instanceof ParameterizedType) {
            final Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                schema.add("items", createTypeSchema((Class<?>) typeArgs[0]));
                return schema;
            }
        }

        final JsonObject items = new JsonObject();
        items.addProperty("type", "string");
        schema.add("items", items);
        return schema;
    }
}
