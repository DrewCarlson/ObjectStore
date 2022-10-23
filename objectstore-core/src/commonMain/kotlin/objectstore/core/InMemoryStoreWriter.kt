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

public class InMemoryStoreWriter : ObjectStoreWriter {
    private val lock = SynchronizedObject()
    private val map = mutableMapOf<String, String>()

    override fun get(key: String): String? = synchronized(lock) { map[key] }

    override fun put(key: String, value: String?): Unit = synchronized(lock) {
        if (value == null) map.remove(key) else map[key] = value
    }
}