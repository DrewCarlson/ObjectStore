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
import kotlin.reflect.typeOf
import kotlin.test.*

class NoSerializerTest : RobolectricTestCases() {

    private lateinit var store: ObjectStore
    private lateinit var storeWriter: ObjectStoreWriter

    @BeforeTest
    fun setup() {
        val defaultStoreWriter = getStoreWriter(false).takeUnless { it is InMemoryStoreWriter }
        storeWriter = defaultStoreWriter ?: BasicTypeStoreWriter
        store = ObjectStore(storeWriter = storeWriter)
    }

    @Test
    fun testSetFailsWithType() {
        assertFailsWith<IllegalStateException> {
            store.put(TestClass())
        }
    }

    @Test
    fun testSetAndGetWithBasicType() {
        if (storeWriter.canStoreType(typeOf<Int>())) {
            store.put(value = 1, "test")
            assertEquals(1, store.get(key = "test"))
        }
        if (storeWriter.canStoreType(typeOf<Boolean>())) {
            store.put(value = true, "test2")
            assertEquals(true, store.get(key = "test2"))
        }
        if (storeWriter.canStoreType(typeOf<Long>())) {
            store.put(value = 1L, "test3")
            assertEquals(1L, store.get(key = "test3"))
        }
        if (storeWriter.canStoreType(typeOf<Float>())) {
            store.put(value = 1f, "test4")
            assertEquals(1f, store.get(key = "test4"))
        }
    }
}

object BasicTypeStoreWriter : ObjectStoreWriter {

    private val data = mutableMapOf<String, Any?>()

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

    override fun keys(): Set<String> = data.keys.toSet()

    override fun <T : Any> put(type: KType, key: String, value: T?) {
        data[key] = value
    }

    override fun <T : Any> get(type: KType, key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return data[key] as? T?
    }

    override fun clear() {
        data.clear()
    }
}
