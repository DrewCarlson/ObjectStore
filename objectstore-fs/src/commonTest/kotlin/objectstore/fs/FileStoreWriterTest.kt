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
package objectstore.fs

import objectstore.core.ObjectStore
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.*

class FileStoreWriterTest {

    private lateinit var store: ObjectStore
    private lateinit var fs: FileSystem
    private val basePath = "/test".toPath()

    @BeforeTest
    fun setup() {
        fs = FakeFileSystem()
        store = ObjectStore(
            storeWriter = FileStoreWriter(basePath.toString(), fs)
        )
    }

    @Test
    fun testPut() {
        store.put("test-value", "test-key")
        val path = basePath.resolve("test-key".encodeUtf8().sha1().hex())
        assertTrue(fs.exists(path))
        assertEquals("test-value", fs.read(path) { readUtf8() })
    }

    @Test
    fun testGetWithValue() {
        val path = basePath.resolve("test-key".encodeUtf8().sha1().hex())
        fs.write(path, true) { writeUtf8("test-value") }

        assertEquals("test-value", store.getOrNull(key = "test-key"))
    }

    @Test
    fun testGetWithoutValue() {
        assertNull(store.getOrNull(key = "test-key"))
    }

    @Test
    fun testRemove() {
        val path = basePath.resolve("test-key".encodeUtf8().sha1().hex())
        fs.write(path) { writeUtf8("test-value") }

        store.remove<String>(key = "test-key")

        assertFalse(fs.exists(path))
    }
}
