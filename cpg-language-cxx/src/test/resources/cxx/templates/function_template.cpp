// abs uses a template with the type directly
template <typename T> inline T abs(T t) {
    return t >= 0 ? t : -t;
}

// abs2 uses a template with a const reference, which also needs to match any type T
template <typename T> inline T abs2(const T &t) {
    return t >= 0 ? t : -t;
}

// abs3 also uses a const reference but without a template
double abs3(const double &d) {
    return d >= 0 ? d : -d;
}

int main() {
    abs(1.0);

    double d = 1.0;

    abs2(d);
    abs3(d);
}