// Fixtures for member access-control resolution (public / protected / private).

class Base {
public:
  int publicMethod() { return 1; }

protected:
  int protectedMethod() { return 2; }

private:
  int secret = 0;
  // A private member is freely accessible from within its own record.
  int privateMethod() { return secret; }
};

// A subclass may access the protected (but not the private) members of its base.
class Derived : public Base {
public:
  int useProtected() { return protectedMethod(); }
};

// Two unrelated bases that both declare a `ping`, once accessible and once not.
class Speaker {
public:
  int ping() { return 1; }
};

class Muted {
private:
  int ping() { return 2; }
};

// `Combined` inherits `ping` from both bases; only `Speaker::ping` is accessible
// from outside the class, so an external call must resolve to it.
class Combined : public Speaker, public Muted {};

int callPing(Combined *c) { return c->ping(); }

// A private method declared in the class but defined out-of-line: the definition must
// inherit the access control (private) of its in-class declaration.
class OutOfLine {
private:
  int hidden();
};

int OutOfLine::hidden() { return 42; }
