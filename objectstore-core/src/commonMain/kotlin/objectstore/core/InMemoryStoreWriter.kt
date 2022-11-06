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

public class InMemoryStoreWriter : ObjectStoreWriter {
    private val lock = SynchronizedObject()
    private val map = mutableMapOf<String, Any?>()

    override fun canStoreType(type: KType): Boolean = true

    override fun keys(): Set<String> = synchronized(lock) { map.keys }.toSet()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: KType, key: String): T? = synchronized(lock) { map[key] } as? T

    override fun <T : Any> put(type: KType, key: String, value: T?): Unit = synchronized(lock) {
        if (value == null) map.remove(key) else map[key] = value
    }

    override fun clear() {
        map.clear()
    }
}
