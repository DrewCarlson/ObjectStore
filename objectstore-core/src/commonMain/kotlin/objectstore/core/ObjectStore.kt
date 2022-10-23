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

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public class ObjectStore(
    private val storeWriter: ObjectStoreWriter,
    private val storeSerializer: ObjectStoreSerializer
) {
    public companion object {
        @PublishedApi
        internal fun keyForType(type: KType): String {
            val classifier = nameFromKtype(type)
            val arguments = type.arguments.joinToString(", ") { keyForType(checkNotNull(it.type)) }
            return if (arguments.isBlank()) classifier else "$classifier<$arguments>"
        }
    }

    private val lock = SynchronizedObject()
    private val flowsMap = mutableMapOf<String, MutableStateFlow<*>>()

    public inline fun <reified T : Any> getOrNull(key: String? = null): T? {
        val type = checkKeyForType(typeOf<T>(), key)
        return getOrNull(typeOf<T>(), key ?: keyForType(type))
    }

    public inline fun <reified T : Any> get(default: T? = null, key: String? = null): T {
        val type = checkKeyForType(typeOf<T>(), key)
        return get(typeOf<T>(), key ?: keyForType(type), default)
    }

    public inline fun <reified T : Any> getFlow(
        default: T? = null,
        key: String? = null
    ): StateFlow<T?> {
        val type = checkKeyForType(typeOf<T>(), key)
        return getFlow(typeOf<T>(), key ?: keyForType(type), default)
    }

    public inline fun <reified T : Any> put(value: T?, key: String? = null) {
        val type = checkKeyForType(typeOf<T>(), key)
        return put(typeOf<T>(), key ?: keyForType(type), value)
    }

    @PublishedApi
    internal fun <T : Any> getOrNull(type: KType, key: String): T? {
        return storeWriter.get(key)?.let {
            storeSerializer.deserialize(type, it)
        }
    }

    @PublishedApi
    internal fun <T : Any> get(type: KType, key: String, default: T? = null): T {
        val value = storeWriter.get(key)
        return if (value == null) {
            requireNotNull(default) { "No value for '$key' and default was null" }
            put(type, key, default)
            default
        } else {
            storeSerializer.deserialize(type, value)
        }
    }

    @PublishedApi
    internal fun <T : Any> getFlow(type: KType, key: String, default: T? = null): StateFlow<T?> {
        @Suppress("UNCHECKED_CAST")
        val flow = synchronized(lock) { flowsMap[key] } as? MutableStateFlow<T>
        return flow ?: run {
            val currentValue = getOrNull<T>(type, key)
            if (currentValue == null && default != null) {
                put(type, key, default)
            }
            MutableStateFlow(currentValue ?: default).also {
                synchronized(lock) { flowsMap[key] = it }
            }
        }
    }

    @PublishedApi
    internal fun <T : Any> put(type: KType, key: String, value: T? = null) {
        if (value == null) {
            storeWriter.put(key, null)
        } else {
            val serialized = storeSerializer.serialize(type, value)
            storeWriter.put(key, serialized)

            synchronized(lock) {
                @Suppress("UNCHECKED_CAST")
                (flowsMap[key] as? MutableStateFlow<T?>)
            }?.update { value }
        }
    }

    @PublishedApi
    internal fun checkKeyForType(type: KType, key: String?): KType {
        when (type) {
            Boolean::class,
            Long::class,
            Int::class,
            Double::class,
            Float::class,
            String::class,
            Char::class,
            Short::class,
            Array::class -> requireNotNull(key) {
                "Storing a primitive type requires a non-null Key."
            }
        }
        return type
    }
}