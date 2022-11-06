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

import kotlin.reflect.KType

public interface ObjectStoreWriter {

    public fun canStoreType(type: KType): Boolean

    public fun keys(): Set<String>

    public fun <T : Any> put(type: KType, key: String, value: T?)

    public fun <T : Any> get(type: KType, key: String): T?

    public fun <T : Any> putRaw(type: KType, key: String, value: T?): Unit = Unit

    public fun <T : Any> getRaw(type: KType, key: String): T? = null

    public fun clear()

    public fun unhandledType(type: KType): Nothing {
        error("Unsupported storage type '$type', install an ObjectStoreSerializer")
    }
}
