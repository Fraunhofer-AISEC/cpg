int main() {
  int i = sizeof(myClass);
  const std::type_info& typeInfo = typeid(myClass);
  int j = alignof(A);
  int k = typeof(A);
  //int l = sizeof...(A); somehow does not work
}