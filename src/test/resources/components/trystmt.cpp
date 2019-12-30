int main() {
  try {
    some_dangerous_operation();
  } catch(const std::exception& e) {
    // named exception
  } catch(const std::exception&) {
    // unnamed exception
  } catch(...) {
    // catch all
  }
}