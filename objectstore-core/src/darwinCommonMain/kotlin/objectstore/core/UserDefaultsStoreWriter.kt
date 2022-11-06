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

import platform.Foundation.NSUserDefaults
import platform.Foundation.setValue
import kotlin.reflect.KType

public class UserDefaultsStoreWriter(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : ObjectStoreWriter {

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

    @Suppress("UNCHECKED_CAST")
    override fun keys(): Set<String> = defaults.dictionaryRepresentation().keys as Set<String>

    override fun <T : Any> put(type: KType, key: String, value: T?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
            return
        }
        return when (value) {
            is String -> defaults.setValue(value = value, forKey = key)
            is Boolean -> defaults.setBool(value = value, forKey = key)
            is Int -> defaults.setObject(value = value, forKey = key)
            is Long -> defaults.setObject(value = value, forKey = key)
            is Float -> defaults.setFloat(value = value, forKey = key)
            else -> unhandledType(type)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: KType, key: String): T? {
        return when (type.classifier) {
            String::class -> defaults.stringForKey(key) as T?
            Boolean::class -> defaults.boolForKey(key) as T?
            Int::class -> defaults.objectForKey(key) as T?
            Long::class -> defaults.objectForKey(key) as T?
            Float::class -> defaults.floatForKey(key) as T?
            else -> error("Cannot retrieve type '$type'")
        }
    }

    override fun clear() {
        keys().forEach(defaults::removeObjectForKey)
    }
}
