/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
@file:Suppress("NAME_SHADOWING")

package uniffi.rustast

// Common helper code.
//
// Ideally this would live in a separate .kt file where it can be unittested etc
// in isolation, and perhaps even published as a re-useable package.
//
// However, it's important that the details of how this helper code works (e.g. the
// way that different builtin types are passed across the FFI) exactly match what's
// expected by the Rust code on the other side of the interface. In practice right
// now that means coming from the exact some version of `uniffi` that was used to
// compile the Rust component. The easiest way to ensure this is to bundle the Kotlin
// helpers directly inline like we're doing here.

import com.sun.jna.Callback
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// This is a helper for safely working with byte buffers returned from the Rust code.
// A rust-owned buffer is represented by its capacity, its current length, and a
// pointer to the underlying data.

/** @suppress */
@Structure.FieldOrder("capacity", "len", "data")
open class RustBuffer : Structure() {
    // Note: `capacity` and `len` are actually `ULong` values, but JVM only supports signed values.
    // When dealing with these fields, make sure to call `toULong()`.
    @JvmField var capacity: Long = 0
    @JvmField var len: Long = 0
    @JvmField var data: Pointer? = null

    class ByValue : RustBuffer(), Structure.ByValue

    class ByReference : RustBuffer(), Structure.ByReference

    internal fun setValue(other: RustBuffer) {
        capacity = other.capacity
        len = other.len
        data = other.data
    }

    companion object {
        internal fun alloc(size: ULong = 0UL) =
            uniffiRustCall() { status ->
                    // Note: need to convert the size to a `Long` value to make this work with JVM.
                    UniffiLib.ffi_rustast_rustbuffer_alloc(size.toLong(), status)
                }
                .also {
                    if (it.data == null) {
                        throw RuntimeException(
                            "RustBuffer.alloc() returned null data pointer (size=${size})"
                        )
                    }
                }

        internal fun create(capacity: ULong, len: ULong, data: Pointer?): RustBuffer.ByValue {
            var buf = RustBuffer.ByValue()
            buf.capacity = capacity.toLong()
            buf.len = len.toLong()
            buf.data = data
            return buf
        }

        internal fun free(buf: RustBuffer.ByValue) =
            uniffiRustCall() { status -> UniffiLib.ffi_rustast_rustbuffer_free(buf, status) }
    }

    @Suppress("TooGenericExceptionThrown")
    fun asByteBuffer() =
        this.data?.getByteBuffer(0, this.len.toLong())?.also { it.order(ByteOrder.BIG_ENDIAN) }
}

// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

@Structure.FieldOrder("len", "data")
internal open class ForeignBytes : Structure() {
    @JvmField var len: Int = 0
    @JvmField var data: Pointer? = null

    class ByValue : ForeignBytes(), Structure.ByValue
}

/**
 * The FfiConverter interface handles converter types to and from the FFI
 *
 * All implementing objects should be public to support external types. When a type is external we
 * need to import it's FfiConverter.
 *
 * @suppress
 */
public interface FfiConverter<KotlinType, FfiType> {
    // Convert an FFI type to a Kotlin type
    fun lift(value: FfiType): KotlinType

    // Convert an Kotlin type to an FFI type
    fun lower(value: KotlinType): FfiType

    // Read a Kotlin type from a `ByteBuffer`
    fun read(buf: ByteBuffer): KotlinType

    // Calculate bytes to allocate when creating a `RustBuffer`
    //
    // This must return at least as many bytes as the write() function will
    // write. It can return more bytes than needed, for example when writing
    // Strings we can't know the exact bytes needed until we the UTF-8
    // encoding, so we pessimistically allocate the largest size possible (3
    // bytes per codepoint).  Allocating extra bytes is not really a big deal
    // because the `RustBuffer` is short-lived.
    fun allocationSize(value: KotlinType): ULong

    // Write a Kotlin type to a `ByteBuffer`
    fun write(value: KotlinType, buf: ByteBuffer)

    // Lower a value into a `RustBuffer`
    //
    // This method lowers a value into a `RustBuffer` rather than the normal
    // FfiType.  It's used by the callback interface code.  Callback interface
    // returns are always serialized into a `RustBuffer` regardless of their
    // normal FFI type.
    fun lowerIntoRustBuffer(value: KotlinType): RustBuffer.ByValue {
        val rbuf = RustBuffer.alloc(allocationSize(value))
        try {
            val bbuf =
                rbuf.data!!.getByteBuffer(0, rbuf.capacity).also { it.order(ByteOrder.BIG_ENDIAN) }
            write(value, bbuf)
            rbuf.writeField("len", bbuf.position().toLong())
            return rbuf
        } catch (e: Throwable) {
            RustBuffer.free(rbuf)
            throw e
        }
    }

    // Lift a value from a `RustBuffer`.
    //
    // This here mostly because of the symmetry with `lowerIntoRustBuffer()`.
    // It's currently only used by the `FfiConverterRustBuffer` class below.
    fun liftFromRustBuffer(rbuf: RustBuffer.ByValue): KotlinType {
        val byteBuf = rbuf.asByteBuffer()!!
        try {
            val item = read(byteBuf)
            if (byteBuf.hasRemaining()) {
                throw RuntimeException(
                    "junk remaining in buffer after lifting, something is very wrong!!"
                )
            }
            return item
        } finally {
            RustBuffer.free(rbuf)
        }
    }
}

/**
 * FfiConverter that uses `RustBuffer` as the FfiType
 *
 * @suppress
 */
public interface FfiConverterRustBuffer<KotlinType> : FfiConverter<KotlinType, RustBuffer.ByValue> {
    override fun lift(value: RustBuffer.ByValue) = liftFromRustBuffer(value)

    override fun lower(value: KotlinType) = lowerIntoRustBuffer(value)
}

// A handful of classes and functions to support the generated data structures.
// This would be a good candidate for isolating in its own ffi-support lib.

internal const val UNIFFI_CALL_SUCCESS = 0.toByte()
internal const val UNIFFI_CALL_ERROR = 1.toByte()
internal const val UNIFFI_CALL_UNEXPECTED_ERROR = 2.toByte()

@Structure.FieldOrder("code", "error_buf")
internal open class UniffiRustCallStatus : Structure() {
    @JvmField var code: Byte = 0
    @JvmField var error_buf: RustBuffer.ByValue = RustBuffer.ByValue()

    class ByValue : UniffiRustCallStatus(), Structure.ByValue

    fun isSuccess(): Boolean {
        return code == UNIFFI_CALL_SUCCESS
    }

    fun isError(): Boolean {
        return code == UNIFFI_CALL_ERROR
    }

    fun isPanic(): Boolean {
        return code == UNIFFI_CALL_UNEXPECTED_ERROR
    }

    companion object {
        fun create(code: Byte, errorBuf: RustBuffer.ByValue): UniffiRustCallStatus.ByValue {
            val callStatus = UniffiRustCallStatus.ByValue()
            callStatus.code = code
            callStatus.error_buf = errorBuf
            return callStatus
        }
    }
}

class InternalException(message: String) : kotlin.Exception(message)

/**
 * Each top-level error class has a companion object that can lift the error from the call status's
 * rust buffer
 *
 * @suppress
 */
interface UniffiRustCallStatusErrorHandler<E> {
    fun lift(error_buf: RustBuffer.ByValue): E
}

// Helpers for calling Rust
// In practice we usually need to be synchronized to call this safely, so it doesn't
// synchronize itself

// Call a rust function that returns a Result<>.  Pass in the Error class companion that corresponds
// to the Err
private inline fun <U, E : kotlin.Exception> uniffiRustCallWithError(
    errorHandler: UniffiRustCallStatusErrorHandler<E>,
    callback: (UniffiRustCallStatus) -> U,
): U {
    var status = UniffiRustCallStatus()
    val return_value = callback(status)
    uniffiCheckCallStatus(errorHandler, status)
    return return_value
}

// Check UniffiRustCallStatus and throw an error if the call wasn't successful
private fun <E : kotlin.Exception> uniffiCheckCallStatus(
    errorHandler: UniffiRustCallStatusErrorHandler<E>,
    status: UniffiRustCallStatus,
) {
    if (status.isSuccess()) {
        return
    } else if (status.isError()) {
        throw errorHandler.lift(status.error_buf)
    } else if (status.isPanic()) {
        // when the rust code sees a panic, it tries to construct a rustbuffer
        // with the message.  but if that code panics, then it just sends back
        // an empty buffer.
        if (status.error_buf.len > 0) {
            throw InternalException(FfiConverterString.lift(status.error_buf))
        } else {
            throw InternalException("Rust panic")
        }
    } else {
        throw InternalException("Unknown rust call status: $status.code")
    }
}

/**
 * UniffiRustCallStatusErrorHandler implementation for times when we don't expect a CALL_ERROR
 *
 * @suppress
 */
object UniffiNullRustCallStatusErrorHandler : UniffiRustCallStatusErrorHandler<InternalException> {
    override fun lift(error_buf: RustBuffer.ByValue): InternalException {
        RustBuffer.free(error_buf)
        return InternalException("Unexpected CALL_ERROR")
    }
}

// Call a rust function that returns a plain value
private inline fun <U> uniffiRustCall(callback: (UniffiRustCallStatus) -> U): U {
    return uniffiRustCallWithError(UniffiNullRustCallStatusErrorHandler, callback)
}

internal inline fun <T> uniffiTraitInterfaceCall(
    callStatus: UniffiRustCallStatus,
    makeCall: () -> T,
    writeReturn: (T) -> Unit,
) {
    try {
        writeReturn(makeCall())
    } catch (e: kotlin.Exception) {
        callStatus.code = UNIFFI_CALL_UNEXPECTED_ERROR
        callStatus.error_buf = FfiConverterString.lower(e.toString())
    }
}

internal inline fun <T, reified E : Throwable> uniffiTraitInterfaceCallWithError(
    callStatus: UniffiRustCallStatus,
    makeCall: () -> T,
    writeReturn: (T) -> Unit,
    lowerError: (E) -> RustBuffer.ByValue,
) {
    try {
        writeReturn(makeCall())
    } catch (e: kotlin.Exception) {
        if (e is E) {
            callStatus.code = UNIFFI_CALL_ERROR
            callStatus.error_buf = lowerError(e)
        } else {
            callStatus.code = UNIFFI_CALL_UNEXPECTED_ERROR
            callStatus.error_buf = FfiConverterString.lower(e.toString())
        }
    }
}

// Initial value and increment amount for handles.
// These ensure that Kotlin-generated handles always have the lowest bit set
private const val UNIFFI_HANDLEMAP_INITIAL = 1.toLong()
private const val UNIFFI_HANDLEMAP_DELTA = 2.toLong()

// Map handles to objects
//
// This is used pass an opaque 64-bit handle representing a foreign object to the Rust code.
internal class UniffiHandleMap<T : Any> {
    private val map = ConcurrentHashMap<Long, T>()
    // Start
    private val counter = java.util.concurrent.atomic.AtomicLong(UNIFFI_HANDLEMAP_INITIAL)

    val size: Int
        get() = map.size

    // Insert a new object into the handle map and get a handle for it
    fun insert(obj: T): Long {
        val handle = counter.getAndAdd(UNIFFI_HANDLEMAP_DELTA)
        map.put(handle, obj)
        return handle
    }

    // Clone a handle, creating a new one
    fun clone(handle: Long): Long {
        val obj =
            map.get(handle) ?: throw InternalException("UniffiHandleMap.clone: Invalid handle")
        return insert(obj)
    }

    // Get an object from the handle map
    fun get(handle: Long): T {
        return map.get(handle) ?: throw InternalException("UniffiHandleMap.get: Invalid handle")
    }

    // Remove an entry from the handlemap and get the Kotlin object back
    fun remove(handle: Long): T {
        return map.remove(handle) ?: throw InternalException("UniffiHandleMap: Invalid handle")
    }
}

// Contains loading, initialization code,
// and the FFI Function declarations in a com.sun.jna.Library.
@Synchronized
private fun findLibraryName(componentName: String): String {
    val libOverride = System.getProperty("uniffi.component.$componentName.libraryOverride")
    if (libOverride != null) {
        return libOverride
    }
    return "rustast"
}

// Define FFI callback types
internal interface UniffiRustFutureContinuationCallback : com.sun.jna.Callback {
    fun callback(`data`: Long, `pollResult`: Byte)
}

internal interface UniffiForeignFutureDroppedCallback : com.sun.jna.Callback {
    fun callback(`handle`: Long)
}

internal interface UniffiCallbackInterfaceFree : com.sun.jna.Callback {
    fun callback(`handle`: Long)
}

internal interface UniffiCallbackInterfaceClone : com.sun.jna.Callback {
    fun callback(`handle`: Long): Long
}

