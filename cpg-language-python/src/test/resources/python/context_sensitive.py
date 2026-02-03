def cipher_operation(command, data, key):
    return platform_cipher_operation(command, data, key)

class EncryptionManager:
    @staticmethod
    def setup_new_key():
        key_id = create_new_key()
        key = retrieve_key_from_server(key_id)
        hex_key = to_hex(key)

        return (hex_key, key_id)

    def encryt_data(self, data):
        (key, key_id) = self.setup_new_key()

        if some_decision():
            very_good_key = "very_good" + key
            (res, err) = cipher_operation("something_else", data.payload, very_good_key)
            del key
            data.key_id = key_id
        else:
            (res, err) = cipher_operation("encrypt", data.payload, key)
            del key
            data.key_id = key_id
