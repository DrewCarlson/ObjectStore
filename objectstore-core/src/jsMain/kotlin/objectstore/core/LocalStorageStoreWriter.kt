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

import kotlinx.browser.window
import org.w3c.dom.Storage
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.reflect.KType

@Suppress("FunctionName")
public fun LocalStorageStoreWriter(): ObjectStoreWriter {
    return runCatching { StorageStoreWriter(window.localStorage) }.getOrElse {
        println("WARNING: LocalStorageStoreWriter does not support Node.js, InMemoryStoreWriter will be used.")
        println("WARNING: Please use InMemoryStoreWriter explicitely or use FileStoreWriter from the `objectstore-fs` module.")
        InMemoryStoreWriter()
    }
}

public class StorageStoreWriter(
    private val storage: Storage
) : ObjectStoreWriter {

    override fun canStoreType(type: KType): Boolean {
        return type.classifier == String::class
    }

    override fun keys(): Set<String> {
        val keys = mutableSetOf<String>()
        repeat(storage.length) { i ->
            storage.key(i)?.run(keys::add)
        }
        return keys.toSet()
    }

    override fun <T : Any> put(type: KType, key: String, value: T?) {
        if (value == null) {
            storage.removeItem(key)
        } else {
            storage[key] = value as String
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: KType, key: String): T? {
        return storage[key] as? T?
    }

    override fun clear() {
        storage.clear()
    }
}
