pub fn load_secret(name: &str) -> Vec<u8> {
    let key = get_secret(name);
    return key;
}

pub fn encrypt_message(plaintext: &[u8], key: &[u8]) -> Vec<u8> {
    let cipher = new_cipher("AES-256-GCM");
    let ciphertext = do_encrypt(cipher, plaintext, key);
    log_event(&ciphertext);
    return ciphertext;
}

pub fn persist_ciphertext(ciphertext: &[u8]) {
    write_output(ciphertext);
}
