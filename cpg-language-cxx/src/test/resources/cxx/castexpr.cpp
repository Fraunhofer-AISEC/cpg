int main() {
  ExtendedClass* e = new ExtendedClass();
  BaseClass* b = (BaseClass*) e;
  b = static_cast<BaseClass*>(e);
  b = reinterpret_cast<BaseClass*>(e);
  int d = (int) 0.4f;
}