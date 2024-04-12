namespace std {
    class string {};
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
}
