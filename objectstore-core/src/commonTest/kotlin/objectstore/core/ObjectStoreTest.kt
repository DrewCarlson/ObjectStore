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

import kotlinx.serialization.Serializable
import objectstore.cbor.CborStoreSerializer
import objectstore.json.JsonStoreSerializer
import objectstore.protobuf.ProtoBufStoreSerializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.*

expect fun getStoreWriter(secure: Boolean): ObjectStoreWriter

expect abstract class RobolectricTestCases()

@Serializable
data class TestClass(
    val a: String = "test",
    val b: Long = Long.MAX_VALUE
)

@Serializable
data class TestClassWithGeneric<T>(
    val value: T?
)

class NoSerializerStoreTest : ObjectStoreTest(CborStoreSerializer())
class CborObjectStoreTest : ObjectStoreTest(CborStoreSerializer())
class JsonObjectStoreTest : ObjectStoreTest(JsonStoreSerializer())
class ProtobufObjectStoreTest : ObjectStoreTest(ProtoBufStoreSerializer())

abstract class ObjectStoreTest(
    private val serializer: ObjectStoreSerializer,
    private val secure: Boolean = false
) : RobolectricTestCases() {

    private lateinit var store: ObjectStore

    @BeforeTest
    fun setup() {
        store = ObjectStore(
            storeWriter = getStoreWriter(secure),
            storeSerializer = serializer
        )
    }

    private fun keyForType(type: KType): String {
        return ObjectStore.keyForType(type)
    }

    @Test
    fun testKeyForType() {
        assertEquals(nameFromKtype(typeOf<TestClass>()), keyForType(typeOf<TestClass>()))
        assertEquals(
            "objectstore.core.TestClassWithGeneric<kotlin.Boolean>",
            keyForType(typeOf<TestClassWithGeneric<Boolean>>())
        )
        assertEquals(
            "objectstore.core.TestClassWithGeneric<objectstore.core.TestClass>",
            keyForType(typeOf<TestClassWithGeneric<TestClass>>())
        )
    }

    @Test
    fun testGetOrNullWithoutValue() {
        assertNull(store.getOrNull<TestClass>())
    }

    @Test
    fun testGetWithoutDefaultAndWithoutValue() {
        assertFailsWith<IllegalArgumentException> {
            store.get<TestClass>()
        }
    }

    @Test
    fun testGetWithDefaultAndWithoutValue() {
        val expected = TestClass()
        val actual1 = store.get(expected)
        val actual2 = store.get<TestClass>()
        assertEquals(expected, actual1)
        assertEquals(expected, actual2)
    }

    @Test
    fun testGetFlowWithoutDefaultAndWithoutValueEmitsNull() {
        val subject = store.getFlow<TestClass>()
        assertNull(subject.value)
    }

    @Test
    fun testGetFlowWithoutDefaultEmitsSetValue() {
        val subject = store.getFlow<TestClass>()
        val expected = TestClass()

        store.put(expected)
        assertEquals(expected, subject.value)
    }

    @Test
    fun testGetFlowWithDefaultEmitsDefault() {
        val expected = TestClass()
        val subject = store.getFlow(expected)

        assertEquals(expected, subject.value)
    }

    @Test
    fun testSetAndGetWithBasicType() {
        store.put(value = 1, "test")
        assertEquals(1, store.get(key = "test"))
        store.put(value = true, "test2")
        assertEquals(true, store.get(key = "test2"))
        store.put(value = 1L, "test3")
        assertEquals(1L, store.get(key = "test3"))
        store.put(value = 1f, "test4")
        assertEquals(1f, store.get(key = "test4"))
    }

    @Test
    fun testSetAndDelete() {
        store.put(value = 1, "test")
        store.put(value = null, "test")
        assertNull(store.getOrNull(key = "test"))
        store.put(value = true, "test2")
        store.put(value = null, "test2")
        assertNull(store.getOrNull(key = "test2"))
        store.put(value = 1L, "test3")
        store.put(value = null, "test3")
        assertNull(store.getOrNull(key = "test3"))
        store.put(value = 1f, "test4")
        store.put(value = null, "test4")
        assertNull(store.getOrNull(key = "test4"))
    }

    @Test
    fun testBasicTypesFailWithoutKey() {
        assertFailsWith<IllegalArgumentException> { store.get<Boolean>() }
        assertFailsWith<IllegalArgumentException> { store.get<Long>() }
        assertFailsWith<IllegalArgumentException> { store.get<Int>() }
        assertFailsWith<IllegalArgumentException> { store.get<Double>() }
        assertFailsWith<IllegalArgumentException> { store.get<Float>() }
        assertFailsWith<IllegalArgumentException> { store.get<String>() }
        assertFailsWith<IllegalArgumentException> { store.get<Char>() }
        assertFailsWith<IllegalArgumentException> { store.get<Array<String>>() }
        assertFailsWith<IllegalArgumentException> { store.get<UInt>() }
        assertFailsWith<IllegalArgumentException> { store.get<ULong>() }
        assertFailsWith<IllegalArgumentException> { store.get<UByte>() }
        assertFailsWith<IllegalArgumentException> { store.get<UShort>() }
    }
}