@Structure.FieldOrder("handle", "free")
internal open class UniffiForeignFutureDroppedCallbackStruct(
    @JvmField internal var `handle`: Long = 0.toLong(),
    @JvmField internal var `free`: UniffiForeignFutureDroppedCallback? = null,
) : Structure() {
    class UniffiByValue(
        `handle`: Long = 0.toLong(),
        `free`: UniffiForeignFutureDroppedCallback? = null,
    ) : UniffiForeignFutureDroppedCallbackStruct(`handle`, `free`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureDroppedCallbackStruct) {
        `handle` = other.`handle`
        `free` = other.`free`
    }
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultU8(
    @JvmField internal var `returnValue`: Byte = 0.toByte(),
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Byte = 0.toByte(),
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultU8(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultU8) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteU8 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultU8.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultI8(
    @JvmField internal var `returnValue`: Byte = 0.toByte(),
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Byte = 0.toByte(),
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultI8(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultI8) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteI8 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultI8.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultU16(
    @JvmField internal var `returnValue`: Short = 0.toShort(),
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Short = 0.toShort(),
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultU16(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultU16) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteU16 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultU16.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultI16(
    @JvmField internal var `returnValue`: Short = 0.toShort(),
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Short = 0.toShort(),
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultI16(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultI16) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteI16 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultI16.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultU32(
    @JvmField internal var `returnValue`: Int = 0,
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Int = 0,
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultU32(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultU32) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteU32 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultU32.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultI32(
    @JvmField internal var `returnValue`: Int = 0,
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Int = 0,
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultI32(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultI32) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteI32 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultI32.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultU64(
    @JvmField internal var `returnValue`: Long = 0.toLong(),
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Long = 0.toLong(),
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultU64(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultU64) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteU64 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultU64.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultI64(
    @JvmField internal var `returnValue`: Long = 0.toLong(),
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Long = 0.toLong(),
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultI64(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultI64) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteI64 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultI64.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultF32(
    @JvmField internal var `returnValue`: Float = 0.0f,
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Float = 0.0f,
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultF32(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultF32) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteF32 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultF32.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultF64(
    @JvmField internal var `returnValue`: Double = 0.0,
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: Double = 0.0,
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultF64(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultF64) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteF64 : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultF64.UniffiByValue)
}

@Structure.FieldOrder("returnValue", "callStatus")
internal open class UniffiForeignFutureResultRustBuffer(
    @JvmField internal var `returnValue`: RustBuffer.ByValue = RustBuffer.ByValue(),
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
) : Structure() {
    class UniffiByValue(
        `returnValue`: RustBuffer.ByValue = RustBuffer.ByValue(),
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue(),
    ) : UniffiForeignFutureResultRustBuffer(`returnValue`, `callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultRustBuffer) {
        `returnValue` = other.`returnValue`
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteRustBuffer : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultRustBuffer.UniffiByValue)
}

@Structure.FieldOrder("callStatus")
internal open class UniffiForeignFutureResultVoid(
    @JvmField
    internal var `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue()
) : Structure() {
    class UniffiByValue(
        `callStatus`: UniffiRustCallStatus.ByValue = UniffiRustCallStatus.ByValue()
    ) : UniffiForeignFutureResultVoid(`callStatus`), Structure.ByValue

    internal fun uniffiSetValue(other: UniffiForeignFutureResultVoid) {
        `callStatus` = other.`callStatus`
    }
}

internal interface UniffiForeignFutureCompleteVoid : com.sun.jna.Callback {
    fun callback(`callbackData`: Long, `result`: UniffiForeignFutureResultVoid.UniffiByValue)
}

// A JNA Library to expose the extern-C FFI definitions.
// This is an implementation detail which will be called internally by the public API.

// For large crates we prevent `MethodTooLargeException` (see #2340)
// N.B. the name of the extension is very misleading, since it is
// rather `InterfaceTooLargeException`, caused by too many methods
// in the interface for large crates.
//
// By splitting the otherwise huge interface into two parts
// * UniffiLib (this)
// * IntegrityCheckingUniffiLib
// And all checksum methods are put into `IntegrityCheckingUniffiLib`
// we allow for ~2x as many methods in the UniffiLib interface.
//
// Note: above all written when we used JNA's `loadIndirect` etc.
// We now use JNA's "direct mapping" - unclear if same considerations apply exactly.
internal object IntegrityCheckingUniffiLib {
    init {
        Native.register(
            IntegrityCheckingUniffiLib::class.java,
            findLibraryName(componentName = "rustast"),
        )
        uniffiCheckContractApiVersion(this)
        uniffiCheckApiChecksums(this)
    }

    external fun uniffi_rustast_checksum_func_parse_rust_code(): Short

    external fun ffi_rustast_uniffi_contract_version(): Int
}

internal object UniffiLib {

    init {
        Native.register(UniffiLib::class.java, findLibraryName(componentName = "rustast"))
    }

    external fun uniffi_rustast_fn_func_parse_rust_code(
        `source`: RustBuffer.ByValue,
        uniffi_out_err: UniffiRustCallStatus,
    ): RustBuffer.ByValue

    external fun ffi_rustast_rustbuffer_alloc(
        `size`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): RustBuffer.ByValue

    external fun ffi_rustast_rustbuffer_from_bytes(
        `bytes`: ForeignBytes.ByValue,
        uniffi_out_err: UniffiRustCallStatus,
    ): RustBuffer.ByValue

    external fun ffi_rustast_rustbuffer_free(
        `buf`: RustBuffer.ByValue,
        uniffi_out_err: UniffiRustCallStatus,
    ): Unit

    external fun ffi_rustast_rustbuffer_reserve(
        `buf`: RustBuffer.ByValue,
        `additional`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): RustBuffer.ByValue

    external fun ffi_rustast_rust_future_poll_u8(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_u8(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_u8(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_u8(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Byte

    external fun ffi_rustast_rust_future_poll_i8(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_i8(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_i8(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_i8(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Byte

    external fun ffi_rustast_rust_future_poll_u16(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_u16(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_u16(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_u16(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Short

    external fun ffi_rustast_rust_future_poll_i16(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_i16(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_i16(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_i16(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Short

    external fun ffi_rustast_rust_future_poll_u32(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_u32(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_u32(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_u32(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Int

    external fun ffi_rustast_rust_future_poll_i32(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_i32(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_i32(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_i32(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Int

    external fun ffi_rustast_rust_future_poll_u64(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_u64(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_u64(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_u64(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Long

    external fun ffi_rustast_rust_future_poll_i64(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_i64(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_i64(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_i64(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Long

    external fun ffi_rustast_rust_future_poll_f32(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_f32(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_f32(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_f32(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Float

    external fun ffi_rustast_rust_future_poll_f64(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_f64(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_f64(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_f64(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Double

    external fun ffi_rustast_rust_future_poll_rust_buffer(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_rust_buffer(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_rust_buffer(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_rust_buffer(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): RustBuffer.ByValue

    external fun ffi_rustast_rust_future_poll_void(
        `handle`: Long,
        `callback`: UniffiRustFutureContinuationCallback,
        `callbackData`: Long,
    ): Unit

    external fun ffi_rustast_rust_future_cancel_void(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_free_void(`handle`: Long): Unit

    external fun ffi_rustast_rust_future_complete_void(
        `handle`: Long,
        uniffi_out_err: UniffiRustCallStatus,
    ): Unit
}

private fun uniffiCheckContractApiVersion(lib: IntegrityCheckingUniffiLib) {
    // Get the bindings contract version from our ComponentInterface
    val bindings_contract_version = 30
    // Get the scaffolding contract version by calling the into the dylib
    val scaffolding_contract_version = lib.ffi_rustast_uniffi_contract_version()
    if (bindings_contract_version != scaffolding_contract_version) {
        throw RuntimeException(
            "UniFFI contract version mismatch: try cleaning and rebuilding your project"
        )
    }
}

@Suppress("UNUSED_PARAMETER")
private fun uniffiCheckApiChecksums(lib: IntegrityCheckingUniffiLib) {
    if (lib.uniffi_rustast_checksum_func_parse_rust_code() != 45886.toShort()) {
        throw RuntimeException(
            "UniFFI API checksum mismatch: try cleaning and rebuilding your project"
        )
    }
}

/** @suppress */
public fun uniffiEnsureInitialized() {
    IntegrityCheckingUniffiLib
    // UniffiLib() initialized as objects are used, but we still need to explicitly
    // reference it so initialization across crates works as expected.
    UniffiLib
}

// Async support

// Public interface members begin here.

// Interface implemented by anything that can contain an object reference.
//
// Such types expose a `destroy()` method that must be called to cleanly
// dispose of the contained objects. Failure to call this method may result
// in memory leaks.
//
// The easiest way to ensure this method is called is to use the `.use`
// helper method to execute a block and destroy the object at the end.
interface Disposable {
    fun destroy()

    companion object {
        fun destroy(vararg args: Any?) {
            for (arg in args) {
                when (arg) {
                    is Disposable -> arg.destroy()
                    is ArrayList<*> -> {
                        for (idx in arg.indices) {
                            val element = arg[idx]
                            if (element is Disposable) {
                                element.destroy()
                            }
                        }
                    }
                    is Map<*, *> -> {
                        for (element in arg.values) {
                            if (element is Disposable) {
                                element.destroy()
                            }
                        }
                    }
                    is Iterable<*> -> {
                        for (element in arg) {
                            if (element is Disposable) {
                                element.destroy()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** @suppress */
inline fun <T : Disposable?, R> T.use(block: (T) -> R) =
    try {
        block(this)
    } finally {
        try {
            // N.B. our implementation is on the nullable type `Disposable?`.
            this?.destroy()
        } catch (e: Throwable) {
            // swallow
        }
    }

/**
 * Placeholder object used to signal that we're constructing an interface with a FFI handle.
 *
 * This is the first argument for interface constructors that input a raw handle. It exists is that
 * so we can avoid signature conflicts when an interface has a regular constructor than inputs a
 * Long.
 *
 * @suppress
 */
object UniffiWithHandle

/**
 * Used to instantiate an interface without an actual pointer, for fakes in tests, mostly.
 *
 * @suppress
 */
object NoHandle

/** @suppress */
public object FfiConverterUInt : FfiConverter<UInt, Int> {
    override fun lift(value: Int): UInt {
        return value.toUInt()
    }

    override fun read(buf: ByteBuffer): UInt {
        return lift(buf.getInt())
    }

    override fun lower(value: UInt): Int {
        return value.toInt()
    }

    override fun allocationSize(value: UInt) = 4UL

    override fun write(value: UInt, buf: ByteBuffer) {
        buf.putInt(value.toInt())
    }
}

/** @suppress */
public object FfiConverterBoolean : FfiConverter<Boolean, Byte> {
    override fun lift(value: Byte): Boolean {
        return value.toInt() != 0
    }

    override fun read(buf: ByteBuffer): Boolean {
        return lift(buf.get())
    }

    override fun lower(value: Boolean): Byte {
        return if (value) 1.toByte() else 0.toByte()
    }

    override fun allocationSize(value: Boolean) = 1UL

    override fun write(value: Boolean, buf: ByteBuffer) {
        buf.put(lower(value))
    }
}

/** @suppress */
public object FfiConverterString : FfiConverter<String, RustBuffer.ByValue> {
    // Note: we don't inherit from FfiConverterRustBuffer, because we use a
    // special encoding when lowering/lifting.  We can use `RustBuffer.len` to
    // store our length and avoid writing it out to the buffer.
    override fun lift(value: RustBuffer.ByValue): String {
        try {
            val byteArr = ByteArray(value.len.toInt())
            value.asByteBuffer()!!.get(byteArr)
            return byteArr.toString(Charsets.UTF_8)
        } finally {
            RustBuffer.free(value)
        }
    }

    override fun read(buf: ByteBuffer): String {
        val len = buf.getInt()
        val byteArr = ByteArray(len)
        buf.get(byteArr)
        return byteArr.toString(Charsets.UTF_8)
    }

    fun toUtf8(value: String): ByteBuffer {
        // Make sure we don't have invalid UTF-16, check for lone surrogates.
        return Charsets.UTF_8.newEncoder().run {
            onMalformedInput(CodingErrorAction.REPORT)
            encode(CharBuffer.wrap(value))
        }
    }

    override fun lower(value: String): RustBuffer.ByValue {
        val byteBuf = toUtf8(value)
        // Ideally we'd pass these bytes to `ffi_bytebuffer_from_bytes`, but doing so would require
        // us
        // to copy them into a JNA `Memory`. So we might as well directly copy them into a
        // `RustBuffer`.
        val rbuf = RustBuffer.alloc(byteBuf.limit().toULong())
        rbuf.asByteBuffer()!!.put(byteBuf)
        return rbuf
    }

    // We aren't sure exactly how many bytes our string will be once it's UTF-8
    // encoded.  Allocate 3 bytes per UTF-16 code unit which will always be
    // enough.
    override fun allocationSize(value: String): ULong {
        val sizeForLength = 4UL
        val sizeForString = value.length.toULong() * 3UL
        return sizeForLength + sizeForString
    }

    override fun write(value: String, buf: ByteBuffer) {
        val byteBuf = toUtf8(value)
        buf.putInt(byteBuf.limit())
        buf.put(byteBuf)
    }
}

data class RsAbi(
    var `astNode`: RsNode,
    var `hasExtern`: kotlin.Boolean,
    var `stringLiteral`: kotlin.String,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAbi : FfiConverterRustBuffer<RsAbi> {
    override fun read(buf: ByteBuffer): RsAbi {
        return RsAbi(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterString.read(buf),
        )
    }

    override fun allocationSize(value: RsAbi) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterBoolean.allocationSize(value.`hasExtern`) +
            FfiConverterString.allocationSize(value.`stringLiteral`))

    override fun write(value: RsAbi, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterBoolean.write(value.`hasExtern`, buf)
        FfiConverterString.write(value.`stringLiteral`, buf)
    }
}

data class RsArrayExpr(
    var `astNode`: RsNode,
    var `expressions`: List<RsExpr>,
    var `repeating`: kotlin.Boolean,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSArrayExpr : FfiConverterRustBuffer<RsArrayExpr> {
    override fun read(buf: ByteBuffer): RsArrayExpr {
        return RsArrayExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterBoolean.read(buf),
        )
    }

    override fun allocationSize(value: RsArrayExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expressions`) +
            FfiConverterBoolean.allocationSize(value.`repeating`))

    override fun write(value: RsArrayExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expressions`, buf)
        FfiConverterBoolean.write(value.`repeating`, buf)
    }
}

data class RsArrayType(var `astNode`: RsNode, var `ty`: List<RsType>, var `constArg`: RsConstArg?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSArrayType : FfiConverterRustBuffer<RsArrayType> {
    override fun read(buf: ByteBuffer): RsArrayType {
        return RsArrayType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
            FfiConverterOptionalTypeRSConstArg.read(buf),
        )
    }

    override fun allocationSize(value: RsArrayType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`ty`) +
            FfiConverterOptionalTypeRSConstArg.allocationSize(value.`constArg`))

    override fun write(value: RsArrayType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSType.write(value.`ty`, buf)
        FfiConverterOptionalTypeRSConstArg.write(value.`constArg`, buf)
    }
}

data class RsAsmClobberAbi(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmClobberAbi : FfiConverterRustBuffer<RsAsmClobberAbi> {
    override fun read(buf: ByteBuffer): RsAsmClobberAbi {
        return RsAsmClobberAbi(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsAsmClobberAbi) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsAsmClobberAbi, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsAsmConst(var `astNode`: RsNode, var `expr`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmConst : FfiConverterRustBuffer<RsAsmConst> {
    override fun read(buf: ByteBuffer): RsAsmConst {
        return RsAsmConst(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsAsmConst) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsAsmConst, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsAsmExpr(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmExpr : FfiConverterRustBuffer<RsAsmExpr> {
    override fun read(buf: ByteBuffer): RsAsmExpr {
        return RsAsmExpr(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsAsmExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsAsmExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsAsmLabel(var `astNode`: RsNode, var `blockExpr`: RsBlockExpr?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmLabel : FfiConverterRustBuffer<RsAsmLabel> {
    override fun read(buf: ByteBuffer): RsAsmLabel {
        return RsAsmLabel(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSBlockExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsAsmLabel) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSBlockExpr.allocationSize(value.`blockExpr`))

    override fun write(value: RsAsmLabel, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSBlockExpr.write(value.`blockExpr`, buf)
    }
}

data class RsAsmOperandNamed(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmOperandNamed : FfiConverterRustBuffer<RsAsmOperandNamed> {
    override fun read(buf: ByteBuffer): RsAsmOperandNamed {
        return RsAsmOperandNamed(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsAsmOperandNamed) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsAsmOperandNamed, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsAsmOptions(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmOptions : FfiConverterRustBuffer<RsAsmOptions> {
    override fun read(buf: ByteBuffer): RsAsmOptions {
        return RsAsmOptions(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsAsmOptions) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsAsmOptions, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsAsmRegOperand(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmRegOperand : FfiConverterRustBuffer<RsAsmRegOperand> {
    override fun read(buf: ByteBuffer): RsAsmRegOperand {
        return RsAsmRegOperand(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsAsmRegOperand) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsAsmRegOperand, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsAsmSym(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmSym : FfiConverterRustBuffer<RsAsmSym> {
    override fun read(buf: ByteBuffer): RsAsmSym {
        return RsAsmSym(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsAsmSym) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsAsmSym, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsAssocTypeArg(
    var `astNode`: RsNode,
    var `constArg`: RsConstArg?,
    var `name`: RsNameRef?,
    var `paramList`: RsParamList?,
    var `retType`: RsType?,
    var `returnTypeSyntax`: RsReturnTypeSyntax?,
    var `ty`: RsType?,
    var `typeBounds`: List<RsTypeBound>,
    var `genericArgs`: List<RsGenericArg>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAssocTypeArg : FfiConverterRustBuffer<RsAssocTypeArg> {
    override fun read(buf: ByteBuffer): RsAssocTypeArg {
        return RsAssocTypeArg(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSConstArg.read(buf),
            FfiConverterOptionalTypeRSNameRef.read(buf),
            FfiConverterOptionalTypeRSParamList.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
            FfiConverterOptionalTypeRSReturnTypeSyntax.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
            FfiConverterSequenceTypeRSTypeBound.read(buf),
            FfiConverterSequenceTypeRSGenericArg.read(buf),
        )
    }

    override fun allocationSize(value: RsAssocTypeArg) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSConstArg.allocationSize(value.`constArg`) +
            FfiConverterOptionalTypeRSNameRef.allocationSize(value.`name`) +
            FfiConverterOptionalTypeRSParamList.allocationSize(value.`paramList`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`retType`) +
            FfiConverterOptionalTypeRSReturnTypeSyntax.allocationSize(value.`returnTypeSyntax`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`) +
            FfiConverterSequenceTypeRSTypeBound.allocationSize(value.`typeBounds`) +
            FfiConverterSequenceTypeRSGenericArg.allocationSize(value.`genericArgs`))

    override fun write(value: RsAssocTypeArg, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSConstArg.write(value.`constArg`, buf)
        FfiConverterOptionalTypeRSNameRef.write(value.`name`, buf)
        FfiConverterOptionalTypeRSParamList.write(value.`paramList`, buf)
        FfiConverterOptionalTypeRSType.write(value.`retType`, buf)
        FfiConverterOptionalTypeRSReturnTypeSyntax.write(value.`returnTypeSyntax`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
        FfiConverterSequenceTypeRSTypeBound.write(value.`typeBounds`, buf)
        FfiConverterSequenceTypeRSGenericArg.write(value.`genericArgs`, buf)
    }
}

data class RsAwaitExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAwaitExpr : FfiConverterRustBuffer<RsAwaitExpr> {
    override fun read(buf: ByteBuffer): RsAwaitExpr {
        return RsAwaitExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsAwaitExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsAwaitExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsBecomeExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSBecomeExpr : FfiConverterRustBuffer<RsBecomeExpr> {
    override fun read(buf: ByteBuffer): RsBecomeExpr {
        return RsBecomeExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsBecomeExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsBecomeExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsBinExpr(
    var `astNode`: RsNode,
    var `expressions`: List<RsExpr>,
    var `operator`: kotlin.String,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSBinExpr : FfiConverterRustBuffer<RsBinExpr> {
    override fun read(buf: ByteBuffer): RsBinExpr {
        return RsBinExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterString.read(buf),
        )
    }

    override fun allocationSize(value: RsBinExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expressions`) +
            FfiConverterString.allocationSize(value.`operator`))

    override fun write(value: RsBinExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expressions`, buf)
        FfiConverterString.write(value.`operator`, buf)
    }
}

data class RsBlockExpr(
    var `astNode`: RsNode,
    var `stmts`: List<RsStmt>,
    var `tailExpr`: List<RsExpr>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSBlockExpr : FfiConverterRustBuffer<RsBlockExpr> {
    override fun read(buf: ByteBuffer): RsBlockExpr {
        return RsBlockExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSStmt.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsBlockExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSStmt.allocationSize(value.`stmts`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`tailExpr`))

    override fun write(value: RsBlockExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSStmt.write(value.`stmts`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`tailExpr`, buf)
    }
}

data class RsBoxPat(var `astNode`: RsNode, var `pat`: List<RsPat>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSBoxPat : FfiConverterRustBuffer<RsBoxPat> {
    override fun read(buf: ByteBuffer): RsBoxPat {
        return RsBoxPat(FfiConverterTypeRSNode.read(buf), FfiConverterSequenceTypeRSPat.read(buf))
    }

    override fun allocationSize(value: RsBoxPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`pat`))

    override fun write(value: RsBoxPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`pat`, buf)
    }
}

data class RsBreakExpr(
    var `astNode`: RsNode,
    var `expr`: List<RsExpr>,
    var `lifetime`: RsLifetime?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSBreakExpr : FfiConverterRustBuffer<RsBreakExpr> {
    override fun read(buf: ByteBuffer): RsBreakExpr {
        return RsBreakExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterOptionalTypeRSLifetime.read(buf),
        )
    }

    override fun allocationSize(value: RsBreakExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterOptionalTypeRSLifetime.allocationSize(value.`lifetime`))

    override fun write(value: RsBreakExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterOptionalTypeRSLifetime.write(value.`lifetime`, buf)
    }
}

data class RsCallExpr(
    var `astNode`: RsNode,
    var `expr`: List<RsExpr>,
    var `arguments`: List<RsExpr>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSCallExpr : FfiConverterRustBuffer<RsCallExpr> {
    override fun read(buf: ByteBuffer): RsCallExpr {
        return RsCallExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsCallExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`arguments`))

    override fun write(value: RsCallExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`arguments`, buf)
    }
}

data class RsCastExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>, var `ty`: List<RsType>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSCastExpr : FfiConverterRustBuffer<RsCastExpr> {
    override fun read(buf: ByteBuffer): RsCastExpr {
        return RsCastExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsCastExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsCastExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterSequenceTypeRSType.write(value.`ty`, buf)
    }
}

data class RsClosureExpr(
    var `astNode`: RsNode,
    var `paramList`: List<RsParamList>,
    var `retType`: List<RsType>,
    var `expressions`: List<RsExpr>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSClosureExpr : FfiConverterRustBuffer<RsClosureExpr> {
    override fun read(buf: ByteBuffer): RsClosureExpr {
        return RsClosureExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSParamList.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsClosureExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSParamList.allocationSize(value.`paramList`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`retType`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expressions`))

    override fun write(value: RsClosureExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSParamList.write(value.`paramList`, buf)
        FfiConverterSequenceTypeRSType.write(value.`retType`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expressions`, buf)
    }
}

data class RsConst(
    var `astNode`: RsNode,
    var `name`: kotlin.String?,
    var `ty`: RsType?,
    var `expr`: List<RsExpr>,
    var `genericParams`: List<RsGenericParam>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSConst : FfiConverterRustBuffer<RsConst> {
    override fun read(buf: ByteBuffer): RsConst {
        return RsConst(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
        )
    }

    override fun allocationSize(value: RsConst) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericParams`))

    override fun write(value: RsConst, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericParams`, buf)
    }
}

data class RsConstArg(var `astNode`: RsNode, var `expr`: RsExpr?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSConstArg : FfiConverterRustBuffer<RsConstArg> {
    override fun read(buf: ByteBuffer): RsConstArg {
        return RsConstArg(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsConstArg) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsConstArg, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsConstBlockPat(var `astNode`: RsNode, var `blockExpr`: RsBlockExpr?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSConstBlockPat : FfiConverterRustBuffer<RsConstBlockPat> {
    override fun read(buf: ByteBuffer): RsConstBlockPat {
        return RsConstBlockPat(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSBlockExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsConstBlockPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSBlockExpr.allocationSize(value.`blockExpr`))

    override fun write(value: RsConstBlockPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSBlockExpr.write(value.`blockExpr`, buf)
    }
}

data class RsConstParam(var `astNode`: RsNode, var `defaultVal`: RsConstArg?, var `ty`: RsType?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSConstParam : FfiConverterRustBuffer<RsConstParam> {
    override fun read(buf: ByteBuffer): RsConstParam {
        return RsConstParam(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSConstArg.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsConstParam) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSConstArg.allocationSize(value.`defaultVal`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsConstParam, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSConstArg.write(value.`defaultVal`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
    }
}

data class RsContinueExpr(var `astNode`: RsNode, var `lifetime`: RsLifetime?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSContinueExpr : FfiConverterRustBuffer<RsContinueExpr> {
    override fun read(buf: ByteBuffer): RsContinueExpr {
        return RsContinueExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSLifetime.read(buf),
        )
    }

    override fun allocationSize(value: RsContinueExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSLifetime.allocationSize(value.`lifetime`))

    override fun write(value: RsContinueExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSLifetime.write(value.`lifetime`, buf)
    }
}

data class RsDynTraitType(var `astNode`: RsNode, var `typeBoundList`: List<RsTypeBound>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSDynTraitType : FfiConverterRustBuffer<RsDynTraitType> {
    override fun read(buf: ByteBuffer): RsDynTraitType {
        return RsDynTraitType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSTypeBound.read(buf),
        )
    }

    override fun allocationSize(value: RsDynTraitType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSTypeBound.allocationSize(value.`typeBoundList`))

    override fun write(value: RsDynTraitType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSTypeBound.write(value.`typeBoundList`, buf)
    }
}

data class RsEnum(
    var `astNode`: RsNode,
    var `name`: kotlin.String?,
    var `variants`: List<RsVariant>,
    var `genericParams`: List<RsGenericParam>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSEnum : FfiConverterRustBuffer<RsEnum> {
    override fun read(buf: ByteBuffer): RsEnum {
        return RsEnum(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterSequenceTypeRSVariant.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
        )
    }

    override fun allocationSize(value: RsEnum) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterSequenceTypeRSVariant.allocationSize(value.`variants`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericParams`))

    override fun write(value: RsEnum, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterSequenceTypeRSVariant.write(value.`variants`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericParams`, buf)
    }
}

data class RsExprStmt(var `astNode`: RsNode, var `expr`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSExprStmt : FfiConverterRustBuffer<RsExprStmt> {
    override fun read(buf: ByteBuffer): RsExprStmt {
        return RsExprStmt(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsExprStmt) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsExprStmt, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsExternBlock(
    var `astNode`: RsNode,
    var `externItems`: List<RsExternItem>,
    var `abi`: RsAbi?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSExternBlock : FfiConverterRustBuffer<RsExternBlock> {
    override fun read(buf: ByteBuffer): RsExternBlock {
        return RsExternBlock(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExternItem.read(buf),
            FfiConverterOptionalTypeRSAbi.read(buf),
        )
    }

    override fun allocationSize(value: RsExternBlock) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExternItem.allocationSize(value.`externItems`) +
            FfiConverterOptionalTypeRSAbi.allocationSize(value.`abi`))

    override fun write(value: RsExternBlock, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExternItem.write(value.`externItems`, buf)
        FfiConverterOptionalTypeRSAbi.write(value.`abi`, buf)
    }
}

data class RsExternCrate(
    var `astNode`: RsNode,
    var `nameRef`: RsNameRef?,
    var `rename`: kotlin.String?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSExternCrate : FfiConverterRustBuffer<RsExternCrate> {
    override fun read(buf: ByteBuffer): RsExternCrate {
        return RsExternCrate(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSNameRef.read(buf),
            FfiConverterOptionalString.read(buf),
        )
    }

    override fun allocationSize(value: RsExternCrate) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSNameRef.allocationSize(value.`nameRef`) +
            FfiConverterOptionalString.allocationSize(value.`rename`))

    override fun write(value: RsExternCrate, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSNameRef.write(value.`nameRef`, buf)
        FfiConverterOptionalString.write(value.`rename`, buf)
    }
}

data class RsFieldExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>, var `nameRef`: RsNameRef?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSFieldExpr : FfiConverterRustBuffer<RsFieldExpr> {
    override fun read(buf: ByteBuffer): RsFieldExpr {
        return RsFieldExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterOptionalTypeRSNameRef.read(buf),
        )
    }

    override fun allocationSize(value: RsFieldExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterOptionalTypeRSNameRef.allocationSize(value.`nameRef`))

    override fun write(value: RsFieldExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterOptionalTypeRSNameRef.write(value.`nameRef`, buf)
    }
}

data class RsFn(
    var `astNode`: RsNode,
    var `paramList`: RsParamList?,
    var `retType`: RsType?,
    var `body`: RsBlockExpr?,
    var `name`: kotlin.String?,
    var `genericParams`: List<RsGenericParam>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSFn : FfiConverterRustBuffer<RsFn> {
    override fun read(buf: ByteBuffer): RsFn {
        return RsFn(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSParamList.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
            FfiConverterOptionalTypeRSBlockExpr.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
        )
    }

    override fun allocationSize(value: RsFn) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSParamList.allocationSize(value.`paramList`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`retType`) +
            FfiConverterOptionalTypeRSBlockExpr.allocationSize(value.`body`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericParams`))

    override fun write(value: RsFn, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSParamList.write(value.`paramList`, buf)
        FfiConverterOptionalTypeRSType.write(value.`retType`, buf)
        FfiConverterOptionalTypeRSBlockExpr.write(value.`body`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericParams`, buf)
    }
}

data class RsFnPtrType(
    var `astNode`: RsNode,
    var `paramList`: List<RsParamList>,
    var `retType`: List<RsType>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSFnPtrType : FfiConverterRustBuffer<RsFnPtrType> {
    override fun read(buf: ByteBuffer): RsFnPtrType {
        return RsFnPtrType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSParamList.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsFnPtrType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSParamList.allocationSize(value.`paramList`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`retType`))

    override fun write(value: RsFnPtrType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSParamList.write(value.`paramList`, buf)
        FfiConverterSequenceTypeRSType.write(value.`retType`, buf)
    }
}

data class RsForExpr(var `astNode`: RsNode, var `pat`: RsPat?, var `expressions`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSForExpr : FfiConverterRustBuffer<RsForExpr> {
    override fun read(buf: ByteBuffer): RsForExpr {
        return RsForExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPat.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsForExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPat.allocationSize(value.`pat`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expressions`))

    override fun write(value: RsForExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPat.write(value.`pat`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expressions`, buf)
    }
}

data class RsForType(
    var `astNode`: RsNode,
    var `ty`: List<RsType>,
    var `genericsInFor`: List<RsGenericParam>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSForType : FfiConverterRustBuffer<RsForType> {
    override fun read(buf: ByteBuffer): RsForType {
        return RsForType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
        )
    }

    override fun allocationSize(value: RsForType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`ty`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericsInFor`))

    override fun write(value: RsForType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSType.write(value.`ty`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericsInFor`, buf)
    }
}

data class RsFormatArgsExpr(
    var `astNode`: RsNode,
    var `template`: List<RsExpr>,
    var `hasPound`: kotlin.Boolean,
    var `hasComma`: kotlin.Boolean,
    var `hasBuiltin`: kotlin.Boolean,
    var `hasFormatArgs`: kotlin.Boolean,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSFormatArgsExpr : FfiConverterRustBuffer<RsFormatArgsExpr> {
    override fun read(buf: ByteBuffer): RsFormatArgsExpr {
        return RsFormatArgsExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
        )
    }

    override fun allocationSize(value: RsFormatArgsExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`template`) +
            FfiConverterBoolean.allocationSize(value.`hasPound`) +
            FfiConverterBoolean.allocationSize(value.`hasComma`) +
            FfiConverterBoolean.allocationSize(value.`hasBuiltin`) +
            FfiConverterBoolean.allocationSize(value.`hasFormatArgs`))

    override fun write(value: RsFormatArgsExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`template`, buf)
        FfiConverterBoolean.write(value.`hasPound`, buf)
        FfiConverterBoolean.write(value.`hasComma`, buf)
        FfiConverterBoolean.write(value.`hasBuiltin`, buf)
        FfiConverterBoolean.write(value.`hasFormatArgs`, buf)
    }
}

data class RsIdentPat(var `astNode`: RsNode, var `name`: kotlin.String?, var `pat`: List<RsPat>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSIdentPat : FfiConverterRustBuffer<RsIdentPat> {
    override fun read(buf: ByteBuffer): RsIdentPat {
        return RsIdentPat(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterSequenceTypeRSPat.read(buf),
        )
    }

    override fun allocationSize(value: RsIdentPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`pat`))

    override fun write(value: RsIdentPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`pat`, buf)
    }
}

data class RsIfExpr(var `astNode`: RsNode, var `expressions`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSIfExpr : FfiConverterRustBuffer<RsIfExpr> {
    override fun read(buf: ByteBuffer): RsIfExpr {
        return RsIfExpr(FfiConverterTypeRSNode.read(buf), FfiConverterSequenceTypeRSExpr.read(buf))
    }

    override fun allocationSize(value: RsIfExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expressions`))

    override fun write(value: RsIfExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expressions`, buf)
    }
}

data class RsImpl(
    var `astNode`: RsNode,
    var `items`: List<RsAssocItem>,
    var `pathTypes`: List<RsPathType>,
    var `genericParams`: List<RsGenericParam>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSImpl : FfiConverterRustBuffer<RsImpl> {
    override fun read(buf: ByteBuffer): RsImpl {
        return RsImpl(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSAssocItem.read(buf),
            FfiConverterSequenceTypeRSPathType.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
        )
    }

    override fun allocationSize(value: RsImpl) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSAssocItem.allocationSize(value.`items`) +
            FfiConverterSequenceTypeRSPathType.allocationSize(value.`pathTypes`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericParams`))

    override fun write(value: RsImpl, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSAssocItem.write(value.`items`, buf)
        FfiConverterSequenceTypeRSPathType.write(value.`pathTypes`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericParams`, buf)
    }
}

data class RsImplTraitType(var `astNode`: RsNode, var `typeBoundList`: List<RsTypeBound>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSImplTraitType : FfiConverterRustBuffer<RsImplTraitType> {
    override fun read(buf: ByteBuffer): RsImplTraitType {
        return RsImplTraitType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSTypeBound.read(buf),
        )
    }

    override fun allocationSize(value: RsImplTraitType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSTypeBound.allocationSize(value.`typeBoundList`))

    override fun write(value: RsImplTraitType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSTypeBound.write(value.`typeBoundList`, buf)
    }
}

data class RsIndexExpr(var `astNode`: RsNode, var `expressions`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSIndexExpr : FfiConverterRustBuffer<RsIndexExpr> {
    override fun read(buf: ByteBuffer): RsIndexExpr {
        return RsIndexExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsIndexExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expressions`))

    override fun write(value: RsIndexExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expressions`, buf)
    }
}

data class RsInferType(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSInferType : FfiConverterRustBuffer<RsInferType> {
    override fun read(buf: ByteBuffer): RsInferType {
        return RsInferType(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsInferType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsInferType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsLetExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>, var `pat`: List<RsPat>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSLetExpr : FfiConverterRustBuffer<RsLetExpr> {
    override fun read(buf: ByteBuffer): RsLetExpr {
        return RsLetExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterSequenceTypeRSPat.read(buf),
        )
    }

    override fun allocationSize(value: RsLetExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`pat`))

    override fun write(value: RsLetExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`pat`, buf)
    }
}

data class RsLetStmt(
    var `astNode`: RsNode,
    var `initializer`: RsExpr?,
    var `letElse`: RsBlockExpr?,
    var `pat`: RsPat?,
    var `ty`: RsType?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSLetStmt : FfiConverterRustBuffer<RsLetStmt> {
    override fun read(buf: ByteBuffer): RsLetStmt {
        return RsLetStmt(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSExpr.read(buf),
            FfiConverterOptionalTypeRSBlockExpr.read(buf),
            FfiConverterOptionalTypeRSPat.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsLetStmt) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSExpr.allocationSize(value.`initializer`) +
            FfiConverterOptionalTypeRSBlockExpr.allocationSize(value.`letElse`) +
            FfiConverterOptionalTypeRSPat.allocationSize(value.`pat`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsLetStmt, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSExpr.write(value.`initializer`, buf)
        FfiConverterOptionalTypeRSBlockExpr.write(value.`letElse`, buf)
        FfiConverterOptionalTypeRSPat.write(value.`pat`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
    }
}

data class RsLifetime(var `astNode`: RsNode, var `name`: kotlin.String) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSLifetime : FfiConverterRustBuffer<RsLifetime> {
    override fun read(buf: ByteBuffer): RsLifetime {
        return RsLifetime(FfiConverterTypeRSNode.read(buf), FfiConverterString.read(buf))
    }

    override fun allocationSize(value: RsLifetime) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterString.allocationSize(value.`name`))

    override fun write(value: RsLifetime, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterString.write(value.`name`, buf)
    }
}

data class RsLifetimeArg(var `astNode`: RsNode, var `lifetime`: RsLifetime?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSLifetimeArg : FfiConverterRustBuffer<RsLifetimeArg> {
    override fun read(buf: ByteBuffer): RsLifetimeArg {
        return RsLifetimeArg(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSLifetime.read(buf),
        )
    }

    override fun allocationSize(value: RsLifetimeArg) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSLifetime.allocationSize(value.`lifetime`))

    override fun write(value: RsLifetimeArg, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSLifetime.write(value.`lifetime`, buf)
    }
}

data class RsLifetimeParam(
    var `astNode`: RsNode,
    var `lifetime`: RsLifetime?,
    var `typeBoundList`: List<RsTypeBound>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSLifetimeParam : FfiConverterRustBuffer<RsLifetimeParam> {
    override fun read(buf: ByteBuffer): RsLifetimeParam {
        return RsLifetimeParam(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSLifetime.read(buf),
            FfiConverterSequenceTypeRSTypeBound.read(buf),
        )
    }

    override fun allocationSize(value: RsLifetimeParam) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSLifetime.allocationSize(value.`lifetime`) +
            FfiConverterSequenceTypeRSTypeBound.allocationSize(value.`typeBoundList`))

    override fun write(value: RsLifetimeParam, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSLifetime.write(value.`lifetime`, buf)
        FfiConverterSequenceTypeRSTypeBound.write(value.`typeBoundList`, buf)
    }
}

data class RsLiteral(var `astNode`: RsNode, var `literalType`: RsLiteralType) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSLiteral : FfiConverterRustBuffer<RsLiteral> {
    override fun read(buf: ByteBuffer): RsLiteral {
        return RsLiteral(FfiConverterTypeRSNode.read(buf), FfiConverterTypeRSLiteralType.read(buf))
    }

    override fun allocationSize(value: RsLiteral) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterTypeRSLiteralType.allocationSize(value.`literalType`))

    override fun write(value: RsLiteral, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterTypeRSLiteralType.write(value.`literalType`, buf)
    }
}

data class RsLiteralPat(var `astNode`: RsNode, var `literal`: RsLiteral?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSLiteralPat : FfiConverterRustBuffer<RsLiteralPat> {
    override fun read(buf: ByteBuffer): RsLiteralPat {
        return RsLiteralPat(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSLiteral.read(buf),
        )
    }

    override fun allocationSize(value: RsLiteralPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSLiteral.allocationSize(value.`literal`))

    override fun write(value: RsLiteralPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSLiteral.write(value.`literal`, buf)
    }
}

data class RsLoopExpr(
    var `astNode`: RsNode,
    var `label`: kotlin.String?,
    var `body`: List<RsBlockExpr>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSLoopExpr : FfiConverterRustBuffer<RsLoopExpr> {
    override fun read(buf: ByteBuffer): RsLoopExpr {
        return RsLoopExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterSequenceTypeRSBlockExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsLoopExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`label`) +
            FfiConverterSequenceTypeRSBlockExpr.allocationSize(value.`body`))

    override fun write(value: RsLoopExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`label`, buf)
        FfiConverterSequenceTypeRSBlockExpr.write(value.`body`, buf)
    }
}

data class RsMacroCall(
    var `astNode`: RsNode,
    var `path`: RsPath?,
    var `macroString`: kotlin.String,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSMacroCall : FfiConverterRustBuffer<RsMacroCall> {
    override fun read(buf: ByteBuffer): RsMacroCall {
        return RsMacroCall(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPath.read(buf),
            FfiConverterString.read(buf),
        )
    }

    override fun allocationSize(value: RsMacroCall) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPath.allocationSize(value.`path`) +
            FfiConverterString.allocationSize(value.`macroString`))

    override fun write(value: RsMacroCall, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPath.write(value.`path`, buf)
        FfiConverterString.write(value.`macroString`, buf)
    }
}

data class RsMacroDef(var `astNode`: RsNode, var `name`: kotlin.String?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSMacroDef : FfiConverterRustBuffer<RsMacroDef> {
    override fun read(buf: ByteBuffer): RsMacroDef {
        return RsMacroDef(FfiConverterTypeRSNode.read(buf), FfiConverterOptionalString.read(buf))
    }

    override fun allocationSize(value: RsMacroDef) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`))

    override fun write(value: RsMacroDef, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
    }
}

data class RsMacroExpr(var `astNode`: RsNode, var `macroCall`: RsMacroCall?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSMacroExpr : FfiConverterRustBuffer<RsMacroExpr> {
    override fun read(buf: ByteBuffer): RsMacroExpr {
        return RsMacroExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSMacroCall.read(buf),
        )
    }

    override fun allocationSize(value: RsMacroExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSMacroCall.allocationSize(value.`macroCall`))

    override fun write(value: RsMacroExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSMacroCall.write(value.`macroCall`, buf)
    }
}

data class RsMacroPat(var `astNode`: RsNode, var `macroCall`: RsMacroCall?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSMacroPat : FfiConverterRustBuffer<RsMacroPat> {
    override fun read(buf: ByteBuffer): RsMacroPat {
        return RsMacroPat(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSMacroCall.read(buf),
        )
    }

    override fun allocationSize(value: RsMacroPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSMacroCall.allocationSize(value.`macroCall`))

    override fun write(value: RsMacroPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSMacroCall.write(value.`macroCall`, buf)
    }
}

data class RsMacroRules(var `astNode`: RsNode, var `name`: kotlin.String?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSMacroRules : FfiConverterRustBuffer<RsMacroRules> {
    override fun read(buf: ByteBuffer): RsMacroRules {
        return RsMacroRules(FfiConverterTypeRSNode.read(buf), FfiConverterOptionalString.read(buf))
    }

    override fun allocationSize(value: RsMacroRules) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`))

    override fun write(value: RsMacroRules, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
    }
}

data class RsMacroType(var `astNode`: RsNode, var `macroCall`: RsMacroCall?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSMacroType : FfiConverterRustBuffer<RsMacroType> {
    override fun read(buf: ByteBuffer): RsMacroType {
        return RsMacroType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSMacroCall.read(buf),
        )
    }

    override fun allocationSize(value: RsMacroType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSMacroCall.allocationSize(value.`macroCall`))

    override fun write(value: RsMacroType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSMacroCall.write(value.`macroCall`, buf)
    }
}

data class RsMatchArm(
    var `astNode`: RsNode,
    var `expr`: List<RsExpr>,
    var `pat`: List<RsPat>,
    var `guard`: List<RsExpr>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSMatchArm : FfiConverterRustBuffer<RsMatchArm> {
    override fun read(buf: ByteBuffer): RsMatchArm {
        return RsMatchArm(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterSequenceTypeRSPat.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsMatchArm) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`pat`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`guard`))

    override fun write(value: RsMatchArm, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`pat`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`guard`, buf)
    }
}

data class RsMatchExpr(
    var `astNode`: RsNode,
    var `expr`: List<RsExpr>,
    var `arms`: List<RsMatchArm>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSMatchExpr : FfiConverterRustBuffer<RsMatchExpr> {
    override fun read(buf: ByteBuffer): RsMatchExpr {
        return RsMatchExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterSequenceTypeRSMatchArm.read(buf),
        )
    }

    override fun allocationSize(value: RsMatchExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterSequenceTypeRSMatchArm.allocationSize(value.`arms`))

    override fun write(value: RsMatchExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterSequenceTypeRSMatchArm.write(value.`arms`, buf)
    }
}

data class RsMethodCallExpr(
    var `astNode`: RsNode,
    var `receiver`: List<RsExpr>,
    var `nameRef`: RsNameRef?,
    var `arguments`: List<RsExpr>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSMethodCallExpr : FfiConverterRustBuffer<RsMethodCallExpr> {
    override fun read(buf: ByteBuffer): RsMethodCallExpr {
        return RsMethodCallExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterOptionalTypeRSNameRef.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsMethodCallExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`receiver`) +
            FfiConverterOptionalTypeRSNameRef.allocationSize(value.`nameRef`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`arguments`))

    override fun write(value: RsMethodCallExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`receiver`, buf)
        FfiConverterOptionalTypeRSNameRef.write(value.`nameRef`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`arguments`, buf)
    }
}

data class RsModule(var `astNode`: RsNode, var `name`: kotlin.String?, var `items`: List<RsItem>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSModule : FfiConverterRustBuffer<RsModule> {
    override fun read(buf: ByteBuffer): RsModule {
        return RsModule(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterSequenceTypeRSItem.read(buf),
        )
    }

    override fun allocationSize(value: RsModule) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterSequenceTypeRSItem.allocationSize(value.`items`))

    override fun write(value: RsModule, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterSequenceTypeRSItem.write(value.`items`, buf)
    }
}

data class RsNameRef(
    var `astNode`: RsNode,
    var `text`: kotlin.String,
    var `ident`: kotlin.String?,
    var `intNumberToken`: kotlin.String?,
    var `hasCapSelf`: kotlin.Boolean,
    var `hasCrate`: kotlin.Boolean,
    var `hasSelf`: kotlin.Boolean,
    var `hasSuper`: kotlin.Boolean,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSNameRef : FfiConverterRustBuffer<RsNameRef> {
    override fun read(buf: ByteBuffer): RsNameRef {
        return RsNameRef(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterString.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
        )
    }

    override fun allocationSize(value: RsNameRef) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterString.allocationSize(value.`text`) +
            FfiConverterOptionalString.allocationSize(value.`ident`) +
            FfiConverterOptionalString.allocationSize(value.`intNumberToken`) +
            FfiConverterBoolean.allocationSize(value.`hasCapSelf`) +
            FfiConverterBoolean.allocationSize(value.`hasCrate`) +
            FfiConverterBoolean.allocationSize(value.`hasSelf`) +
            FfiConverterBoolean.allocationSize(value.`hasSuper`))

    override fun write(value: RsNameRef, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterString.write(value.`text`, buf)
        FfiConverterOptionalString.write(value.`ident`, buf)
        FfiConverterOptionalString.write(value.`intNumberToken`, buf)
        FfiConverterBoolean.write(value.`hasCapSelf`, buf)
        FfiConverterBoolean.write(value.`hasCrate`, buf)
        FfiConverterBoolean.write(value.`hasSelf`, buf)
        FfiConverterBoolean.write(value.`hasSuper`, buf)
    }
}

data class RsNeverType(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSNeverType : FfiConverterRustBuffer<RsNeverType> {
    override fun read(buf: ByteBuffer): RsNeverType {
        return RsNeverType(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsNeverType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsNeverType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsNode(
    var `text`: kotlin.String,
    var `startOffset`: kotlin.UInt,
    var `endOffset`: kotlin.UInt,
    var `comments`: kotlin.String?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSNode : FfiConverterRustBuffer<RsNode> {
    override fun read(buf: ByteBuffer): RsNode {
        return RsNode(
            FfiConverterString.read(buf),
            FfiConverterUInt.read(buf),
            FfiConverterUInt.read(buf),
            FfiConverterOptionalString.read(buf),
        )
    }

    override fun allocationSize(value: RsNode) =
        (FfiConverterString.allocationSize(value.`text`) +
            FfiConverterUInt.allocationSize(value.`startOffset`) +
            FfiConverterUInt.allocationSize(value.`endOffset`) +
            FfiConverterOptionalString.allocationSize(value.`comments`))

    override fun write(value: RsNode, buf: ByteBuffer) {
        FfiConverterString.write(value.`text`, buf)
        FfiConverterUInt.write(value.`startOffset`, buf)
        FfiConverterUInt.write(value.`endOffset`, buf)
        FfiConverterOptionalString.write(value.`comments`, buf)
    }
}

data class RsOffsetOfExpr(
    var `astNode`: RsNode,
    var `fields`: List<RsNameRef>,
    var `ty`: List<RsType>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSOffsetOfExpr : FfiConverterRustBuffer<RsOffsetOfExpr> {
    override fun read(buf: ByteBuffer): RsOffsetOfExpr {
        return RsOffsetOfExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSNameRef.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsOffsetOfExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSNameRef.allocationSize(value.`fields`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsOffsetOfExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSNameRef.write(value.`fields`, buf)
        FfiConverterSequenceTypeRSType.write(value.`ty`, buf)
    }
}

data class RsOrPat(var `astNode`: RsNode, var `pats`: List<RsPat>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSOrPat : FfiConverterRustBuffer<RsOrPat> {
    override fun read(buf: ByteBuffer): RsOrPat {
        return RsOrPat(FfiConverterTypeRSNode.read(buf), FfiConverterSequenceTypeRSPat.read(buf))
    }

    override fun allocationSize(value: RsOrPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`pats`))

    override fun write(value: RsOrPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`pats`, buf)
    }
}

data class RsParam(var `astNode`: RsNode, var `pat`: RsPat?, var `ty`: RsType?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSParam : FfiConverterRustBuffer<RsParam> {
    override fun read(buf: ByteBuffer): RsParam {
        return RsParam(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPat.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsParam) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPat.allocationSize(value.`pat`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsParam, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPat.write(value.`pat`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
    }
}

data class RsParamList(
    var `astNode`: RsNode,
    var `params`: List<RsParam>,
    var `selfParam`: RsSelfParam?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSParamList : FfiConverterRustBuffer<RsParamList> {
    override fun read(buf: ByteBuffer): RsParamList {
        return RsParamList(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSParam.read(buf),
            FfiConverterOptionalTypeRSSelfParam.read(buf),
        )
    }

    override fun allocationSize(value: RsParamList) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSParam.allocationSize(value.`params`) +
            FfiConverterOptionalTypeRSSelfParam.allocationSize(value.`selfParam`))

    override fun write(value: RsParamList, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSParam.write(value.`params`, buf)
        FfiConverterOptionalTypeRSSelfParam.write(value.`selfParam`, buf)
    }
}

data class RsParenExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSParenExpr : FfiConverterRustBuffer<RsParenExpr> {
    override fun read(buf: ByteBuffer): RsParenExpr {
        return RsParenExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsParenExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsParenExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsParenPat(var `astNode`: RsNode, var `pat`: List<RsPat>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSParenPat : FfiConverterRustBuffer<RsParenPat> {
    override fun read(buf: ByteBuffer): RsParenPat {
        return RsParenPat(FfiConverterTypeRSNode.read(buf), FfiConverterSequenceTypeRSPat.read(buf))
    }

    override fun allocationSize(value: RsParenPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`pat`))

    override fun write(value: RsParenPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`pat`, buf)
    }
}

data class RsParenType(var `astNode`: RsNode, var `ty`: List<RsType>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSParenType : FfiConverterRustBuffer<RsParenType> {
    override fun read(buf: ByteBuffer): RsParenType {
        return RsParenType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsParenType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsParenType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSType.write(value.`ty`, buf)
    }
}

data class RsPath(
    var `astNode`: RsNode,
    var `segment`: RsPathSegment?,
    var `qualifier`: List<RsPath>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSPath : FfiConverterRustBuffer<RsPath> {
    override fun read(buf: ByteBuffer): RsPath {
        return RsPath(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPathSegment.read(buf),
            FfiConverterSequenceTypeRSPath.read(buf),
        )
    }

    override fun allocationSize(value: RsPath) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPathSegment.allocationSize(value.`segment`) +
            FfiConverterSequenceTypeRSPath.allocationSize(value.`qualifier`))

    override fun write(value: RsPath, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPathSegment.write(value.`segment`, buf)
        FfiConverterSequenceTypeRSPath.write(value.`qualifier`, buf)
    }
}

data class RsPathExpr(var `astNode`: RsNode, var `path`: RsPath?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSPathExpr : FfiConverterRustBuffer<RsPathExpr> {
    override fun read(buf: ByteBuffer): RsPathExpr {
        return RsPathExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPath.read(buf),
        )
    }

    override fun allocationSize(value: RsPathExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPath.allocationSize(value.`path`))

    override fun write(value: RsPathExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPath.write(value.`path`, buf)
    }
}

data class RsPathPat(var `astNode`: RsNode, var `path`: RsPath?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSPathPat : FfiConverterRustBuffer<RsPathPat> {
    override fun read(buf: ByteBuffer): RsPathPat {
        return RsPathPat(FfiConverterTypeRSNode.read(buf), FfiConverterOptionalTypeRSPath.read(buf))
    }

    override fun allocationSize(value: RsPathPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPath.allocationSize(value.`path`))

    override fun write(value: RsPathPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPath.write(value.`path`, buf)
    }
}

data class RsPathSegment(
    var `astNode`: RsNode,
    var `nameRef`: RsNameRef?,
    var `typeArgs`: List<RsType>,
    var `retType`: List<RsType>,
    var `retTypeSyntax`: RsReturnTypeSyntax?,
    var `tyAnchor`: RsTypeAnchor?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSPathSegment : FfiConverterRustBuffer<RsPathSegment> {
    override fun read(buf: ByteBuffer): RsPathSegment {
        return RsPathSegment(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSNameRef.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
            FfiConverterOptionalTypeRSReturnTypeSyntax.read(buf),
            FfiConverterOptionalTypeRSTypeAnchor.read(buf),
        )
    }

    override fun allocationSize(value: RsPathSegment) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSNameRef.allocationSize(value.`nameRef`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`typeArgs`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`retType`) +
            FfiConverterOptionalTypeRSReturnTypeSyntax.allocationSize(value.`retTypeSyntax`) +
            FfiConverterOptionalTypeRSTypeAnchor.allocationSize(value.`tyAnchor`))

    override fun write(value: RsPathSegment, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSNameRef.write(value.`nameRef`, buf)
        FfiConverterSequenceTypeRSType.write(value.`typeArgs`, buf)
        FfiConverterSequenceTypeRSType.write(value.`retType`, buf)
        FfiConverterOptionalTypeRSReturnTypeSyntax.write(value.`retTypeSyntax`, buf)
        FfiConverterOptionalTypeRSTypeAnchor.write(value.`tyAnchor`, buf)
    }
}

data class RsPathType(var `astNode`: RsNode, var `path`: RsPath?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSPathType : FfiConverterRustBuffer<RsPathType> {
    override fun read(buf: ByteBuffer): RsPathType {
        return RsPathType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPath.read(buf),
        )
    }

    override fun allocationSize(value: RsPathType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPath.allocationSize(value.`path`))

    override fun write(value: RsPathType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPath.write(value.`path`, buf)
    }
}

data class RsPrefixExpr(
    var `astNode`: RsNode,
    var `operator`: kotlin.String,
    var `expr`: List<RsExpr>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSPrefixExpr : FfiConverterRustBuffer<RsPrefixExpr> {
    override fun read(buf: ByteBuffer): RsPrefixExpr {
        return RsPrefixExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterString.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsPrefixExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterString.allocationSize(value.`operator`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsPrefixExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterString.write(value.`operator`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsProblem(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSProblem : FfiConverterRustBuffer<RsProblem> {
    override fun read(buf: ByteBuffer): RsProblem {
        return RsProblem(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsProblem) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsProblem, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsPtrType(
    var `astNode`: RsNode,
    var `ty`: List<RsType>,
    var `hasStar`: kotlin.Boolean,
    var `isConst`: kotlin.Boolean,
    var `isMut`: kotlin.Boolean,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSPtrType : FfiConverterRustBuffer<RsPtrType> {
    override fun read(buf: ByteBuffer): RsPtrType {
        return RsPtrType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
        )
    }

    override fun allocationSize(value: RsPtrType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`ty`) +
            FfiConverterBoolean.allocationSize(value.`hasStar`) +
            FfiConverterBoolean.allocationSize(value.`isConst`) +
            FfiConverterBoolean.allocationSize(value.`isMut`))

    override fun write(value: RsPtrType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSType.write(value.`ty`, buf)
        FfiConverterBoolean.write(value.`hasStar`, buf)
        FfiConverterBoolean.write(value.`isConst`, buf)
        FfiConverterBoolean.write(value.`isMut`, buf)
    }
}

data class RsRangeExpr(
    var `astNode`: RsNode,
    var `expressions`: List<RsExpr>,
    var `operator`: kotlin.String,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRangeExpr : FfiConverterRustBuffer<RsRangeExpr> {
    override fun read(buf: ByteBuffer): RsRangeExpr {
        return RsRangeExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterString.read(buf),
        )
    }

    override fun allocationSize(value: RsRangeExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expressions`) +
            FfiConverterString.allocationSize(value.`operator`))

    override fun write(value: RsRangeExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expressions`, buf)
        FfiConverterString.write(value.`operator`, buf)
    }
}

data class RsRangePat(
    var `astNode`: RsNode,
    var `patterns`: List<RsPat>,
    var `operator`: kotlin.String,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRangePat : FfiConverterRustBuffer<RsRangePat> {
    override fun read(buf: ByteBuffer): RsRangePat {
        return RsRangePat(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSPat.read(buf),
            FfiConverterString.read(buf),
        )
    }

    override fun allocationSize(value: RsRangePat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`patterns`) +
            FfiConverterString.allocationSize(value.`operator`))

    override fun write(value: RsRangePat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`patterns`, buf)
        FfiConverterString.write(value.`operator`, buf)
    }
}

data class RsRecordExpr(
    var `astNode`: RsNode,
    var `path`: RsPath?,
    var `fields`: List<RsRecordExprField>,
    var `spread`: List<RsExpr>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRecordExpr : FfiConverterRustBuffer<RsRecordExpr> {
    override fun read(buf: ByteBuffer): RsRecordExpr {
        return RsRecordExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPath.read(buf),
            FfiConverterSequenceTypeRSRecordExprField.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsRecordExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPath.allocationSize(value.`path`) +
            FfiConverterSequenceTypeRSRecordExprField.allocationSize(value.`fields`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`spread`))

    override fun write(value: RsRecordExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPath.write(value.`path`, buf)
        FfiConverterSequenceTypeRSRecordExprField.write(value.`fields`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`spread`, buf)
    }
}

data class RsRecordExprField(
    var `astNode`: RsNode,
    var `name`: RsNameRef?,
    var `expr`: List<RsExpr>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRecordExprField : FfiConverterRustBuffer<RsRecordExprField> {
    override fun read(buf: ByteBuffer): RsRecordExprField {
        return RsRecordExprField(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSNameRef.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsRecordExprField) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSNameRef.allocationSize(value.`name`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsRecordExprField, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSNameRef.write(value.`name`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsRecordField(
    var `astNode`: RsNode,
    var `fieldType`: RsType?,
    var `expr`: RsExpr?,
    var `name`: kotlin.String?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRecordField : FfiConverterRustBuffer<RsRecordField> {
    override fun read(buf: ByteBuffer): RsRecordField {
        return RsRecordField(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
            FfiConverterOptionalTypeRSExpr.read(buf),
            FfiConverterOptionalString.read(buf),
        )
    }

    override fun allocationSize(value: RsRecordField) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`fieldType`) +
            FfiConverterOptionalTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterOptionalString.allocationSize(value.`name`))

    override fun write(value: RsRecordField, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSType.write(value.`fieldType`, buf)
        FfiConverterOptionalTypeRSExpr.write(value.`expr`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
    }
}

data class RsRecordFieldList(var `astNode`: RsNode, var `fields`: List<RsRecordField>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRecordFieldList : FfiConverterRustBuffer<RsRecordFieldList> {
    override fun read(buf: ByteBuffer): RsRecordFieldList {
        return RsRecordFieldList(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSRecordField.read(buf),
        )
    }

    override fun allocationSize(value: RsRecordFieldList) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSRecordField.allocationSize(value.`fields`))

    override fun write(value: RsRecordFieldList, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSRecordField.write(value.`fields`, buf)
    }
}

data class RsRecordPat(
    var `astNode`: RsNode,
    var `path`: RsPath?,
    var `fields`: List<RsRecordPatField>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRecordPat : FfiConverterRustBuffer<RsRecordPat> {
    override fun read(buf: ByteBuffer): RsRecordPat {
        return RsRecordPat(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPath.read(buf),
            FfiConverterSequenceTypeRSRecordPatField.read(buf),
        )
    }

    override fun allocationSize(value: RsRecordPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPath.allocationSize(value.`path`) +
            FfiConverterSequenceTypeRSRecordPatField.allocationSize(value.`fields`))

    override fun write(value: RsRecordPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPath.write(value.`path`, buf)
        FfiConverterSequenceTypeRSRecordPatField.write(value.`fields`, buf)
    }
}

data class RsRecordPatField(var `astNode`: RsNode, var `name`: RsNameRef?, var `pat`: List<RsPat>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRecordPatField : FfiConverterRustBuffer<RsRecordPatField> {
    override fun read(buf: ByteBuffer): RsRecordPatField {
        return RsRecordPatField(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSNameRef.read(buf),
            FfiConverterSequenceTypeRSPat.read(buf),
        )
    }

    override fun allocationSize(value: RsRecordPatField) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSNameRef.allocationSize(value.`name`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`pat`))

    override fun write(value: RsRecordPatField, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSNameRef.write(value.`name`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`pat`, buf)
    }
}

data class RsRefExpr(
    var `astNode`: RsNode,
    var `expr`: List<RsExpr>,
    var `mutable`: kotlin.Boolean,
    var `isRef`: kotlin.Boolean,
    var `isConst`: kotlin.Boolean,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRefExpr : FfiConverterRustBuffer<RsRefExpr> {
    override fun read(buf: ByteBuffer): RsRefExpr {
        return RsRefExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
        )
    }

    override fun allocationSize(value: RsRefExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterBoolean.allocationSize(value.`mutable`) +
            FfiConverterBoolean.allocationSize(value.`isRef`) +
            FfiConverterBoolean.allocationSize(value.`isConst`))

    override fun write(value: RsRefExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterBoolean.write(value.`mutable`, buf)
        FfiConverterBoolean.write(value.`isRef`, buf)
        FfiConverterBoolean.write(value.`isConst`, buf)
    }
}

data class RsRefPat(
    var `astNode`: RsNode,
    var `pat`: List<RsPat>,
    var `mutable`: kotlin.Boolean,
    var `isRef`: kotlin.Boolean,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRefPat : FfiConverterRustBuffer<RsRefPat> {
    override fun read(buf: ByteBuffer): RsRefPat {
        return RsRefPat(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSPat.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
        )
    }

    override fun allocationSize(value: RsRefPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`pat`) +
            FfiConverterBoolean.allocationSize(value.`mutable`) +
            FfiConverterBoolean.allocationSize(value.`isRef`))

    override fun write(value: RsRefPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`pat`, buf)
        FfiConverterBoolean.write(value.`mutable`, buf)
        FfiConverterBoolean.write(value.`isRef`, buf)
    }
}

data class RsRefType(
    var `astNode`: RsNode,
    var `lifetime`: RsLifetime?,
    var `ty`: List<RsType>,
    var `hasAmp`: kotlin.Boolean,
    var `hasMut`: kotlin.Boolean,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRefType : FfiConverterRustBuffer<RsRefType> {
    override fun read(buf: ByteBuffer): RsRefType {
        return RsRefType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSLifetime.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
        )
    }

    override fun allocationSize(value: RsRefType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSLifetime.allocationSize(value.`lifetime`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`ty`) +
            FfiConverterBoolean.allocationSize(value.`hasAmp`) +
            FfiConverterBoolean.allocationSize(value.`hasMut`))

    override fun write(value: RsRefType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSLifetime.write(value.`lifetime`, buf)
        FfiConverterSequenceTypeRSType.write(value.`ty`, buf)
        FfiConverterBoolean.write(value.`hasAmp`, buf)
        FfiConverterBoolean.write(value.`hasMut`, buf)
    }
}

data class RsRestPat(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSRestPat : FfiConverterRustBuffer<RsRestPat> {
    override fun read(buf: ByteBuffer): RsRestPat {
        return RsRestPat(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsRestPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsRestPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsReturnExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSReturnExpr : FfiConverterRustBuffer<RsReturnExpr> {
    override fun read(buf: ByteBuffer): RsReturnExpr {
        return RsReturnExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsReturnExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsReturnExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsReturnTypeSyntax(
    var `astNode`: RsNode,
    var `lParen`: kotlin.Boolean,
    var `rParen`: kotlin.Boolean,
    var `dotdot`: kotlin.Boolean,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSReturnTypeSyntax : FfiConverterRustBuffer<RsReturnTypeSyntax> {
    override fun read(buf: ByteBuffer): RsReturnTypeSyntax {
        return RsReturnTypeSyntax(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
            FfiConverterBoolean.read(buf),
        )
    }

    override fun allocationSize(value: RsReturnTypeSyntax) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterBoolean.allocationSize(value.`lParen`) +
            FfiConverterBoolean.allocationSize(value.`rParen`) +
            FfiConverterBoolean.allocationSize(value.`dotdot`))

    override fun write(value: RsReturnTypeSyntax, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterBoolean.write(value.`lParen`, buf)
        FfiConverterBoolean.write(value.`rParen`, buf)
        FfiConverterBoolean.write(value.`dotdot`, buf)
    }
}

data class RsSelfParam(var `astNode`: RsNode, var `ty`: RsType?, var `lifetime`: RsLifetime?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSSelfParam : FfiConverterRustBuffer<RsSelfParam> {
    override fun read(buf: ByteBuffer): RsSelfParam {
        return RsSelfParam(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
            FfiConverterOptionalTypeRSLifetime.read(buf),
        )
    }

    override fun allocationSize(value: RsSelfParam) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`) +
            FfiConverterOptionalTypeRSLifetime.allocationSize(value.`lifetime`))

    override fun write(value: RsSelfParam, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
        FfiConverterOptionalTypeRSLifetime.write(value.`lifetime`, buf)
    }
}

data class RsSlicePat(var `astNode`: RsNode, var `pats`: List<RsPat>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSSlicePat : FfiConverterRustBuffer<RsSlicePat> {
    override fun read(buf: ByteBuffer): RsSlicePat {
        return RsSlicePat(FfiConverterTypeRSNode.read(buf), FfiConverterSequenceTypeRSPat.read(buf))
    }

    override fun allocationSize(value: RsSlicePat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`pats`))

    override fun write(value: RsSlicePat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`pats`, buf)
    }
}

data class RsSliceType(var `astNode`: RsNode, var `ty`: List<RsType>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSSliceType : FfiConverterRustBuffer<RsSliceType> {
    override fun read(buf: ByteBuffer): RsSliceType {
        return RsSliceType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsSliceType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsSliceType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSType.write(value.`ty`, buf)
    }
}

data class RsSourceFile(
    var `astNode`: RsNode,
    var `path`: kotlin.String,
    var `items`: List<RsAst>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSSourceFile : FfiConverterRustBuffer<RsSourceFile> {
    override fun read(buf: ByteBuffer): RsSourceFile {
        return RsSourceFile(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterString.read(buf),
            FfiConverterSequenceTypeRSAst.read(buf),
        )
    }

    override fun allocationSize(value: RsSourceFile) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterString.allocationSize(value.`path`) +
            FfiConverterSequenceTypeRSAst.allocationSize(value.`items`))

    override fun write(value: RsSourceFile, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterString.write(value.`path`, buf)
        FfiConverterSequenceTypeRSAst.write(value.`items`, buf)
    }
}

data class RsStatic(var `astNode`: RsNode, var `name`: kotlin.String?, var `ty`: RsType?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSStatic : FfiConverterRustBuffer<RsStatic> {
    override fun read(buf: ByteBuffer): RsStatic {
        return RsStatic(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsStatic) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsStatic, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
    }
}

data class RsStruct(
    var `astNode`: RsNode,
    var `name`: kotlin.String?,
    var `fieldList`: RsFieldList?,
    var `genericParams`: List<RsGenericParam>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSStruct : FfiConverterRustBuffer<RsStruct> {
    override fun read(buf: ByteBuffer): RsStruct {
        return RsStruct(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterOptionalTypeRSFieldList.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
        )
    }

    override fun allocationSize(value: RsStruct) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterOptionalTypeRSFieldList.allocationSize(value.`fieldList`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericParams`))

    override fun write(value: RsStruct, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterOptionalTypeRSFieldList.write(value.`fieldList`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericParams`, buf)
    }
}

data class RsTrait(
    var `astNode`: RsNode,
    var `name`: kotlin.String?,
    var `items`: List<RsAssocItem>,
    var `genericParams`: List<RsGenericParam>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTrait : FfiConverterRustBuffer<RsTrait> {
    override fun read(buf: ByteBuffer): RsTrait {
        return RsTrait(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterSequenceTypeRSAssocItem.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
        )
    }

    override fun allocationSize(value: RsTrait) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterSequenceTypeRSAssocItem.allocationSize(value.`items`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericParams`))

    override fun write(value: RsTrait, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterSequenceTypeRSAssocItem.write(value.`items`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericParams`, buf)
    }
}

data class RsTryExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTryExpr : FfiConverterRustBuffer<RsTryExpr> {
    override fun read(buf: ByteBuffer): RsTryExpr {
        return RsTryExpr(FfiConverterTypeRSNode.read(buf), FfiConverterSequenceTypeRSExpr.read(buf))
    }

    override fun allocationSize(value: RsTryExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsTryExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsTupleExpr(var `astNode`: RsNode, var `exprs`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTupleExpr : FfiConverterRustBuffer<RsTupleExpr> {
    override fun read(buf: ByteBuffer): RsTupleExpr {
        return RsTupleExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsTupleExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`exprs`))

    override fun write(value: RsTupleExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`exprs`, buf)
    }
}

data class RsTupleField(var `astNode`: RsNode, var `fieldType`: RsType?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTupleField : FfiConverterRustBuffer<RsTupleField> {
    override fun read(buf: ByteBuffer): RsTupleField {
        return RsTupleField(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsTupleField) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`fieldType`))

    override fun write(value: RsTupleField, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSType.write(value.`fieldType`, buf)
    }
}

data class RsTupleFieldList(var `astNode`: RsNode, var `fields`: List<RsTupleField>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTupleFieldList : FfiConverterRustBuffer<RsTupleFieldList> {
    override fun read(buf: ByteBuffer): RsTupleFieldList {
        return RsTupleFieldList(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSTupleField.read(buf),
        )
    }

    override fun allocationSize(value: RsTupleFieldList) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSTupleField.allocationSize(value.`fields`))

    override fun write(value: RsTupleFieldList, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSTupleField.write(value.`fields`, buf)
    }
}

data class RsTuplePat(var `astNode`: RsNode, var `fields`: List<RsPat>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTuplePat : FfiConverterRustBuffer<RsTuplePat> {
    override fun read(buf: ByteBuffer): RsTuplePat {
        return RsTuplePat(FfiConverterTypeRSNode.read(buf), FfiConverterSequenceTypeRSPat.read(buf))
    }

    override fun allocationSize(value: RsTuplePat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`fields`))

    override fun write(value: RsTuplePat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`fields`, buf)
    }
}

data class RsTupleStructPat(var `astNode`: RsNode, var `path`: RsPath?, var `fields`: List<RsPat>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTupleStructPat : FfiConverterRustBuffer<RsTupleStructPat> {
    override fun read(buf: ByteBuffer): RsTupleStructPat {
        return RsTupleStructPat(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPath.read(buf),
            FfiConverterSequenceTypeRSPat.read(buf),
        )
    }

    override fun allocationSize(value: RsTupleStructPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPath.allocationSize(value.`path`) +
            FfiConverterSequenceTypeRSPat.allocationSize(value.`fields`))

    override fun write(value: RsTupleStructPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPath.write(value.`path`, buf)
        FfiConverterSequenceTypeRSPat.write(value.`fields`, buf)
    }
}

data class RsTupleType(var `astNode`: RsNode, var `fields`: List<RsType>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTupleType : FfiConverterRustBuffer<RsTupleType> {
    override fun read(buf: ByteBuffer): RsTupleType {
        return RsTupleType(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsTupleType) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`fields`))

    override fun write(value: RsTupleType, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSType.write(value.`fields`, buf)
    }
}

data class RsTypeAlias(
    var `astNode`: RsNode,
    var `name`: kotlin.String?,
    var `ty`: RsType?,
    var `typeBoundList`: List<RsTypeBound>,
    var `genericParams`: List<RsGenericParam>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTypeAlias : FfiConverterRustBuffer<RsTypeAlias> {
    override fun read(buf: ByteBuffer): RsTypeAlias {
        return RsTypeAlias(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
            FfiConverterSequenceTypeRSTypeBound.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
        )
    }

    override fun allocationSize(value: RsTypeAlias) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`) +
            FfiConverterSequenceTypeRSTypeBound.allocationSize(value.`typeBoundList`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericParams`))

    override fun write(value: RsTypeAlias, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
        FfiConverterSequenceTypeRSTypeBound.write(value.`typeBoundList`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericParams`, buf)
    }
}

data class RsTypeAnchor(
    var `astNode`: RsNode,
    var `pathTy`: List<RsPathType>,
    var `ty`: List<RsType>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTypeAnchor : FfiConverterRustBuffer<RsTypeAnchor> {
    override fun read(buf: ByteBuffer): RsTypeAnchor {
        return RsTypeAnchor(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSPathType.read(buf),
            FfiConverterSequenceTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsTypeAnchor) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSPathType.allocationSize(value.`pathTy`) +
            FfiConverterSequenceTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsTypeAnchor, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSPathType.write(value.`pathTy`, buf)
        FfiConverterSequenceTypeRSType.write(value.`ty`, buf)
    }
}

data class RsTypeArg(var `astNode`: RsNode, var `ty`: RsType?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTypeArg : FfiConverterRustBuffer<RsTypeArg> {
    override fun read(buf: ByteBuffer): RsTypeArg {
        return RsTypeArg(FfiConverterTypeRSNode.read(buf), FfiConverterOptionalTypeRSType.read(buf))
    }

    override fun allocationSize(value: RsTypeArg) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`))

    override fun write(value: RsTypeArg, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
    }
}

data class RsTypeBound(
    var `astNode`: RsNode,
    var `ty`: RsType?,
    var `genericsInFor`: List<RsGenericParam>,
    var `lifetime`: RsLifetime?,
    var `boundGenericArgs`: List<RsUseBoundGenericArg>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTypeBound : FfiConverterRustBuffer<RsTypeBound> {
    override fun read(buf: ByteBuffer): RsTypeBound {
        return RsTypeBound(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
            FfiConverterOptionalTypeRSLifetime.read(buf),
            FfiConverterSequenceTypeRSUseBoundGenericArg.read(buf),
        )
    }

    override fun allocationSize(value: RsTypeBound) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`ty`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericsInFor`) +
            FfiConverterOptionalTypeRSLifetime.allocationSize(value.`lifetime`) +
            FfiConverterSequenceTypeRSUseBoundGenericArg.allocationSize(value.`boundGenericArgs`))

    override fun write(value: RsTypeBound, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSType.write(value.`ty`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericsInFor`, buf)
        FfiConverterOptionalTypeRSLifetime.write(value.`lifetime`, buf)
        FfiConverterSequenceTypeRSUseBoundGenericArg.write(value.`boundGenericArgs`, buf)
    }
}

data class RsTypeParam(
    var `astNode`: RsNode,
    var `name`: kotlin.String?,
    var `typeBoundList`: List<RsTypeBound>,
    var `defaultType`: RsType?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSTypeParam : FfiConverterRustBuffer<RsTypeParam> {
    override fun read(buf: ByteBuffer): RsTypeParam {
        return RsTypeParam(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterSequenceTypeRSTypeBound.read(buf),
            FfiConverterOptionalTypeRSType.read(buf),
        )
    }

    override fun allocationSize(value: RsTypeParam) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterSequenceTypeRSTypeBound.allocationSize(value.`typeBoundList`) +
            FfiConverterOptionalTypeRSType.allocationSize(value.`defaultType`))

    override fun write(value: RsTypeParam, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterSequenceTypeRSTypeBound.write(value.`typeBoundList`, buf)
        FfiConverterOptionalTypeRSType.write(value.`defaultType`, buf)
    }
}

data class RsUnderscoreExpr(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSUnderscoreExpr : FfiConverterRustBuffer<RsUnderscoreExpr> {
    override fun read(buf: ByteBuffer): RsUnderscoreExpr {
        return RsUnderscoreExpr(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsUnderscoreExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsUnderscoreExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsUnion(
    var `astNode`: RsNode,
    var `genericParams`: List<RsGenericParam>,
    var `name`: kotlin.String?,
    var `fieldList`: RsRecordFieldList?,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSUnion : FfiConverterRustBuffer<RsUnion> {
    override fun read(buf: ByteBuffer): RsUnion {
        return RsUnion(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSGenericParam.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterOptionalTypeRSRecordFieldList.read(buf),
        )
    }

    override fun allocationSize(value: RsUnion) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSGenericParam.allocationSize(value.`genericParams`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterOptionalTypeRSRecordFieldList.allocationSize(value.`fieldList`))

    override fun write(value: RsUnion, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSGenericParam.write(value.`genericParams`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterOptionalTypeRSRecordFieldList.write(value.`fieldList`, buf)
    }
}

data class RsUse(var `astNode`: RsNode, var `useTree`: RsUseTree?) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSUse : FfiConverterRustBuffer<RsUse> {
    override fun read(buf: ByteBuffer): RsUse {
        return RsUse(FfiConverterTypeRSNode.read(buf), FfiConverterOptionalTypeRSUseTree.read(buf))
    }

    override fun allocationSize(value: RsUse) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSUseTree.allocationSize(value.`useTree`))

    override fun write(value: RsUse, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSUseTree.write(value.`useTree`, buf)
    }
}

data class RsUseTree(
    var `astNode`: RsNode,
    var `path`: RsPath?,
    var `rename`: kotlin.String?,
    var `useTrees`: List<RsUseTree>,
    var `star`: kotlin.Boolean,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSUseTree : FfiConverterRustBuffer<RsUseTree> {
    override fun read(buf: ByteBuffer): RsUseTree {
        return RsUseTree(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalTypeRSPath.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterSequenceTypeRSUseTree.read(buf),
            FfiConverterBoolean.read(buf),
        )
    }

    override fun allocationSize(value: RsUseTree) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalTypeRSPath.allocationSize(value.`path`) +
            FfiConverterOptionalString.allocationSize(value.`rename`) +
            FfiConverterSequenceTypeRSUseTree.allocationSize(value.`useTrees`) +
            FfiConverterBoolean.allocationSize(value.`star`))

    override fun write(value: RsUseTree, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalTypeRSPath.write(value.`path`, buf)
        FfiConverterOptionalString.write(value.`rename`, buf)
        FfiConverterSequenceTypeRSUseTree.write(value.`useTrees`, buf)
        FfiConverterBoolean.write(value.`star`, buf)
    }
}

data class RsVariant(
    var `astNode`: RsNode,
    var `name`: kotlin.String?,
    var `expr`: List<RsExpr>,
    var `fields`: List<RsFieldList>,
) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSVariant : FfiConverterRustBuffer<RsVariant> {
    override fun read(buf: ByteBuffer): RsVariant {
        return RsVariant(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterOptionalString.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
            FfiConverterSequenceTypeRSFieldList.read(buf),
        )
    }

    override fun allocationSize(value: RsVariant) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterOptionalString.allocationSize(value.`name`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`) +
            FfiConverterSequenceTypeRSFieldList.allocationSize(value.`fields`))

    override fun write(value: RsVariant, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterOptionalString.write(value.`name`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
        FfiConverterSequenceTypeRSFieldList.write(value.`fields`, buf)
    }
}

data class RsWhileExpr(var `astNode`: RsNode, var `expressions`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSWhileExpr : FfiConverterRustBuffer<RsWhileExpr> {
    override fun read(buf: ByteBuffer): RsWhileExpr {
        return RsWhileExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsWhileExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expressions`))

    override fun write(value: RsWhileExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expressions`, buf)
    }
}

data class RsWildcardPat(var `astNode`: RsNode) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSWildcardPat : FfiConverterRustBuffer<RsWildcardPat> {
    override fun read(buf: ByteBuffer): RsWildcardPat {
        return RsWildcardPat(FfiConverterTypeRSNode.read(buf))
    }

    override fun allocationSize(value: RsWildcardPat) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`))

    override fun write(value: RsWildcardPat, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
    }
}

data class RsYeetExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSYeetExpr : FfiConverterRustBuffer<RsYeetExpr> {
    override fun read(buf: ByteBuffer): RsYeetExpr {
        return RsYeetExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsYeetExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsYeetExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

data class RsYieldExpr(var `astNode`: RsNode, var `expr`: List<RsExpr>) {

    companion object
}

/** @suppress */
public object FfiConverterTypeRSYieldExpr : FfiConverterRustBuffer<RsYieldExpr> {
    override fun read(buf: ByteBuffer): RsYieldExpr {
        return RsYieldExpr(
            FfiConverterTypeRSNode.read(buf),
            FfiConverterSequenceTypeRSExpr.read(buf),
        )
    }

    override fun allocationSize(value: RsYieldExpr) =
        (FfiConverterTypeRSNode.allocationSize(value.`astNode`) +
            FfiConverterSequenceTypeRSExpr.allocationSize(value.`expr`))

    override fun write(value: RsYieldExpr, buf: ByteBuffer) {
        FfiConverterTypeRSNode.write(value.`astNode`, buf)
        FfiConverterSequenceTypeRSExpr.write(value.`expr`, buf)
    }
}

sealed class RsAdt {

    data class Enum(val v1: RsEnum) : RsAdt() {

        companion object
    }

    data class Struct(val v1: RsStruct) : RsAdt() {

        companion object
    }

    data class Union(val v1: RsUnion) : RsAdt() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAdt : FfiConverterRustBuffer<RsAdt> {
    override fun read(buf: ByteBuffer): RsAdt {
        return when (buf.getInt()) {
            1 -> RsAdt.Enum(FfiConverterTypeRSEnum.read(buf))
            2 -> RsAdt.Struct(FfiConverterTypeRSStruct.read(buf))
            3 -> RsAdt.Union(FfiConverterTypeRSUnion.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsAdt) =
        when (value) {
            is RsAdt.Enum -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSEnum.allocationSize(value.v1))
            }
            is RsAdt.Struct -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSStruct.allocationSize(value.v1))
            }
            is RsAdt.Union -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSUnion.allocationSize(value.v1))
            }
        }

    override fun write(value: RsAdt, buf: ByteBuffer) {
        when (value) {
            is RsAdt.Enum -> {
                buf.putInt(1)
                FfiConverterTypeRSEnum.write(value.v1, buf)
                Unit
            }
            is RsAdt.Struct -> {
                buf.putInt(2)
                FfiConverterTypeRSStruct.write(value.v1, buf)
                Unit
            }
            is RsAdt.Union -> {
                buf.putInt(3)
                FfiConverterTypeRSUnion.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsAsmOperand {

    data class AsmConst(val v1: RsAsmConst) : RsAsmOperand() {

        companion object
    }

    data class AsmLabel(val v1: RsAsmLabel) : RsAsmOperand() {

        companion object
    }

    data class AsmRegOperand(val v1: RsAsmRegOperand) : RsAsmOperand() {

        companion object
    }

    data class AsmSym(val v1: RsAsmSym) : RsAsmOperand() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmOperand : FfiConverterRustBuffer<RsAsmOperand> {
    override fun read(buf: ByteBuffer): RsAsmOperand {
        return when (buf.getInt()) {
            1 -> RsAsmOperand.AsmConst(FfiConverterTypeRSAsmConst.read(buf))
            2 -> RsAsmOperand.AsmLabel(FfiConverterTypeRSAsmLabel.read(buf))
            3 -> RsAsmOperand.AsmRegOperand(FfiConverterTypeRSAsmRegOperand.read(buf))
            4 -> RsAsmOperand.AsmSym(FfiConverterTypeRSAsmSym.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsAsmOperand) =
        when (value) {
            is RsAsmOperand.AsmConst -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAsmConst.allocationSize(value.v1))
            }
            is RsAsmOperand.AsmLabel -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAsmLabel.allocationSize(value.v1))
            }
            is RsAsmOperand.AsmRegOperand -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAsmRegOperand.allocationSize(value.v1))
            }
            is RsAsmOperand.AsmSym -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAsmSym.allocationSize(value.v1))
            }
        }

    override fun write(value: RsAsmOperand, buf: ByteBuffer) {
        when (value) {
            is RsAsmOperand.AsmConst -> {
                buf.putInt(1)
                FfiConverterTypeRSAsmConst.write(value.v1, buf)
                Unit
            }
            is RsAsmOperand.AsmLabel -> {
                buf.putInt(2)
                FfiConverterTypeRSAsmLabel.write(value.v1, buf)
                Unit
            }
            is RsAsmOperand.AsmRegOperand -> {
                buf.putInt(3)
                FfiConverterTypeRSAsmRegOperand.write(value.v1, buf)
                Unit
            }
            is RsAsmOperand.AsmSym -> {
                buf.putInt(4)
                FfiConverterTypeRSAsmSym.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsAsmPiece {

    data class AsmClobberAbi(val v1: RsAsmClobberAbi) : RsAsmPiece() {

        companion object
    }

    data class AsmOperandNamed(val v1: RsAsmOperandNamed) : RsAsmPiece() {

        companion object
    }

    data class AsmOptions(val v1: RsAsmOptions) : RsAsmPiece() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAsmPiece : FfiConverterRustBuffer<RsAsmPiece> {
    override fun read(buf: ByteBuffer): RsAsmPiece {
        return when (buf.getInt()) {
            1 -> RsAsmPiece.AsmClobberAbi(FfiConverterTypeRSAsmClobberAbi.read(buf))
            2 -> RsAsmPiece.AsmOperandNamed(FfiConverterTypeRSAsmOperandNamed.read(buf))
            3 -> RsAsmPiece.AsmOptions(FfiConverterTypeRSAsmOptions.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsAsmPiece) =
        when (value) {
            is RsAsmPiece.AsmClobberAbi -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAsmClobberAbi.allocationSize(value.v1))
            }
            is RsAsmPiece.AsmOperandNamed -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAsmOperandNamed.allocationSize(value.v1))
            }
            is RsAsmPiece.AsmOptions -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAsmOptions.allocationSize(value.v1))
            }
        }

    override fun write(value: RsAsmPiece, buf: ByteBuffer) {
        when (value) {
            is RsAsmPiece.AsmClobberAbi -> {
                buf.putInt(1)
                FfiConverterTypeRSAsmClobberAbi.write(value.v1, buf)
                Unit
            }
            is RsAsmPiece.AsmOperandNamed -> {
                buf.putInt(2)
                FfiConverterTypeRSAsmOperandNamed.write(value.v1, buf)
                Unit
            }
            is RsAsmPiece.AsmOptions -> {
                buf.putInt(3)
                FfiConverterTypeRSAsmOptions.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsAssocItem {

    data class Const(val v1: RsConst) : RsAssocItem() {

        companion object
    }

    data class Fn(val v1: RsFn) : RsAssocItem() {

        companion object
    }

    data class MacroCall(val v1: RsMacroCall) : RsAssocItem() {

        companion object
    }

    data class TypeAlias(val v1: RsTypeAlias) : RsAssocItem() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAssocItem : FfiConverterRustBuffer<RsAssocItem> {
    override fun read(buf: ByteBuffer): RsAssocItem {
        return when (buf.getInt()) {
            1 -> RsAssocItem.Const(FfiConverterTypeRSConst.read(buf))
            2 -> RsAssocItem.Fn(FfiConverterTypeRSFn.read(buf))
            3 -> RsAssocItem.MacroCall(FfiConverterTypeRSMacroCall.read(buf))
            4 -> RsAssocItem.TypeAlias(FfiConverterTypeRSTypeAlias.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsAssocItem) =
        when (value) {
            is RsAssocItem.Const -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSConst.allocationSize(value.v1))
            }
            is RsAssocItem.Fn -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSFn.allocationSize(value.v1))
            }
            is RsAssocItem.MacroCall -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMacroCall.allocationSize(value.v1))
            }
            is RsAssocItem.TypeAlias -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTypeAlias.allocationSize(value.v1))
            }
        }

    override fun write(value: RsAssocItem, buf: ByteBuffer) {
        when (value) {
            is RsAssocItem.Const -> {
                buf.putInt(1)
                FfiConverterTypeRSConst.write(value.v1, buf)
                Unit
            }
            is RsAssocItem.Fn -> {
                buf.putInt(2)
                FfiConverterTypeRSFn.write(value.v1, buf)
                Unit
            }
            is RsAssocItem.MacroCall -> {
                buf.putInt(3)
                FfiConverterTypeRSMacroCall.write(value.v1, buf)
                Unit
            }
            is RsAssocItem.TypeAlias -> {
                buf.putInt(4)
                FfiConverterTypeRSTypeAlias.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

/** Creating a common root node for all AST nodes */
sealed class RsAst {

    data class RustItem(val v1: RsItem) : RsAst() {

        companion object
    }

    data class RustExpr(val v1: RsExpr) : RsAst() {

        companion object
    }

    data class RustStmt(val v1: RsStmt) : RsAst() {

        companion object
    }

    data class RustType(val v1: RsType) : RsAst() {

        companion object
    }

    data class RustAbi(val v1: RsAbi) : RsAst() {

        companion object
    }

    data class RustProblem(val v1: RsProblem) : RsAst() {

        companion object
    }

    data class RustUseTree(val v1: RsUseTree) : RsAst() {

        companion object
    }

    data class RustPat(val v1: RsPat) : RsAst() {

        companion object
    }

    data class RustVariant(val v1: RsVariant) : RsAst() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSAst : FfiConverterRustBuffer<RsAst> {
    override fun read(buf: ByteBuffer): RsAst {
        return when (buf.getInt()) {
            1 -> RsAst.RustItem(FfiConverterTypeRSItem.read(buf))
            2 -> RsAst.RustExpr(FfiConverterTypeRSExpr.read(buf))
            3 -> RsAst.RustStmt(FfiConverterTypeRSStmt.read(buf))
            4 -> RsAst.RustType(FfiConverterTypeRSType.read(buf))
            5 -> RsAst.RustAbi(FfiConverterTypeRSAbi.read(buf))
            6 -> RsAst.RustProblem(FfiConverterTypeRSProblem.read(buf))
            7 -> RsAst.RustUseTree(FfiConverterTypeRSUseTree.read(buf))
            8 -> RsAst.RustPat(FfiConverterTypeRSPat.read(buf))
            9 -> RsAst.RustVariant(FfiConverterTypeRSVariant.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsAst) =
        when (value) {
            is RsAst.RustItem -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSItem.allocationSize(value.v1))
            }
            is RsAst.RustExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSExpr.allocationSize(value.v1))
            }
            is RsAst.RustStmt -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSStmt.allocationSize(value.v1))
            }
            is RsAst.RustType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSType.allocationSize(value.v1))
            }
            is RsAst.RustAbi -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAbi.allocationSize(value.v1))
            }
            is RsAst.RustProblem -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSProblem.allocationSize(value.v1))
            }
            is RsAst.RustUseTree -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSUseTree.allocationSize(value.v1))
            }
            is RsAst.RustPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSPat.allocationSize(value.v1))
            }
            is RsAst.RustVariant -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSVariant.allocationSize(value.v1))
            }
        }

    override fun write(value: RsAst, buf: ByteBuffer) {
        when (value) {
            is RsAst.RustItem -> {
                buf.putInt(1)
                FfiConverterTypeRSItem.write(value.v1, buf)
                Unit
            }
            is RsAst.RustExpr -> {
                buf.putInt(2)
                FfiConverterTypeRSExpr.write(value.v1, buf)
                Unit
            }
            is RsAst.RustStmt -> {
                buf.putInt(3)
                FfiConverterTypeRSStmt.write(value.v1, buf)
                Unit
            }
            is RsAst.RustType -> {
                buf.putInt(4)
                FfiConverterTypeRSType.write(value.v1, buf)
                Unit
            }
            is RsAst.RustAbi -> {
                buf.putInt(5)
                FfiConverterTypeRSAbi.write(value.v1, buf)
                Unit
            }
            is RsAst.RustProblem -> {
                buf.putInt(6)
                FfiConverterTypeRSProblem.write(value.v1, buf)
                Unit
            }
            is RsAst.RustUseTree -> {
                buf.putInt(7)
                FfiConverterTypeRSUseTree.write(value.v1, buf)
                Unit
            }
            is RsAst.RustPat -> {
                buf.putInt(8)
                FfiConverterTypeRSPat.write(value.v1, buf)
                Unit
            }
            is RsAst.RustVariant -> {
                buf.putInt(9)
                FfiConverterTypeRSVariant.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsExpr {

    data class ArrayExpr(val v1: RsArrayExpr) : RsExpr() {

        companion object
    }

    data class AsmExpr(val v1: RsAsmExpr) : RsExpr() {

        companion object
    }

    data class AwaitExpr(val v1: RsAwaitExpr) : RsExpr() {

        companion object
    }

    data class BecomeExpr(val v1: RsBecomeExpr) : RsExpr() {

        companion object
    }

    data class BinExpr(val v1: RsBinExpr) : RsExpr() {

        companion object
    }

    data class BlockExpr(val v1: RsBlockExpr) : RsExpr() {

        companion object
    }

    data class BreakExpr(val v1: RsBreakExpr) : RsExpr() {

        companion object
    }

    data class CallExpr(val v1: RsCallExpr) : RsExpr() {

        companion object
    }

    data class CastExpr(val v1: RsCastExpr) : RsExpr() {

        companion object
    }

    data class ClosureExpr(val v1: RsClosureExpr) : RsExpr() {

        companion object
    }

    data class ContinueExpr(val v1: RsContinueExpr) : RsExpr() {

        companion object
    }

    data class FieldExpr(val v1: RsFieldExpr) : RsExpr() {

        companion object
    }

    data class ForExpr(val v1: RsForExpr) : RsExpr() {

        companion object
    }

    data class FormatArgsExpr(val v1: RsFormatArgsExpr) : RsExpr() {

        companion object
    }

    data class IfExpr(val v1: RsIfExpr) : RsExpr() {

        companion object
    }

    data class IndexExpr(val v1: RsIndexExpr) : RsExpr() {

        companion object
    }

    data class LetExpr(val v1: RsLetExpr) : RsExpr() {

        companion object
    }

    data class Literal(val v1: RsLiteral) : RsExpr() {

        companion object
    }

    data class LoopExpr(val v1: RsLoopExpr) : RsExpr() {

        companion object
    }

    data class MacroExpr(val v1: RsMacroExpr) : RsExpr() {

        companion object
    }

    data class MatchExpr(val v1: RsMatchExpr) : RsExpr() {

        companion object
    }

    data class MatchArm(val v1: RsMatchArm) : RsExpr() {

        companion object
    }

    data class MethodCallExpr(val v1: RsMethodCallExpr) : RsExpr() {

        companion object
    }

    data class OffsetOfExpr(val v1: RsOffsetOfExpr) : RsExpr() {

        companion object
    }

    data class ParenExpr(val v1: RsParenExpr) : RsExpr() {

        companion object
    }

    data class PathExpr(val v1: RsPathExpr) : RsExpr() {

        companion object
    }

    data class PrefixExpr(val v1: RsPrefixExpr) : RsExpr() {

        companion object
    }

    data class RangeExpr(val v1: RsRangeExpr) : RsExpr() {

        companion object
    }

    data class RecordExpr(val v1: RsRecordExpr) : RsExpr() {

        companion object
    }

    data class RefExpr(val v1: RsRefExpr) : RsExpr() {

        companion object
    }

    data class ReturnExpr(val v1: RsReturnExpr) : RsExpr() {

        companion object
    }

    data class TryExpr(val v1: RsTryExpr) : RsExpr() {

        companion object
    }

    data class TupleExpr(val v1: RsTupleExpr) : RsExpr() {

        companion object
    }

    data class UnderscoreExpr(val v1: RsUnderscoreExpr) : RsExpr() {

        companion object
    }

    data class WhileExpr(val v1: RsWhileExpr) : RsExpr() {

        companion object
    }

    data class YeetExpr(val v1: RsYeetExpr) : RsExpr() {

        companion object
    }

    data class YieldExpr(val v1: RsYieldExpr) : RsExpr() {

        companion object
    }

    data class Path(val v1: RsPath) : RsExpr() {

        companion object
    }

    data class PathSegment(val v1: RsPathSegment) : RsExpr() {

        companion object
    }

    data class NameRef(val v1: RsNameRef) : RsExpr() {

        companion object
    }

    data class RecordExprField(val v1: RsRecordExprField) : RsExpr() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSExpr : FfiConverterRustBuffer<RsExpr> {
    override fun read(buf: ByteBuffer): RsExpr {
        return when (buf.getInt()) {
            1 -> RsExpr.ArrayExpr(FfiConverterTypeRSArrayExpr.read(buf))
            2 -> RsExpr.AsmExpr(FfiConverterTypeRSAsmExpr.read(buf))
            3 -> RsExpr.AwaitExpr(FfiConverterTypeRSAwaitExpr.read(buf))
            4 -> RsExpr.BecomeExpr(FfiConverterTypeRSBecomeExpr.read(buf))
            5 -> RsExpr.BinExpr(FfiConverterTypeRSBinExpr.read(buf))
            6 -> RsExpr.BlockExpr(FfiConverterTypeRSBlockExpr.read(buf))
            7 -> RsExpr.BreakExpr(FfiConverterTypeRSBreakExpr.read(buf))
            8 -> RsExpr.CallExpr(FfiConverterTypeRSCallExpr.read(buf))
            9 -> RsExpr.CastExpr(FfiConverterTypeRSCastExpr.read(buf))
            10 -> RsExpr.ClosureExpr(FfiConverterTypeRSClosureExpr.read(buf))
            11 -> RsExpr.ContinueExpr(FfiConverterTypeRSContinueExpr.read(buf))
            12 -> RsExpr.FieldExpr(FfiConverterTypeRSFieldExpr.read(buf))
            13 -> RsExpr.ForExpr(FfiConverterTypeRSForExpr.read(buf))
            14 -> RsExpr.FormatArgsExpr(FfiConverterTypeRSFormatArgsExpr.read(buf))
            15 -> RsExpr.IfExpr(FfiConverterTypeRSIfExpr.read(buf))
            16 -> RsExpr.IndexExpr(FfiConverterTypeRSIndexExpr.read(buf))
            17 -> RsExpr.LetExpr(FfiConverterTypeRSLetExpr.read(buf))
            18 -> RsExpr.Literal(FfiConverterTypeRSLiteral.read(buf))
            19 -> RsExpr.LoopExpr(FfiConverterTypeRSLoopExpr.read(buf))
            20 -> RsExpr.MacroExpr(FfiConverterTypeRSMacroExpr.read(buf))
            21 -> RsExpr.MatchExpr(FfiConverterTypeRSMatchExpr.read(buf))
            22 -> RsExpr.MatchArm(FfiConverterTypeRSMatchArm.read(buf))
            23 -> RsExpr.MethodCallExpr(FfiConverterTypeRSMethodCallExpr.read(buf))
            24 -> RsExpr.OffsetOfExpr(FfiConverterTypeRSOffsetOfExpr.read(buf))
            25 -> RsExpr.ParenExpr(FfiConverterTypeRSParenExpr.read(buf))
            26 -> RsExpr.PathExpr(FfiConverterTypeRSPathExpr.read(buf))
            27 -> RsExpr.PrefixExpr(FfiConverterTypeRSPrefixExpr.read(buf))
            28 -> RsExpr.RangeExpr(FfiConverterTypeRSRangeExpr.read(buf))
            29 -> RsExpr.RecordExpr(FfiConverterTypeRSRecordExpr.read(buf))
            30 -> RsExpr.RefExpr(FfiConverterTypeRSRefExpr.read(buf))
            31 -> RsExpr.ReturnExpr(FfiConverterTypeRSReturnExpr.read(buf))
            32 -> RsExpr.TryExpr(FfiConverterTypeRSTryExpr.read(buf))
            33 -> RsExpr.TupleExpr(FfiConverterTypeRSTupleExpr.read(buf))
            34 -> RsExpr.UnderscoreExpr(FfiConverterTypeRSUnderscoreExpr.read(buf))
            35 -> RsExpr.WhileExpr(FfiConverterTypeRSWhileExpr.read(buf))
            36 -> RsExpr.YeetExpr(FfiConverterTypeRSYeetExpr.read(buf))
            37 -> RsExpr.YieldExpr(FfiConverterTypeRSYieldExpr.read(buf))
            38 -> RsExpr.Path(FfiConverterTypeRSPath.read(buf))
            39 -> RsExpr.PathSegment(FfiConverterTypeRSPathSegment.read(buf))
            40 -> RsExpr.NameRef(FfiConverterTypeRSNameRef.read(buf))
            41 -> RsExpr.RecordExprField(FfiConverterTypeRSRecordExprField.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsExpr) =
        when (value) {
            is RsExpr.ArrayExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSArrayExpr.allocationSize(value.v1))
            }
            is RsExpr.AsmExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAsmExpr.allocationSize(value.v1))
            }
            is RsExpr.AwaitExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAwaitExpr.allocationSize(value.v1))
            }
            is RsExpr.BecomeExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSBecomeExpr.allocationSize(value.v1))
            }
            is RsExpr.BinExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSBinExpr.allocationSize(value.v1))
            }
            is RsExpr.BlockExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSBlockExpr.allocationSize(value.v1))
            }
            is RsExpr.BreakExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSBreakExpr.allocationSize(value.v1))
            }
            is RsExpr.CallExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSCallExpr.allocationSize(value.v1))
            }
            is RsExpr.CastExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSCastExpr.allocationSize(value.v1))
            }
            is RsExpr.ClosureExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSClosureExpr.allocationSize(value.v1))
            }
            is RsExpr.ContinueExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSContinueExpr.allocationSize(value.v1))
            }
            is RsExpr.FieldExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSFieldExpr.allocationSize(value.v1))
            }
            is RsExpr.ForExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSForExpr.allocationSize(value.v1))
            }
            is RsExpr.FormatArgsExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSFormatArgsExpr.allocationSize(value.v1))
            }
            is RsExpr.IfExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSIfExpr.allocationSize(value.v1))
            }
            is RsExpr.IndexExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSIndexExpr.allocationSize(value.v1))
            }
            is RsExpr.LetExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSLetExpr.allocationSize(value.v1))
            }
            is RsExpr.Literal -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSLiteral.allocationSize(value.v1))
            }
            is RsExpr.LoopExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSLoopExpr.allocationSize(value.v1))
            }
            is RsExpr.MacroExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMacroExpr.allocationSize(value.v1))
            }
            is RsExpr.MatchExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMatchExpr.allocationSize(value.v1))
            }
            is RsExpr.MatchArm -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMatchArm.allocationSize(value.v1))
            }
            is RsExpr.MethodCallExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMethodCallExpr.allocationSize(value.v1))
            }
            is RsExpr.OffsetOfExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSOffsetOfExpr.allocationSize(value.v1))
            }
            is RsExpr.ParenExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSParenExpr.allocationSize(value.v1))
            }
            is RsExpr.PathExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSPathExpr.allocationSize(value.v1))
            }
            is RsExpr.PrefixExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSPrefixExpr.allocationSize(value.v1))
            }
            is RsExpr.RangeExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRangeExpr.allocationSize(value.v1))
            }
            is RsExpr.RecordExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRecordExpr.allocationSize(value.v1))
            }
            is RsExpr.RefExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRefExpr.allocationSize(value.v1))
            }
            is RsExpr.ReturnExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSReturnExpr.allocationSize(value.v1))
            }
            is RsExpr.TryExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTryExpr.allocationSize(value.v1))
            }
            is RsExpr.TupleExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTupleExpr.allocationSize(value.v1))
            }
            is RsExpr.UnderscoreExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSUnderscoreExpr.allocationSize(value.v1))
            }
            is RsExpr.WhileExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSWhileExpr.allocationSize(value.v1))
            }
            is RsExpr.YeetExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSYeetExpr.allocationSize(value.v1))
            }
            is RsExpr.YieldExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSYieldExpr.allocationSize(value.v1))
            }
            is RsExpr.Path -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSPath.allocationSize(value.v1))
            }
            is RsExpr.PathSegment -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSPathSegment.allocationSize(value.v1))
            }
            is RsExpr.NameRef -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSNameRef.allocationSize(value.v1))
            }
            is RsExpr.RecordExprField -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRecordExprField.allocationSize(value.v1))
            }
        }

    override fun write(value: RsExpr, buf: ByteBuffer) {
        when (value) {
            is RsExpr.ArrayExpr -> {
                buf.putInt(1)
                FfiConverterTypeRSArrayExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.AsmExpr -> {
                buf.putInt(2)
                FfiConverterTypeRSAsmExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.AwaitExpr -> {
                buf.putInt(3)
                FfiConverterTypeRSAwaitExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.BecomeExpr -> {
                buf.putInt(4)
                FfiConverterTypeRSBecomeExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.BinExpr -> {
                buf.putInt(5)
                FfiConverterTypeRSBinExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.BlockExpr -> {
                buf.putInt(6)
                FfiConverterTypeRSBlockExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.BreakExpr -> {
                buf.putInt(7)
                FfiConverterTypeRSBreakExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.CallExpr -> {
                buf.putInt(8)
                FfiConverterTypeRSCallExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.CastExpr -> {
                buf.putInt(9)
                FfiConverterTypeRSCastExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.ClosureExpr -> {
                buf.putInt(10)
                FfiConverterTypeRSClosureExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.ContinueExpr -> {
                buf.putInt(11)
                FfiConverterTypeRSContinueExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.FieldExpr -> {
                buf.putInt(12)
                FfiConverterTypeRSFieldExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.ForExpr -> {
                buf.putInt(13)
                FfiConverterTypeRSForExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.FormatArgsExpr -> {
                buf.putInt(14)
                FfiConverterTypeRSFormatArgsExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.IfExpr -> {
                buf.putInt(15)
                FfiConverterTypeRSIfExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.IndexExpr -> {
                buf.putInt(16)
                FfiConverterTypeRSIndexExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.LetExpr -> {
                buf.putInt(17)
                FfiConverterTypeRSLetExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.Literal -> {
                buf.putInt(18)
                FfiConverterTypeRSLiteral.write(value.v1, buf)
                Unit
            }
            is RsExpr.LoopExpr -> {
                buf.putInt(19)
                FfiConverterTypeRSLoopExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.MacroExpr -> {
                buf.putInt(20)
                FfiConverterTypeRSMacroExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.MatchExpr -> {
                buf.putInt(21)
                FfiConverterTypeRSMatchExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.MatchArm -> {
                buf.putInt(22)
                FfiConverterTypeRSMatchArm.write(value.v1, buf)
                Unit
            }
            is RsExpr.MethodCallExpr -> {
                buf.putInt(23)
                FfiConverterTypeRSMethodCallExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.OffsetOfExpr -> {
                buf.putInt(24)
                FfiConverterTypeRSOffsetOfExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.ParenExpr -> {
                buf.putInt(25)
                FfiConverterTypeRSParenExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.PathExpr -> {
                buf.putInt(26)
                FfiConverterTypeRSPathExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.PrefixExpr -> {
                buf.putInt(27)
                FfiConverterTypeRSPrefixExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.RangeExpr -> {
                buf.putInt(28)
                FfiConverterTypeRSRangeExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.RecordExpr -> {
                buf.putInt(29)
                FfiConverterTypeRSRecordExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.RefExpr -> {
                buf.putInt(30)
                FfiConverterTypeRSRefExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.ReturnExpr -> {
                buf.putInt(31)
                FfiConverterTypeRSReturnExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.TryExpr -> {
                buf.putInt(32)
                FfiConverterTypeRSTryExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.TupleExpr -> {
                buf.putInt(33)
                FfiConverterTypeRSTupleExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.UnderscoreExpr -> {
                buf.putInt(34)
                FfiConverterTypeRSUnderscoreExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.WhileExpr -> {
                buf.putInt(35)
                FfiConverterTypeRSWhileExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.YeetExpr -> {
                buf.putInt(36)
                FfiConverterTypeRSYeetExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.YieldExpr -> {
                buf.putInt(37)
                FfiConverterTypeRSYieldExpr.write(value.v1, buf)
                Unit
            }
            is RsExpr.Path -> {
                buf.putInt(38)
                FfiConverterTypeRSPath.write(value.v1, buf)
                Unit
            }
            is RsExpr.PathSegment -> {
                buf.putInt(39)
                FfiConverterTypeRSPathSegment.write(value.v1, buf)
                Unit
            }
            is RsExpr.NameRef -> {
                buf.putInt(40)
                FfiConverterTypeRSNameRef.write(value.v1, buf)
                Unit
            }
            is RsExpr.RecordExprField -> {
                buf.putInt(41)
                FfiConverterTypeRSRecordExprField.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsExternItem {

    data class Fn(val v1: RsFn) : RsExternItem() {

        companion object
    }

    data class MacroCall(val v1: RsMacroCall) : RsExternItem() {

        companion object
    }

    data class Static(val v1: RsStatic) : RsExternItem() {

        companion object
    }

    data class TypeAlias(val v1: RsTypeAlias) : RsExternItem() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSExternItem : FfiConverterRustBuffer<RsExternItem> {
    override fun read(buf: ByteBuffer): RsExternItem {
        return when (buf.getInt()) {
            1 -> RsExternItem.Fn(FfiConverterTypeRSFn.read(buf))
            2 -> RsExternItem.MacroCall(FfiConverterTypeRSMacroCall.read(buf))
            3 -> RsExternItem.Static(FfiConverterTypeRSStatic.read(buf))
            4 -> RsExternItem.TypeAlias(FfiConverterTypeRSTypeAlias.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsExternItem) =
        when (value) {
            is RsExternItem.Fn -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSFn.allocationSize(value.v1))
            }
            is RsExternItem.MacroCall -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMacroCall.allocationSize(value.v1))
            }
            is RsExternItem.Static -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSStatic.allocationSize(value.v1))
            }
            is RsExternItem.TypeAlias -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTypeAlias.allocationSize(value.v1))
            }
        }

    override fun write(value: RsExternItem, buf: ByteBuffer) {
        when (value) {
            is RsExternItem.Fn -> {
                buf.putInt(1)
                FfiConverterTypeRSFn.write(value.v1, buf)
                Unit
            }
            is RsExternItem.MacroCall -> {
                buf.putInt(2)
                FfiConverterTypeRSMacroCall.write(value.v1, buf)
                Unit
            }
            is RsExternItem.Static -> {
                buf.putInt(3)
                FfiConverterTypeRSStatic.write(value.v1, buf)
                Unit
            }
            is RsExternItem.TypeAlias -> {
                buf.putInt(4)
                FfiConverterTypeRSTypeAlias.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsFieldList {

    data class RecordFieldList(val v1: RsRecordFieldList) : RsFieldList() {

        companion object
    }

    data class TupleFieldList(val v1: RsTupleFieldList) : RsFieldList() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSFieldList : FfiConverterRustBuffer<RsFieldList> {
    override fun read(buf: ByteBuffer): RsFieldList {
        return when (buf.getInt()) {
            1 -> RsFieldList.RecordFieldList(FfiConverterTypeRSRecordFieldList.read(buf))
            2 -> RsFieldList.TupleFieldList(FfiConverterTypeRSTupleFieldList.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsFieldList) =
        when (value) {
            is RsFieldList.RecordFieldList -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRecordFieldList.allocationSize(value.v1))
            }
            is RsFieldList.TupleFieldList -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTupleFieldList.allocationSize(value.v1))
            }
        }

    override fun write(value: RsFieldList, buf: ByteBuffer) {
        when (value) {
            is RsFieldList.RecordFieldList -> {
                buf.putInt(1)
                FfiConverterTypeRSRecordFieldList.write(value.v1, buf)
                Unit
            }
            is RsFieldList.TupleFieldList -> {
                buf.putInt(2)
                FfiConverterTypeRSTupleFieldList.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsGenericArg {

    data class AssocTypeArg(val v1: RsAssocTypeArg) : RsGenericArg() {

        companion object
    }

    data class ConstArg(val v1: RsConstArg) : RsGenericArg() {

        companion object
    }

    data class LifetimeArg(val v1: RsLifetimeArg) : RsGenericArg() {

        companion object
    }

    data class TypeArg(val v1: RsTypeArg) : RsGenericArg() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSGenericArg : FfiConverterRustBuffer<RsGenericArg> {
    override fun read(buf: ByteBuffer): RsGenericArg {
        return when (buf.getInt()) {
            1 -> RsGenericArg.AssocTypeArg(FfiConverterTypeRSAssocTypeArg.read(buf))
            2 -> RsGenericArg.ConstArg(FfiConverterTypeRSConstArg.read(buf))
            3 -> RsGenericArg.LifetimeArg(FfiConverterTypeRSLifetimeArg.read(buf))
            4 -> RsGenericArg.TypeArg(FfiConverterTypeRSTypeArg.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsGenericArg) =
        when (value) {
            is RsGenericArg.AssocTypeArg -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAssocTypeArg.allocationSize(value.v1))
            }
            is RsGenericArg.ConstArg -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSConstArg.allocationSize(value.v1))
            }
            is RsGenericArg.LifetimeArg -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSLifetimeArg.allocationSize(value.v1))
            }
            is RsGenericArg.TypeArg -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTypeArg.allocationSize(value.v1))
            }
        }

    override fun write(value: RsGenericArg, buf: ByteBuffer) {
        when (value) {
            is RsGenericArg.AssocTypeArg -> {
                buf.putInt(1)
                FfiConverterTypeRSAssocTypeArg.write(value.v1, buf)
                Unit
            }
            is RsGenericArg.ConstArg -> {
                buf.putInt(2)
                FfiConverterTypeRSConstArg.write(value.v1, buf)
                Unit
            }
            is RsGenericArg.LifetimeArg -> {
                buf.putInt(3)
                FfiConverterTypeRSLifetimeArg.write(value.v1, buf)
                Unit
            }
            is RsGenericArg.TypeArg -> {
                buf.putInt(4)
                FfiConverterTypeRSTypeArg.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsGenericParam {

    data class ConstParam(val v1: RsConstParam) : RsGenericParam() {

        companion object
    }

    data class LifetimeParam(val v1: RsLifetimeParam) : RsGenericParam() {

        companion object
    }

    data class TypeParam(val v1: RsTypeParam) : RsGenericParam() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSGenericParam : FfiConverterRustBuffer<RsGenericParam> {
    override fun read(buf: ByteBuffer): RsGenericParam {
        return when (buf.getInt()) {
            1 -> RsGenericParam.ConstParam(FfiConverterTypeRSConstParam.read(buf))
            2 -> RsGenericParam.LifetimeParam(FfiConverterTypeRSLifetimeParam.read(buf))
            3 -> RsGenericParam.TypeParam(FfiConverterTypeRSTypeParam.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsGenericParam) =
        when (value) {
            is RsGenericParam.ConstParam -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSConstParam.allocationSize(value.v1))
            }
            is RsGenericParam.LifetimeParam -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSLifetimeParam.allocationSize(value.v1))
            }
            is RsGenericParam.TypeParam -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTypeParam.allocationSize(value.v1))
            }
        }

    override fun write(value: RsGenericParam, buf: ByteBuffer) {
        when (value) {
            is RsGenericParam.ConstParam -> {
                buf.putInt(1)
                FfiConverterTypeRSConstParam.write(value.v1, buf)
                Unit
            }
            is RsGenericParam.LifetimeParam -> {
                buf.putInt(2)
                FfiConverterTypeRSLifetimeParam.write(value.v1, buf)
                Unit
            }
            is RsGenericParam.TypeParam -> {
                buf.putInt(3)
                FfiConverterTypeRSTypeParam.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsItem {

    data class AsmExpr(val v1: RsAsmExpr) : RsItem() {

        companion object
    }

    data class Const(val v1: RsConst) : RsItem() {

        companion object
    }

    data class Enum(val v1: RsEnum) : RsItem() {

        companion object
    }

    data class ExternBlock(val v1: RsExternBlock) : RsItem() {

        companion object
    }

    data class ExternCrate(val v1: RsExternCrate) : RsItem() {

        companion object
    }

    data class Fn(val v1: RsFn) : RsItem() {

        companion object
    }

    data class Impl(val v1: RsImpl) : RsItem() {

        companion object
    }

    data class MacroCall(val v1: RsMacroCall) : RsItem() {

        companion object
    }

    data class MacroDef(val v1: RsMacroDef) : RsItem() {

        companion object
    }

    data class MacroRules(val v1: RsMacroRules) : RsItem() {

        companion object
    }

    data class Module(val v1: RsModule) : RsItem() {

        companion object
    }

    data class Static(val v1: RsStatic) : RsItem() {

        companion object
    }

    data class Struct(val v1: RsStruct) : RsItem() {

        companion object
    }

    data class Trait(val v1: RsTrait) : RsItem() {

        companion object
    }

    data class TypeAlias(val v1: RsTypeAlias) : RsItem() {

        companion object
    }

    data class Union(val v1: RsUnion) : RsItem() {

        companion object
    }

    data class Use(val v1: RsUse) : RsItem() {

        companion object
    }

    data class Param(val v1: RsParam) : RsItem() {

        companion object
    }

    data class SelfParam(val v1: RsSelfParam) : RsItem() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSItem : FfiConverterRustBuffer<RsItem> {
    override fun read(buf: ByteBuffer): RsItem {
        return when (buf.getInt()) {
            1 -> RsItem.AsmExpr(FfiConverterTypeRSAsmExpr.read(buf))
            2 -> RsItem.Const(FfiConverterTypeRSConst.read(buf))
            3 -> RsItem.Enum(FfiConverterTypeRSEnum.read(buf))
            4 -> RsItem.ExternBlock(FfiConverterTypeRSExternBlock.read(buf))
            5 -> RsItem.ExternCrate(FfiConverterTypeRSExternCrate.read(buf))
            6 -> RsItem.Fn(FfiConverterTypeRSFn.read(buf))
            7 -> RsItem.Impl(FfiConverterTypeRSImpl.read(buf))
            8 -> RsItem.MacroCall(FfiConverterTypeRSMacroCall.read(buf))
            9 -> RsItem.MacroDef(FfiConverterTypeRSMacroDef.read(buf))
            10 -> RsItem.MacroRules(FfiConverterTypeRSMacroRules.read(buf))
            11 -> RsItem.Module(FfiConverterTypeRSModule.read(buf))
            12 -> RsItem.Static(FfiConverterTypeRSStatic.read(buf))
            13 -> RsItem.Struct(FfiConverterTypeRSStruct.read(buf))
            14 -> RsItem.Trait(FfiConverterTypeRSTrait.read(buf))
            15 -> RsItem.TypeAlias(FfiConverterTypeRSTypeAlias.read(buf))
            16 -> RsItem.Union(FfiConverterTypeRSUnion.read(buf))
            17 -> RsItem.Use(FfiConverterTypeRSUse.read(buf))
            18 -> RsItem.Param(FfiConverterTypeRSParam.read(buf))
            19 -> RsItem.SelfParam(FfiConverterTypeRSSelfParam.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsItem) =
        when (value) {
            is RsItem.AsmExpr -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSAsmExpr.allocationSize(value.v1))
            }
            is RsItem.Const -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSConst.allocationSize(value.v1))
            }
            is RsItem.Enum -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSEnum.allocationSize(value.v1))
            }
            is RsItem.ExternBlock -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSExternBlock.allocationSize(value.v1))
            }
            is RsItem.ExternCrate -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSExternCrate.allocationSize(value.v1))
            }
            is RsItem.Fn -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSFn.allocationSize(value.v1))
            }
            is RsItem.Impl -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSImpl.allocationSize(value.v1))
            }
            is RsItem.MacroCall -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMacroCall.allocationSize(value.v1))
            }
            is RsItem.MacroDef -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMacroDef.allocationSize(value.v1))
            }
            is RsItem.MacroRules -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMacroRules.allocationSize(value.v1))
            }
            is RsItem.Module -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSModule.allocationSize(value.v1))
            }
            is RsItem.Static -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSStatic.allocationSize(value.v1))
            }
            is RsItem.Struct -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSStruct.allocationSize(value.v1))
            }
            is RsItem.Trait -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTrait.allocationSize(value.v1))
            }
            is RsItem.TypeAlias -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTypeAlias.allocationSize(value.v1))
            }
            is RsItem.Union -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSUnion.allocationSize(value.v1))
            }
            is RsItem.Use -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSUse.allocationSize(value.v1))
            }
            is RsItem.Param -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSParam.allocationSize(value.v1))
            }
            is RsItem.SelfParam -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSSelfParam.allocationSize(value.v1))
            }
        }

    override fun write(value: RsItem, buf: ByteBuffer) {
        when (value) {
            is RsItem.AsmExpr -> {
                buf.putInt(1)
                FfiConverterTypeRSAsmExpr.write(value.v1, buf)
                Unit
            }
            is RsItem.Const -> {
                buf.putInt(2)
                FfiConverterTypeRSConst.write(value.v1, buf)
                Unit
            }
            is RsItem.Enum -> {
                buf.putInt(3)
                FfiConverterTypeRSEnum.write(value.v1, buf)
                Unit
            }
            is RsItem.ExternBlock -> {
                buf.putInt(4)
                FfiConverterTypeRSExternBlock.write(value.v1, buf)
                Unit
            }
            is RsItem.ExternCrate -> {
                buf.putInt(5)
                FfiConverterTypeRSExternCrate.write(value.v1, buf)
                Unit
            }
            is RsItem.Fn -> {
                buf.putInt(6)
                FfiConverterTypeRSFn.write(value.v1, buf)
                Unit
            }
            is RsItem.Impl -> {
                buf.putInt(7)
                FfiConverterTypeRSImpl.write(value.v1, buf)
                Unit
            }
            is RsItem.MacroCall -> {
                buf.putInt(8)
                FfiConverterTypeRSMacroCall.write(value.v1, buf)
                Unit
            }
            is RsItem.MacroDef -> {
                buf.putInt(9)
                FfiConverterTypeRSMacroDef.write(value.v1, buf)
                Unit
            }
            is RsItem.MacroRules -> {
                buf.putInt(10)
                FfiConverterTypeRSMacroRules.write(value.v1, buf)
                Unit
            }
            is RsItem.Module -> {
                buf.putInt(11)
                FfiConverterTypeRSModule.write(value.v1, buf)
                Unit
            }
            is RsItem.Static -> {
                buf.putInt(12)
                FfiConverterTypeRSStatic.write(value.v1, buf)
                Unit
            }
            is RsItem.Struct -> {
                buf.putInt(13)
                FfiConverterTypeRSStruct.write(value.v1, buf)
                Unit
            }
            is RsItem.Trait -> {
                buf.putInt(14)
                FfiConverterTypeRSTrait.write(value.v1, buf)
                Unit
            }
            is RsItem.TypeAlias -> {
                buf.putInt(15)
                FfiConverterTypeRSTypeAlias.write(value.v1, buf)
                Unit
            }
            is RsItem.Union -> {
                buf.putInt(16)
                FfiConverterTypeRSUnion.write(value.v1, buf)
                Unit
            }
            is RsItem.Use -> {
                buf.putInt(17)
                FfiConverterTypeRSUse.write(value.v1, buf)
                Unit
            }
            is RsItem.Param -> {
                buf.putInt(18)
                FfiConverterTypeRSParam.write(value.v1, buf)
                Unit
            }
            is RsItem.SelfParam -> {
                buf.putInt(19)
                FfiConverterTypeRSSelfParam.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

enum class RsLiteralType {

    BYTE_L,
    BYTE_STRING_L,
    CHAR_L,
    C_STRING_L,
    FLOAT_NUMBER_L,
    INT_NUMBER_L,
    STRING_L,
    UNKNOWN_L;

    companion object
}

/** @suppress */
public object FfiConverterTypeRSLiteralType : FfiConverterRustBuffer<RsLiteralType> {
    override fun read(buf: ByteBuffer) =
        try {
            RsLiteralType.values()[buf.getInt() - 1]
        } catch (e: IndexOutOfBoundsException) {
            throw RuntimeException("invalid enum value, something is very wrong!!", e)
        }

    override fun allocationSize(value: RsLiteralType) = 4UL

    override fun write(value: RsLiteralType, buf: ByteBuffer) {
        buf.putInt(value.ordinal + 1)
    }
}

sealed class RsPat {

    data class BoxPat(val v1: RsBoxPat) : RsPat() {

        companion object
    }

    data class ConstBlockPat(val v1: RsConstBlockPat) : RsPat() {

        companion object
    }

    data class IdentPat(val v1: RsIdentPat) : RsPat() {

        companion object
    }

    data class LiteralPat(val v1: RsLiteralPat) : RsPat() {

        companion object
    }

    data class MacroPat(val v1: RsMacroPat) : RsPat() {

        companion object
    }

    data class OrPat(val v1: RsOrPat) : RsPat() {

        companion object
    }

    data class ParenPat(val v1: RsParenPat) : RsPat() {

        companion object
    }

    data class PathPat(val v1: RsPathPat) : RsPat() {

        companion object
    }

    data class RangePat(val v1: RsRangePat) : RsPat() {

        companion object
    }

    data class RecordPat(val v1: RsRecordPat) : RsPat() {

        companion object
    }

    data class RefPat(val v1: RsRefPat) : RsPat() {

        companion object
    }

    data class RestPat(val v1: RsRestPat) : RsPat() {

        companion object
    }

    data class SlicePat(val v1: RsSlicePat) : RsPat() {

        companion object
    }

    data class TuplePat(val v1: RsTuplePat) : RsPat() {

        companion object
    }

    data class TupleStructPat(val v1: RsTupleStructPat) : RsPat() {

        companion object
    }

    data class WildcardPat(val v1: RsWildcardPat) : RsPat() {

        companion object
    }

    data class RecordPatField(val v1: RsRecordPatField) : RsPat() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSPat : FfiConverterRustBuffer<RsPat> {
    override fun read(buf: ByteBuffer): RsPat {
        return when (buf.getInt()) {
            1 -> RsPat.BoxPat(FfiConverterTypeRSBoxPat.read(buf))
            2 -> RsPat.ConstBlockPat(FfiConverterTypeRSConstBlockPat.read(buf))
            3 -> RsPat.IdentPat(FfiConverterTypeRSIdentPat.read(buf))
            4 -> RsPat.LiteralPat(FfiConverterTypeRSLiteralPat.read(buf))
            5 -> RsPat.MacroPat(FfiConverterTypeRSMacroPat.read(buf))
            6 -> RsPat.OrPat(FfiConverterTypeRSOrPat.read(buf))
            7 -> RsPat.ParenPat(FfiConverterTypeRSParenPat.read(buf))
            8 -> RsPat.PathPat(FfiConverterTypeRSPathPat.read(buf))
            9 -> RsPat.RangePat(FfiConverterTypeRSRangePat.read(buf))
            10 -> RsPat.RecordPat(FfiConverterTypeRSRecordPat.read(buf))
            11 -> RsPat.RefPat(FfiConverterTypeRSRefPat.read(buf))
            12 -> RsPat.RestPat(FfiConverterTypeRSRestPat.read(buf))
            13 -> RsPat.SlicePat(FfiConverterTypeRSSlicePat.read(buf))
            14 -> RsPat.TuplePat(FfiConverterTypeRSTuplePat.read(buf))
            15 -> RsPat.TupleStructPat(FfiConverterTypeRSTupleStructPat.read(buf))
            16 -> RsPat.WildcardPat(FfiConverterTypeRSWildcardPat.read(buf))
            17 -> RsPat.RecordPatField(FfiConverterTypeRSRecordPatField.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsPat) =
        when (value) {
            is RsPat.BoxPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSBoxPat.allocationSize(value.v1))
            }
            is RsPat.ConstBlockPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSConstBlockPat.allocationSize(value.v1))
            }
            is RsPat.IdentPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSIdentPat.allocationSize(value.v1))
            }
            is RsPat.LiteralPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSLiteralPat.allocationSize(value.v1))
            }
            is RsPat.MacroPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMacroPat.allocationSize(value.v1))
            }
            is RsPat.OrPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSOrPat.allocationSize(value.v1))
            }
            is RsPat.ParenPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSParenPat.allocationSize(value.v1))
            }
            is RsPat.PathPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSPathPat.allocationSize(value.v1))
            }
            is RsPat.RangePat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRangePat.allocationSize(value.v1))
            }
            is RsPat.RecordPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRecordPat.allocationSize(value.v1))
            }
            is RsPat.RefPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRefPat.allocationSize(value.v1))
            }
            is RsPat.RestPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRestPat.allocationSize(value.v1))
            }
            is RsPat.SlicePat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSSlicePat.allocationSize(value.v1))
            }
            is RsPat.TuplePat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTuplePat.allocationSize(value.v1))
            }
            is RsPat.TupleStructPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTupleStructPat.allocationSize(value.v1))
            }
            is RsPat.WildcardPat -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSWildcardPat.allocationSize(value.v1))
            }
            is RsPat.RecordPatField -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRecordPatField.allocationSize(value.v1))
            }
        }

    override fun write(value: RsPat, buf: ByteBuffer) {
        when (value) {
            is RsPat.BoxPat -> {
                buf.putInt(1)
                FfiConverterTypeRSBoxPat.write(value.v1, buf)
                Unit
            }
            is RsPat.ConstBlockPat -> {
                buf.putInt(2)
                FfiConverterTypeRSConstBlockPat.write(value.v1, buf)
                Unit
            }
            is RsPat.IdentPat -> {
                buf.putInt(3)
                FfiConverterTypeRSIdentPat.write(value.v1, buf)
                Unit
            }
            is RsPat.LiteralPat -> {
                buf.putInt(4)
                FfiConverterTypeRSLiteralPat.write(value.v1, buf)
                Unit
            }
            is RsPat.MacroPat -> {
                buf.putInt(5)
                FfiConverterTypeRSMacroPat.write(value.v1, buf)
                Unit
            }
            is RsPat.OrPat -> {
                buf.putInt(6)
                FfiConverterTypeRSOrPat.write(value.v1, buf)
                Unit
            }
            is RsPat.ParenPat -> {
                buf.putInt(7)
                FfiConverterTypeRSParenPat.write(value.v1, buf)
                Unit
            }
            is RsPat.PathPat -> {
                buf.putInt(8)
                FfiConverterTypeRSPathPat.write(value.v1, buf)
                Unit
            }
            is RsPat.RangePat -> {
                buf.putInt(9)
                FfiConverterTypeRSRangePat.write(value.v1, buf)
                Unit
            }
            is RsPat.RecordPat -> {
                buf.putInt(10)
                FfiConverterTypeRSRecordPat.write(value.v1, buf)
                Unit
            }
            is RsPat.RefPat -> {
                buf.putInt(11)
                FfiConverterTypeRSRefPat.write(value.v1, buf)
                Unit
            }
            is RsPat.RestPat -> {
                buf.putInt(12)
                FfiConverterTypeRSRestPat.write(value.v1, buf)
                Unit
            }
            is RsPat.SlicePat -> {
                buf.putInt(13)
                FfiConverterTypeRSSlicePat.write(value.v1, buf)
                Unit
            }
            is RsPat.TuplePat -> {
                buf.putInt(14)
                FfiConverterTypeRSTuplePat.write(value.v1, buf)
                Unit
            }
            is RsPat.TupleStructPat -> {
                buf.putInt(15)
                FfiConverterTypeRSTupleStructPat.write(value.v1, buf)
                Unit
            }
            is RsPat.WildcardPat -> {
                buf.putInt(16)
                FfiConverterTypeRSWildcardPat.write(value.v1, buf)
                Unit
            }
            is RsPat.RecordPatField -> {
                buf.putInt(17)
                FfiConverterTypeRSRecordPatField.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsStmt {

    data class ExprStmt(val v1: RsExprStmt) : RsStmt() {

        companion object
    }

    data class Item(val v1: RsItem) : RsStmt() {

        companion object
    }

    data class LetStmt(val v1: RsLetStmt) : RsStmt() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSStmt : FfiConverterRustBuffer<RsStmt> {
    override fun read(buf: ByteBuffer): RsStmt {
        return when (buf.getInt()) {
            1 -> RsStmt.ExprStmt(FfiConverterTypeRSExprStmt.read(buf))
            2 -> RsStmt.Item(FfiConverterTypeRSItem.read(buf))
            3 -> RsStmt.LetStmt(FfiConverterTypeRSLetStmt.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsStmt) =
        when (value) {
            is RsStmt.ExprStmt -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSExprStmt.allocationSize(value.v1))
            }
            is RsStmt.Item -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSItem.allocationSize(value.v1))
            }
            is RsStmt.LetStmt -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSLetStmt.allocationSize(value.v1))
            }
        }

    override fun write(value: RsStmt, buf: ByteBuffer) {
        when (value) {
            is RsStmt.ExprStmt -> {
                buf.putInt(1)
                FfiConverterTypeRSExprStmt.write(value.v1, buf)
                Unit
            }
            is RsStmt.Item -> {
                buf.putInt(2)
                FfiConverterTypeRSItem.write(value.v1, buf)
                Unit
            }
            is RsStmt.LetStmt -> {
                buf.putInt(3)
                FfiConverterTypeRSLetStmt.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsType {

    data class ArrayType(val v1: RsArrayType) : RsType() {

        companion object
    }

    data class DynTraitType(val v1: RsDynTraitType) : RsType() {

        companion object
    }

    data class FnPtrType(val v1: RsFnPtrType) : RsType() {

        companion object
    }

    data class ForType(val v1: RsForType) : RsType() {

        companion object
    }

    data class ImplTraitType(val v1: RsImplTraitType) : RsType() {

        companion object
    }

    data class InferType(val v1: RsInferType) : RsType() {

        companion object
    }

    data class MacroType(val v1: RsMacroType) : RsType() {

        companion object
    }

    data class NeverType(val v1: RsNeverType) : RsType() {

        companion object
    }

    data class ParenType(val v1: RsParenType) : RsType() {

        companion object
    }

    data class PathType(val v1: RsPathType) : RsType() {

        companion object
    }

    data class PtrType(val v1: RsPtrType) : RsType() {

        companion object
    }

    data class RefType(val v1: RsRefType) : RsType() {

        companion object
    }

    data class SliceType(val v1: RsSliceType) : RsType() {

        companion object
    }

    data class TupleType(val v1: RsTupleType) : RsType() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSType : FfiConverterRustBuffer<RsType> {
    override fun read(buf: ByteBuffer): RsType {
        return when (buf.getInt()) {
            1 -> RsType.ArrayType(FfiConverterTypeRSArrayType.read(buf))
            2 -> RsType.DynTraitType(FfiConverterTypeRSDynTraitType.read(buf))
            3 -> RsType.FnPtrType(FfiConverterTypeRSFnPtrType.read(buf))
            4 -> RsType.ForType(FfiConverterTypeRSForType.read(buf))
            5 -> RsType.ImplTraitType(FfiConverterTypeRSImplTraitType.read(buf))
            6 -> RsType.InferType(FfiConverterTypeRSInferType.read(buf))
            7 -> RsType.MacroType(FfiConverterTypeRSMacroType.read(buf))
            8 -> RsType.NeverType(FfiConverterTypeRSNeverType.read(buf))
            9 -> RsType.ParenType(FfiConverterTypeRSParenType.read(buf))
            10 -> RsType.PathType(FfiConverterTypeRSPathType.read(buf))
            11 -> RsType.PtrType(FfiConverterTypeRSPtrType.read(buf))
            12 -> RsType.RefType(FfiConverterTypeRSRefType.read(buf))
            13 -> RsType.SliceType(FfiConverterTypeRSSliceType.read(buf))
            14 -> RsType.TupleType(FfiConverterTypeRSTupleType.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsType) =
        when (value) {
            is RsType.ArrayType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSArrayType.allocationSize(value.v1))
            }
            is RsType.DynTraitType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSDynTraitType.allocationSize(value.v1))
            }
            is RsType.FnPtrType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSFnPtrType.allocationSize(value.v1))
            }
            is RsType.ForType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSForType.allocationSize(value.v1))
            }
            is RsType.ImplTraitType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSImplTraitType.allocationSize(value.v1))
            }
            is RsType.InferType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSInferType.allocationSize(value.v1))
            }
            is RsType.MacroType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSMacroType.allocationSize(value.v1))
            }
            is RsType.NeverType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSNeverType.allocationSize(value.v1))
            }
            is RsType.ParenType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSParenType.allocationSize(value.v1))
            }
            is RsType.PathType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSPathType.allocationSize(value.v1))
            }
            is RsType.PtrType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSPtrType.allocationSize(value.v1))
            }
            is RsType.RefType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSRefType.allocationSize(value.v1))
            }
            is RsType.SliceType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSSliceType.allocationSize(value.v1))
            }
            is RsType.TupleType -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSTupleType.allocationSize(value.v1))
            }
        }

    override fun write(value: RsType, buf: ByteBuffer) {
        when (value) {
            is RsType.ArrayType -> {
                buf.putInt(1)
                FfiConverterTypeRSArrayType.write(value.v1, buf)
                Unit
            }
            is RsType.DynTraitType -> {
                buf.putInt(2)
                FfiConverterTypeRSDynTraitType.write(value.v1, buf)
                Unit
            }
            is RsType.FnPtrType -> {
                buf.putInt(3)
                FfiConverterTypeRSFnPtrType.write(value.v1, buf)
                Unit
            }
            is RsType.ForType -> {
                buf.putInt(4)
                FfiConverterTypeRSForType.write(value.v1, buf)
                Unit
            }
            is RsType.ImplTraitType -> {
                buf.putInt(5)
                FfiConverterTypeRSImplTraitType.write(value.v1, buf)
                Unit
            }
            is RsType.InferType -> {
                buf.putInt(6)
                FfiConverterTypeRSInferType.write(value.v1, buf)
                Unit
            }
            is RsType.MacroType -> {
                buf.putInt(7)
                FfiConverterTypeRSMacroType.write(value.v1, buf)
                Unit
            }
            is RsType.NeverType -> {
                buf.putInt(8)
                FfiConverterTypeRSNeverType.write(value.v1, buf)
                Unit
            }
            is RsType.ParenType -> {
                buf.putInt(9)
                FfiConverterTypeRSParenType.write(value.v1, buf)
                Unit
            }
            is RsType.PathType -> {
                buf.putInt(10)
                FfiConverterTypeRSPathType.write(value.v1, buf)
                Unit
            }
            is RsType.PtrType -> {
                buf.putInt(11)
                FfiConverterTypeRSPtrType.write(value.v1, buf)
                Unit
            }
            is RsType.RefType -> {
                buf.putInt(12)
                FfiConverterTypeRSRefType.write(value.v1, buf)
                Unit
            }
            is RsType.SliceType -> {
                buf.putInt(13)
                FfiConverterTypeRSSliceType.write(value.v1, buf)
                Unit
            }
            is RsType.TupleType -> {
                buf.putInt(14)
                FfiConverterTypeRSTupleType.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsUseBoundGenericArg {

    data class Lifetime(val v1: RsLifetime) : RsUseBoundGenericArg() {

        companion object
    }

    data class NameRef(val v1: RsNameRef) : RsUseBoundGenericArg() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSUseBoundGenericArg : FfiConverterRustBuffer<RsUseBoundGenericArg> {
    override fun read(buf: ByteBuffer): RsUseBoundGenericArg {
        return when (buf.getInt()) {
            1 -> RsUseBoundGenericArg.Lifetime(FfiConverterTypeRSLifetime.read(buf))
            2 -> RsUseBoundGenericArg.NameRef(FfiConverterTypeRSNameRef.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsUseBoundGenericArg) =
        when (value) {
            is RsUseBoundGenericArg.Lifetime -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSLifetime.allocationSize(value.v1))
            }
            is RsUseBoundGenericArg.NameRef -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSNameRef.allocationSize(value.v1))
            }
        }

    override fun write(value: RsUseBoundGenericArg, buf: ByteBuffer) {
        when (value) {
            is RsUseBoundGenericArg.Lifetime -> {
                buf.putInt(1)
                FfiConverterTypeRSLifetime.write(value.v1, buf)
                Unit
            }
            is RsUseBoundGenericArg.NameRef -> {
                buf.putInt(2)
                FfiConverterTypeRSNameRef.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

sealed class RsVariantDef {

    data class Struct(val v1: RsStruct) : RsVariantDef() {

        companion object
    }

    data class Union(val v1: RsUnion) : RsVariantDef() {

        companion object
    }

    data class Variant(val v1: RsVariant) : RsVariantDef() {

        companion object
    }

    companion object
}

/** @suppress */
public object FfiConverterTypeRSVariantDef : FfiConverterRustBuffer<RsVariantDef> {
    override fun read(buf: ByteBuffer): RsVariantDef {
        return when (buf.getInt()) {
            1 -> RsVariantDef.Struct(FfiConverterTypeRSStruct.read(buf))
            2 -> RsVariantDef.Union(FfiConverterTypeRSUnion.read(buf))
            3 -> RsVariantDef.Variant(FfiConverterTypeRSVariant.read(buf))
            else -> throw RuntimeException("invalid enum value, something is very wrong!!")
        }
    }

    override fun allocationSize(value: RsVariantDef) =
        when (value) {
            is RsVariantDef.Struct -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSStruct.allocationSize(value.v1))
            }
            is RsVariantDef.Union -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSUnion.allocationSize(value.v1))
            }
            is RsVariantDef.Variant -> {
                // Add the size for the Int that specifies the variant plus the size needed for all
                // fields
                (4UL + FfiConverterTypeRSVariant.allocationSize(value.v1))
            }
        }

    override fun write(value: RsVariantDef, buf: ByteBuffer) {
        when (value) {
            is RsVariantDef.Struct -> {
                buf.putInt(1)
                FfiConverterTypeRSStruct.write(value.v1, buf)
                Unit
            }
            is RsVariantDef.Union -> {
                buf.putInt(2)
                FfiConverterTypeRSUnion.write(value.v1, buf)
                Unit
            }
            is RsVariantDef.Variant -> {
                buf.putInt(3)
                FfiConverterTypeRSVariant.write(value.v1, buf)
                Unit
            }
        }.let { /* this makes the `when` an expression, which ensures it is exhaustive */ }
    }
}

/** @suppress */
public object FfiConverterOptionalString : FfiConverterRustBuffer<kotlin.String?> {
    override fun read(buf: ByteBuffer): kotlin.String? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterString.read(buf)
    }

    override fun allocationSize(value: kotlin.String?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterString.allocationSize(value)
        }
    }

    override fun write(value: kotlin.String?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterString.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSAbi : FfiConverterRustBuffer<RsAbi?> {
    override fun read(buf: ByteBuffer): RsAbi? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSAbi.read(buf)
    }

    override fun allocationSize(value: RsAbi?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSAbi.allocationSize(value)
        }
    }

    override fun write(value: RsAbi?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSAbi.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSBlockExpr : FfiConverterRustBuffer<RsBlockExpr?> {
    override fun read(buf: ByteBuffer): RsBlockExpr? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSBlockExpr.read(buf)
    }

    override fun allocationSize(value: RsBlockExpr?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSBlockExpr.allocationSize(value)
        }
    }

    override fun write(value: RsBlockExpr?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSBlockExpr.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSConstArg : FfiConverterRustBuffer<RsConstArg?> {
    override fun read(buf: ByteBuffer): RsConstArg? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSConstArg.read(buf)
    }

    override fun allocationSize(value: RsConstArg?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSConstArg.allocationSize(value)
        }
    }

    override fun write(value: RsConstArg?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSConstArg.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSLifetime : FfiConverterRustBuffer<RsLifetime?> {
    override fun read(buf: ByteBuffer): RsLifetime? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSLifetime.read(buf)
    }

    override fun allocationSize(value: RsLifetime?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSLifetime.allocationSize(value)
        }
    }

    override fun write(value: RsLifetime?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSLifetime.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSLiteral : FfiConverterRustBuffer<RsLiteral?> {
    override fun read(buf: ByteBuffer): RsLiteral? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSLiteral.read(buf)
    }

    override fun allocationSize(value: RsLiteral?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSLiteral.allocationSize(value)
        }
    }

    override fun write(value: RsLiteral?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSLiteral.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSMacroCall : FfiConverterRustBuffer<RsMacroCall?> {
    override fun read(buf: ByteBuffer): RsMacroCall? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSMacroCall.read(buf)
    }

    override fun allocationSize(value: RsMacroCall?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSMacroCall.allocationSize(value)
        }
    }

    override fun write(value: RsMacroCall?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSMacroCall.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSNameRef : FfiConverterRustBuffer<RsNameRef?> {
    override fun read(buf: ByteBuffer): RsNameRef? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSNameRef.read(buf)
    }

    override fun allocationSize(value: RsNameRef?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSNameRef.allocationSize(value)
        }
    }

    override fun write(value: RsNameRef?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSNameRef.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSParamList : FfiConverterRustBuffer<RsParamList?> {
    override fun read(buf: ByteBuffer): RsParamList? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSParamList.read(buf)
    }

    override fun allocationSize(value: RsParamList?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSParamList.allocationSize(value)
        }
    }

    override fun write(value: RsParamList?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSParamList.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSPath : FfiConverterRustBuffer<RsPath?> {
    override fun read(buf: ByteBuffer): RsPath? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSPath.read(buf)
    }

    override fun allocationSize(value: RsPath?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSPath.allocationSize(value)
        }
    }

    override fun write(value: RsPath?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSPath.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSPathSegment : FfiConverterRustBuffer<RsPathSegment?> {
    override fun read(buf: ByteBuffer): RsPathSegment? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSPathSegment.read(buf)
    }

    override fun allocationSize(value: RsPathSegment?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSPathSegment.allocationSize(value)
        }
    }

    override fun write(value: RsPathSegment?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSPathSegment.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSRecordFieldList :
    FfiConverterRustBuffer<RsRecordFieldList?> {
    override fun read(buf: ByteBuffer): RsRecordFieldList? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSRecordFieldList.read(buf)
    }

    override fun allocationSize(value: RsRecordFieldList?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSRecordFieldList.allocationSize(value)
        }
    }

    override fun write(value: RsRecordFieldList?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSRecordFieldList.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSReturnTypeSyntax :
    FfiConverterRustBuffer<RsReturnTypeSyntax?> {
    override fun read(buf: ByteBuffer): RsReturnTypeSyntax? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSReturnTypeSyntax.read(buf)
    }

    override fun allocationSize(value: RsReturnTypeSyntax?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSReturnTypeSyntax.allocationSize(value)
        }
    }

    override fun write(value: RsReturnTypeSyntax?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSReturnTypeSyntax.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSSelfParam : FfiConverterRustBuffer<RsSelfParam?> {
    override fun read(buf: ByteBuffer): RsSelfParam? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSSelfParam.read(buf)
    }

    override fun allocationSize(value: RsSelfParam?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSSelfParam.allocationSize(value)
        }
    }

    override fun write(value: RsSelfParam?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSSelfParam.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSSourceFile : FfiConverterRustBuffer<RsSourceFile?> {
    override fun read(buf: ByteBuffer): RsSourceFile? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSSourceFile.read(buf)
    }

    override fun allocationSize(value: RsSourceFile?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSSourceFile.allocationSize(value)
        }
    }

    override fun write(value: RsSourceFile?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSSourceFile.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSTypeAnchor : FfiConverterRustBuffer<RsTypeAnchor?> {
    override fun read(buf: ByteBuffer): RsTypeAnchor? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSTypeAnchor.read(buf)
    }

    override fun allocationSize(value: RsTypeAnchor?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSTypeAnchor.allocationSize(value)
        }
    }

    override fun write(value: RsTypeAnchor?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSTypeAnchor.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSUseTree : FfiConverterRustBuffer<RsUseTree?> {
    override fun read(buf: ByteBuffer): RsUseTree? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSUseTree.read(buf)
    }

    override fun allocationSize(value: RsUseTree?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSUseTree.allocationSize(value)
        }
    }

    override fun write(value: RsUseTree?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSUseTree.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSExpr : FfiConverterRustBuffer<RsExpr?> {
    override fun read(buf: ByteBuffer): RsExpr? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSExpr.read(buf)
    }

    override fun allocationSize(value: RsExpr?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSExpr.allocationSize(value)
        }
    }

    override fun write(value: RsExpr?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSExpr.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSFieldList : FfiConverterRustBuffer<RsFieldList?> {
    override fun read(buf: ByteBuffer): RsFieldList? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSFieldList.read(buf)
    }

    override fun allocationSize(value: RsFieldList?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSFieldList.allocationSize(value)
        }
    }

    override fun write(value: RsFieldList?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSFieldList.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSPat : FfiConverterRustBuffer<RsPat?> {
    override fun read(buf: ByteBuffer): RsPat? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSPat.read(buf)
    }

    override fun allocationSize(value: RsPat?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSPat.allocationSize(value)
        }
    }

    override fun write(value: RsPat?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSPat.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterOptionalTypeRSType : FfiConverterRustBuffer<RsType?> {
    override fun read(buf: ByteBuffer): RsType? {
        if (buf.get().toInt() == 0) {
            return null
        }
        return FfiConverterTypeRSType.read(buf)
    }

    override fun allocationSize(value: RsType?): ULong {
        if (value == null) {
            return 1UL
        } else {
            return 1UL + FfiConverterTypeRSType.allocationSize(value)
        }
    }

    override fun write(value: RsType?, buf: ByteBuffer) {
        if (value == null) {
            buf.put(0)
        } else {
            buf.put(1)
            FfiConverterTypeRSType.write(value, buf)
        }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSBlockExpr : FfiConverterRustBuffer<List<RsBlockExpr>> {
    override fun read(buf: ByteBuffer): List<RsBlockExpr> {
        val len = buf.getInt()
        return List<RsBlockExpr>(len) { FfiConverterTypeRSBlockExpr.read(buf) }
    }

    override fun allocationSize(value: List<RsBlockExpr>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSBlockExpr.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsBlockExpr>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSBlockExpr.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSMatchArm : FfiConverterRustBuffer<List<RsMatchArm>> {
    override fun read(buf: ByteBuffer): List<RsMatchArm> {
        val len = buf.getInt()
        return List<RsMatchArm>(len) { FfiConverterTypeRSMatchArm.read(buf) }
    }

    override fun allocationSize(value: List<RsMatchArm>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSMatchArm.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsMatchArm>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSMatchArm.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSNameRef : FfiConverterRustBuffer<List<RsNameRef>> {
    override fun read(buf: ByteBuffer): List<RsNameRef> {
        val len = buf.getInt()
        return List<RsNameRef>(len) { FfiConverterTypeRSNameRef.read(buf) }
    }

    override fun allocationSize(value: List<RsNameRef>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSNameRef.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsNameRef>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSNameRef.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSParam : FfiConverterRustBuffer<List<RsParam>> {
    override fun read(buf: ByteBuffer): List<RsParam> {
        val len = buf.getInt()
        return List<RsParam>(len) { FfiConverterTypeRSParam.read(buf) }
    }

    override fun allocationSize(value: List<RsParam>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSParam.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsParam>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSParam.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSParamList : FfiConverterRustBuffer<List<RsParamList>> {
    override fun read(buf: ByteBuffer): List<RsParamList> {
        val len = buf.getInt()
        return List<RsParamList>(len) { FfiConverterTypeRSParamList.read(buf) }
    }

    override fun allocationSize(value: List<RsParamList>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSParamList.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsParamList>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSParamList.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSPath : FfiConverterRustBuffer<List<RsPath>> {
    override fun read(buf: ByteBuffer): List<RsPath> {
        val len = buf.getInt()
        return List<RsPath>(len) { FfiConverterTypeRSPath.read(buf) }
    }

    override fun allocationSize(value: List<RsPath>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSPath.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsPath>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSPath.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSPathType : FfiConverterRustBuffer<List<RsPathType>> {
    override fun read(buf: ByteBuffer): List<RsPathType> {
        val len = buf.getInt()
        return List<RsPathType>(len) { FfiConverterTypeRSPathType.read(buf) }
    }

    override fun allocationSize(value: List<RsPathType>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSPathType.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsPathType>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSPathType.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSRecordExprField :
    FfiConverterRustBuffer<List<RsRecordExprField>> {
    override fun read(buf: ByteBuffer): List<RsRecordExprField> {
        val len = buf.getInt()
        return List<RsRecordExprField>(len) { FfiConverterTypeRSRecordExprField.read(buf) }
    }

    override fun allocationSize(value: List<RsRecordExprField>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSRecordExprField.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsRecordExprField>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSRecordExprField.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSRecordField : FfiConverterRustBuffer<List<RsRecordField>> {
    override fun read(buf: ByteBuffer): List<RsRecordField> {
        val len = buf.getInt()
        return List<RsRecordField>(len) { FfiConverterTypeRSRecordField.read(buf) }
    }

    override fun allocationSize(value: List<RsRecordField>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSRecordField.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsRecordField>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSRecordField.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSRecordPatField :
    FfiConverterRustBuffer<List<RsRecordPatField>> {
    override fun read(buf: ByteBuffer): List<RsRecordPatField> {
        val len = buf.getInt()
        return List<RsRecordPatField>(len) { FfiConverterTypeRSRecordPatField.read(buf) }
    }

    override fun allocationSize(value: List<RsRecordPatField>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSRecordPatField.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsRecordPatField>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSRecordPatField.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSTupleField : FfiConverterRustBuffer<List<RsTupleField>> {
    override fun read(buf: ByteBuffer): List<RsTupleField> {
        val len = buf.getInt()
        return List<RsTupleField>(len) { FfiConverterTypeRSTupleField.read(buf) }
    }

    override fun allocationSize(value: List<RsTupleField>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSTupleField.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsTupleField>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSTupleField.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSTypeBound : FfiConverterRustBuffer<List<RsTypeBound>> {
    override fun read(buf: ByteBuffer): List<RsTypeBound> {
        val len = buf.getInt()
        return List<RsTypeBound>(len) { FfiConverterTypeRSTypeBound.read(buf) }
    }

    override fun allocationSize(value: List<RsTypeBound>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSTypeBound.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsTypeBound>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSTypeBound.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSUseTree : FfiConverterRustBuffer<List<RsUseTree>> {
    override fun read(buf: ByteBuffer): List<RsUseTree> {
        val len = buf.getInt()
        return List<RsUseTree>(len) { FfiConverterTypeRSUseTree.read(buf) }
    }

    override fun allocationSize(value: List<RsUseTree>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSUseTree.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsUseTree>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSUseTree.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSVariant : FfiConverterRustBuffer<List<RsVariant>> {
    override fun read(buf: ByteBuffer): List<RsVariant> {
        val len = buf.getInt()
        return List<RsVariant>(len) { FfiConverterTypeRSVariant.read(buf) }
    }

    override fun allocationSize(value: List<RsVariant>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSVariant.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsVariant>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSVariant.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSAssocItem : FfiConverterRustBuffer<List<RsAssocItem>> {
    override fun read(buf: ByteBuffer): List<RsAssocItem> {
        val len = buf.getInt()
        return List<RsAssocItem>(len) { FfiConverterTypeRSAssocItem.read(buf) }
    }

    override fun allocationSize(value: List<RsAssocItem>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSAssocItem.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsAssocItem>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSAssocItem.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSAst : FfiConverterRustBuffer<List<RsAst>> {
    override fun read(buf: ByteBuffer): List<RsAst> {
        val len = buf.getInt()
        return List<RsAst>(len) { FfiConverterTypeRSAst.read(buf) }
    }

    override fun allocationSize(value: List<RsAst>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSAst.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsAst>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSAst.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSExpr : FfiConverterRustBuffer<List<RsExpr>> {
    override fun read(buf: ByteBuffer): List<RsExpr> {
        val len = buf.getInt()
        return List<RsExpr>(len) { FfiConverterTypeRSExpr.read(buf) }
    }

    override fun allocationSize(value: List<RsExpr>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSExpr.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsExpr>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSExpr.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSExternItem : FfiConverterRustBuffer<List<RsExternItem>> {
    override fun read(buf: ByteBuffer): List<RsExternItem> {
        val len = buf.getInt()
        return List<RsExternItem>(len) { FfiConverterTypeRSExternItem.read(buf) }
    }

    override fun allocationSize(value: List<RsExternItem>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSExternItem.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsExternItem>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSExternItem.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSFieldList : FfiConverterRustBuffer<List<RsFieldList>> {
    override fun read(buf: ByteBuffer): List<RsFieldList> {
        val len = buf.getInt()
        return List<RsFieldList>(len) { FfiConverterTypeRSFieldList.read(buf) }
    }

    override fun allocationSize(value: List<RsFieldList>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSFieldList.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsFieldList>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSFieldList.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSGenericArg : FfiConverterRustBuffer<List<RsGenericArg>> {
    override fun read(buf: ByteBuffer): List<RsGenericArg> {
        val len = buf.getInt()
        return List<RsGenericArg>(len) { FfiConverterTypeRSGenericArg.read(buf) }
    }

    override fun allocationSize(value: List<RsGenericArg>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSGenericArg.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsGenericArg>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSGenericArg.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSGenericParam :
    FfiConverterRustBuffer<List<RsGenericParam>> {
    override fun read(buf: ByteBuffer): List<RsGenericParam> {
        val len = buf.getInt()
        return List<RsGenericParam>(len) { FfiConverterTypeRSGenericParam.read(buf) }
    }

    override fun allocationSize(value: List<RsGenericParam>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSGenericParam.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsGenericParam>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSGenericParam.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSItem : FfiConverterRustBuffer<List<RsItem>> {
    override fun read(buf: ByteBuffer): List<RsItem> {
        val len = buf.getInt()
        return List<RsItem>(len) { FfiConverterTypeRSItem.read(buf) }
    }

    override fun allocationSize(value: List<RsItem>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSItem.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsItem>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSItem.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSPat : FfiConverterRustBuffer<List<RsPat>> {
    override fun read(buf: ByteBuffer): List<RsPat> {
        val len = buf.getInt()
        return List<RsPat>(len) { FfiConverterTypeRSPat.read(buf) }
    }

    override fun allocationSize(value: List<RsPat>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSPat.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsPat>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSPat.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSStmt : FfiConverterRustBuffer<List<RsStmt>> {
    override fun read(buf: ByteBuffer): List<RsStmt> {
        val len = buf.getInt()
        return List<RsStmt>(len) { FfiConverterTypeRSStmt.read(buf) }
    }

    override fun allocationSize(value: List<RsStmt>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSStmt.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsStmt>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSStmt.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSType : FfiConverterRustBuffer<List<RsType>> {
    override fun read(buf: ByteBuffer): List<RsType> {
        val len = buf.getInt()
        return List<RsType>(len) { FfiConverterTypeRSType.read(buf) }
    }

    override fun allocationSize(value: List<RsType>): ULong {
        val sizeForLength = 4UL
        val sizeForItems = value.map { FfiConverterTypeRSType.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsType>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSType.write(it, buf) }
    }
}

/** @suppress */
public object FfiConverterSequenceTypeRSUseBoundGenericArg :
    FfiConverterRustBuffer<List<RsUseBoundGenericArg>> {
    override fun read(buf: ByteBuffer): List<RsUseBoundGenericArg> {
        val len = buf.getInt()
        return List<RsUseBoundGenericArg>(len) { FfiConverterTypeRSUseBoundGenericArg.read(buf) }
    }

    override fun allocationSize(value: List<RsUseBoundGenericArg>): ULong {
        val sizeForLength = 4UL
        val sizeForItems =
            value.map { FfiConverterTypeRSUseBoundGenericArg.allocationSize(it) }.sum()
        return sizeForLength + sizeForItems
    }

    override fun write(value: List<RsUseBoundGenericArg>, buf: ByteBuffer) {
        buf.putInt(value.size)
        value.iterator().forEach { FfiConverterTypeRSUseBoundGenericArg.write(it, buf) }
    }
}

fun `parseRustCode`(`source`: kotlin.String): RsSourceFile? {
    return FfiConverterOptionalTypeRSSourceFile.lift(
        uniffiRustCall() { _status ->
            UniffiLib.uniffi_rustast_fn_func_parse_rust_code(
                FfiConverterString.lower(`source`),
                _status,
            )
        }
    )
}
