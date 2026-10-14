pub fn load_keypair(name: &str) -> Vec<u8> {
    let key = get_secret(name);
    return key;
}

pub fn encrypt_payload(plaintext: &[u8], key: &[u8]) -> Vec<u8> {
    let cipher = new_cipher("RSA-2048");
    let ciphertext = do_encrypt(cipher, plaintext, key);
    log_event(&ciphertext);
    return ciphertext;
}

pub fn archive_ciphertext(ciphertext: &[u8]) {
    write_output(ciphertext);
}
