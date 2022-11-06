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
import kotlin.reflect.KType

public fun ObjectStoreWriter.memCached(): ObjectStoreWriter {
    return memCached(InMemoryStoreWriter())
}

internal fun ObjectStoreWriter.memCached(inMemoryStoreWriter: InMemoryStoreWriter): ObjectStoreWriter {
    return MemCachedStoreWriter(this, inMemoryStoreWriter)
}

public class MemCachedStoreWriter internal constructor(
    private val storeWriter: ObjectStoreWriter,
    private val inMemoryStoreWriter: InMemoryStoreWriter
) : ObjectStoreWriter {

    public constructor(storeWriter: ObjectStoreWriter) : this(storeWriter, InMemoryStoreWriter())

    private val lock = SynchronizedObject()
    private val knownKeys = mutableSetOf<String>()
    private var keysHaveSynced = false

    override fun canStoreType(type: KType): Boolean = storeWriter.canStoreType(type)

    override fun keys(): Set<String> {
        return if (keysHaveSynced) {
            synchronized(lock) { knownKeys }.toSet()
        } else {
            val keys = storeWriter.keys()
            synchronized(lock) { knownKeys.addAll(keys) }
            keysHaveSynced = true
            keys
        }
    }

    override fun <T : Any> putRaw(type: KType, key: String, value: T?) {
        synchronized(lock) {
            if (value == null) {
                knownKeys.remove(key)
            } else {
                knownKeys.add(key)
            }
        }
        inMemoryStoreWriter.put(type, key, value)
    }

    override fun <T : Any> getRaw(type: KType, key: String): T? {
        return inMemoryStoreWriter.get(type, key)
    }

    override fun <T : Any> put(type: KType, key: String, value: T?) {
        storeWriter.put(type, key, value)
    }

    override fun <T : Any> get(type: KType, key: String): T? {
        return storeWriter.get(type, key)
    }

    override fun clear() {
        synchronized(lock) { knownKeys.clear() }
        inMemoryStoreWriter.clear()
        storeWriter.clear()
    }
}
