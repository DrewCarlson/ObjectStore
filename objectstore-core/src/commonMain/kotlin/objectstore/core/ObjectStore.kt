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
    private val storeWriter: ObjectStoreWriter = InMemoryStoreWriter(),
    private val storeSerializer: ObjectStoreSerializer = ObjectStoreSerializer
) {
    public companion object {
        @PublishedApi
        internal fun keyForType(type: KType): String {
            val classifier = nameFromKtype(type)
            val arguments = type.arguments.joinToString(", ") { keyForType(checkNotNull(it.type)) }
            return if (arguments.isBlank()) classifier else "$classifier<$arguments>"
        }

        private val STRING: KType = typeOf<String>()
    }

    private val lock = SynchronizedObject()
    private val flowsMap = mutableMapOf<String, MutableStateFlow<*>>()

    public fun keys(): Set<String> {
        return storeWriter.keys()
    }

    public fun clear() {
        storeWriter.clear()
        flowsMap.forEach { (_, flow) ->
            @Suppress("UNCHECKED_CAST")
            (flow as MutableStateFlow<Any?>).update { null }
        }
    }

    public inline fun <reified T : Any> getOrNull(key: String? = null): T? {
        val type = checkKeyForType(typeOf<T>(), key)
        return getOrNull(type, key ?: keyForType(type))
    }

    public inline fun <reified T : Any> get(default: T? = null, key: String? = null): T {
        val type = checkKeyForType(typeOf<T>(), key)
        return get(type, key ?: keyForType(type), default)
    }

    public inline fun <reified T : Any> getFlow(
        default: T? = null,
        key: String? = null
    ): StateFlow<T?> {
        val type = checkKeyForType(typeOf<T>(), key)
        return getFlow(type, key ?: keyForType(type), default)
    }

    public inline fun <reified T : Any> put(value: T?, key: String? = null) {
        val type = checkKeyForType(typeOf<T>(), key)
        put(type, key ?: keyForType(type), value)
    }

    public inline fun <reified T : Any> remove(key: String? = null) {
        val type = checkKeyForType(typeOf<T>(), key)
        remove<T>(type, key ?: keyForType(type))
    }

    @PublishedApi
    internal fun <T : Any> getOrNull(type: KType, key: String): T? {
        return if (storeWriter.canStoreType(type)) {
            storeWriter.get(type, key)
        } else {
            storeWriter.get<String>(STRING, key)?.let {
                storeSerializer.deserialize(type, it)
            }
        }
    }

    @PublishedApi
    internal fun <T : Any> get(type: KType, key: String, default: T? = null): T {
        return if (storeWriter.canStoreType(type)) {
            requireNotNull(storeWriter.get(type, key) ?: default?.also { put(type, key, it) }) {
                "No value for '$key' and default was null"
            }
        } else {
            val value = storeWriter.get<String>(STRING, key)
            if (value == null) {
                requireNotNull(default) { "No value for '$key' and default was null" }
                put(type, key, default)
                default
            } else {
                storeSerializer.deserialize(type, value)
            }
        }
    }

    @PublishedApi
    internal fun <T : Any> getFlow(type: KType, key: String, default: T? = null): StateFlow<T?> {
        return getStateFlow(key) ?: run {
            val currentValue = getOrNull<T>(type, key)
            if (currentValue == null && default != null) {
                put(type, key, default)
            }
            createStateFlow(key, currentValue ?: default)
        }
    }

    @PublishedApi
    internal fun <T : Any> put(type: KType, key: String, value: T? = null) {
        if (value == null) {
            storeWriter.put(type, key, null)
        } else {
            if (storeWriter.canStoreType(type)) {
                storeWriter.put(type, key, value)
            } else {
                val serialized = storeSerializer.serialize(type, value)
                storeWriter.put(STRING, key, serialized)
            }
        }

        updateStateFlow(key, value)
    }

    @PublishedApi
    internal fun <T : Any> remove(type: KType, key: String) {
        storeWriter.put<T>(type, key, null)
        updateStateFlow(key, null)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> updateStateFlow(key: String, value: T?) {
        getStateFlow<T>(key)?.update { value }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getStateFlow(key: String): MutableStateFlow<T?>? {
        return synchronized(lock) { flowsMap[key] } as? MutableStateFlow<T?>
    }

    private fun <T : Any> createStateFlow(key: String, value: T?): MutableStateFlow<T?> {
        val stateFlow = MutableStateFlow(value)
        synchronized(lock) { flowsMap[key] = stateFlow }
        return stateFlow
    }

    @PublishedApi
    internal fun checkKeyForType(type: KType, key: String?): KType {
        when (type.classifier) {
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
