class T {
	int i=0;
	int foo() {
		return this->i;
	}
};

struct S {
	int i=0;
	int foo() {
		return this->i;
	}
};

int main() {
  T t;
  S s;
}
