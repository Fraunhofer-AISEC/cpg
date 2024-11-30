namespace some {
    class json {
public:
        class iterator {
public:
            bool isValid() {
                return false;
            }

            json::iterator* next() {
                return nullptr;
            }
        };

        int size() {
            return 1;
        }

        json::iterator* begin() {
            return nullptr;
        }

        json::iterator* end() {
            return nullptr;
        }

        void* data;
    };
}

void log(const char* msg);