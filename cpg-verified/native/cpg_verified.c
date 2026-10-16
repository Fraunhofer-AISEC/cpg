/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 *
 * C interface of the native cpg-verified library, called from the JVM via JNA. It initializes the
 * Lean runtime and converts between plain byte buffers and Lean's ByteArray.
 */
#include <lean/lean.h>

/* Functions of the C interface; everything else stays hidden */
#define CPG_EXPORT __attribute__((visibility("default")))

/* Lean's bundled compiler does not ship the system C headers on all platforms */
void *malloc(size_t size);
void free(void *ptr);
void *memcpy(void *dst, const void *src, size_t n);

/* Provided by the Lean runtime, but not declared in lean.h */
void lean_initialize_runtime_module(void);
void lean_initialize_thread(void);

extern lean_object *initialize_cpg_x2dverified_CpgVerified(uint8_t builtin);
extern lean_object *cpg_verified_translate(lean_object *input);

/* Initializes the Lean runtime and the CpgVerified module. Must be called once before anything
 * else. Returns 0 on success. */
CPG_EXPORT int cpg_verified_init(void) {
    lean_initialize_runtime_module();
    lean_object *res = initialize_cpg_x2dverified_CpgVerified(1 /* builtin */);
    lean_io_mark_end_initialization();
    if (!lean_io_result_is_ok(res)) {
        lean_io_result_show_error(res);
        lean_dec_ref(res);
        return 1;
    }
    lean_dec_ref(res);
    return 0;
}

/* Registers the calling thread with the Lean runtime. Must be called once on every thread (other
 * than the one that called cpg_verified_init) before it calls cpg_verified_translate. */
CPG_EXPORT void cpg_verified_init_thread(void) { lean_initialize_thread(); }

/* Translates the requests in input (see CpgVerified/Wire/Codec.lean). The result must be released
 * with cpg_verified_free; its length is stored in out_len. */
CPG_EXPORT uint8_t *cpg_verified_translate_bytes(const uint8_t *input, size_t len, size_t *out_len) {
    lean_object *in = lean_alloc_sarray(1, len, len);
    memcpy(lean_sarray_cptr(in), input, len);

    /* Takes ownership of in */
    lean_object *out = cpg_verified_translate(in);

    size_t n = lean_sarray_size(out);
    uint8_t *buffer = malloc(n > 0 ? n : 1);
    memcpy(buffer, lean_sarray_cptr(out), n);
    lean_dec(out);

    *out_len = n;
    return buffer;
}

CPG_EXPORT void cpg_verified_free(uint8_t *buffer) { free(buffer); }
