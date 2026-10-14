void* load_secret(const char* name) {
    void* key = get_secret(name);
    return key;
}

void* encrypt_message(const void* plaintext, void* key) {
    void* cipher = new_cipher("AES-256-GCM");
    void* ciphertext = do_encrypt(cipher, plaintext, key);
    log_event(ciphertext);
    return ciphertext;
}

void persist_ciphertext(void* ciphertext) {
    write_output(ciphertext);
}
