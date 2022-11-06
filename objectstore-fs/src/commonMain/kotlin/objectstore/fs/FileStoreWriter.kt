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

import objectstore.core.ObjectStoreWriter
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.reflect.KType

@Suppress("FunctionName")
public fun FileStoreWriter(path: String): ObjectStoreWriter {
    return FileStoreWriter(path, FS)
}

internal class FileStoreWriter(
    path: String,
    private val fs: FileSystem
) : ObjectStoreWriter {

    private val path: Path = path.toPath()

    init {
        if (fs.exists(this.path)) {
            val metadata = fs.metadataOrNull(this.path)
            require(metadata?.isDirectory == true) {
                "FileStoreWriter path must be a directory: $path"
            }
        } else {
            fs.createDirectories(this.path, true)
        }
    }

    override fun canStoreType(type: KType): Boolean {
        return type.classifier == String::class
    }

    override fun keys(): Set<String> {
        return fs.listOrNull(path)
            .orEmpty()
            .mapNotNull { path ->
                try {
                    path.name.decodeHex().utf8()
                } catch (e: IllegalArgumentException) {
                    null // Unknown file
                }
            }.toSet()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: KType, key: String): T? {
        if (type.classifier != String::class) unhandledType(type)
        val keyHash = key.encodeUtf8().hex()
        val objectPath = path.resolve(keyHash)
        return if (fs.exists(objectPath)) {
            fs.read(objectPath) { readUtf8() } as T?
        } else {
            null
        }
    }

    override fun <T : Any> put(type: KType, key: String, value: T?) {
        if (type.classifier != String::class) unhandledType(type)
        val keyHash = key.encodeUtf8().hex()
        val objectPath = path.resolve(keyHash)
        if (value == null) {
            fs.delete(objectPath)
        } else {
            fs.write(objectPath) { writeUtf8(value as String) }
        }
    }

    override fun clear() {
        fs.listOrNull(path)
            .orEmpty()
            .forEach { path ->
                try {
                    path.name.decodeHex()
                    fs.delete(path)
                } catch (e: IllegalArgumentException) {
                    // Unknown file
                }
            }
    }
}
