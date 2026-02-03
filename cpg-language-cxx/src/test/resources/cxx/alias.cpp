namespace std {
    class string {
public:
        int size() {
            return 1;
        }

        class iterator {};
    };
}

// this is completely equivalent to a typedef
using mystring1 = std::string;

// this is a namespace alias. valid for the current scope
namespace estd = std;

using mystring2 = estd::string;

void manipulateString(const std::string& s) {
    // do something with the string
}

int main() {
    mystring1 s1;
    mystring2 s2;

    manipulateString(s1);
    manipulateString(s2);

    s1.size();
    s2.size();

    mystring1::iterator it1;
    mystring2::iterator it2;
    std::string it3;
    estd::string it4;
}
