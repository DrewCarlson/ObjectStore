/**
 * ObjectStore
 * Copyright (C) 2022 Drew Carlson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package objectstore.core

import android.content.Context
import android.content.SharedPreferences
import kotlin.reflect.KType

public class SharedPreferenceStoreWriter(
    private val sharedPreferences: SharedPreferences
) : ObjectStoreWriter {

    public constructor(name: String, context: Context, mode: Int = Context.MODE_PRIVATE) :
        this(context.getSharedPreferences(name, mode))

    override fun canStoreType(type: KType): Boolean {
        return when (type.classifier) {
            String::class,
            Boolean::class,
            Int::class,
            Long::class,
            Float::class -> true
            else -> false
        }
    }

    override fun keys(): Set<String> = sharedPreferences.all.keys.toSet()

    override fun <T : Any> put(type: KType, key: String, value: T?) {
        sharedPreferences.edit().apply {
            if (value == null) {
                remove(key)
                return@apply
            }
            when (value) {
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                else -> unhandledType(type)
            }
        }.apply()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: KType, key: String): T? {
        return with(sharedPreferences) {
            when (type.classifier) {
                Int::class -> if (contains(key)) getInt(key, 0) else null
                Long::class -> if (contains(key)) getLong(key, 0) else null
                Float::class -> if (contains(key)) getFloat(key, 0f) else null
                String::class -> if (contains(key)) getString(key, null) else null
                Boolean::class -> if (contains(key)) getBoolean(key, false) else null
                else -> unhandledType(type)
            } as T?
        }
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
