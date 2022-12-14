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

public interface ObjectStoreSerializer {

    public companion object : ObjectStoreSerializer {
        override fun <T : Any> serialize(type: KType, data: T): String {
            error("ObjectStore requires a custom serializer to handle '$type'")
        }
        override fun <T : Any> deserialize(type: KType, data: String): T {
            error("ObjectStore requires a custom serializer to handle '$type'")
        }
    }

    public fun <T : Any> serialize(type: KType, data: T): String

    public fun <T : Any> deserialize(type: KType, data: String): T
}
