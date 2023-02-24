char s[] = "string";
int i = 1;
float f = 0.2f;
double d = 0.2;
bool b = false;
char c = 'c';
auto hex = 0xfull;

// ms is a custom literal suffix (provided by <chrono>. however, we are not handling this at the moment, so this
// will result in an error
auto duration_ms = 250ms;
auto duration_s = 2.5s;
