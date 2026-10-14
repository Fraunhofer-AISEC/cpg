pub fn load_secret(name: &str) -> Vec<u8> {
    let key = get_secret(name);
    return key;
}

pub fn decrypt_message(ciphertext: &[u8], key: &[u8]) -> Vec<u8> {
    let cipher = new_cipher("AES-256-GCM");
    let plaintext = do_decrypt(cipher, ciphertext, key);
    log_event(&plaintext);
    return plaintext;
}

pub fn persist_plaintext(plaintext: &[u8]) {
    write_output(plaintext);
}
