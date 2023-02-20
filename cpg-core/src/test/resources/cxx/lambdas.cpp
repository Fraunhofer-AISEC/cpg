#include <algorithm>
#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <string>
#include <vector>

// Not important for the tests.
void print_function(const std::string func) {
  std::cout << "Currently in function \"" << func << "\":" << std::endl;
}

// Simple test. No parameters in/out.
void lambda1() {
  print_function(__FUNCTION__);
  auto this_is_a_lambda = []() { std::cout << "Hello" << std::endl; };

  this_is_a_lambda();
}

// Receiving an input parameter.
void lambda2() {
  print_function(__FUNCTION__);
  auto this_is_a_lambda = [](uint64_t number) {
    std::cout << "Hello " << number << std::endl;
  };

  this_is_a_lambda(42);
}

// Lambda with specified return type.
void lambda3() {
  print_function(__FUNCTION__);
  auto this_is_a_lambda = [](bool flag) -> float {
    if (flag) {
      return 42;
    } else {
      return 3.141592;
    }
  };

  std::cout << typeid(this_is_a_lambda(true)).name() << std::endl;
  std::cout << typeid(this_is_a_lambda(false)).name() << std::endl;
}

// Capturing an outside variable by value.
void lambda4() {
  print_function(__FUNCTION__);
  uint64_t a_number = 42;
  auto this_is_a_lambda = [a_number]() {
    std::cout << a_number << std::endl;
    a_number++; //NOT ALLOWED!
  };
  this_is_a_lambda();
}

// Capturing an outside variable by reference.
void lambda5() {
  print_function(__FUNCTION__);
  uint64_t a_number = 42;
  auto this_is_a_lambda = [&a_number]() {
    std::cout << a_number << std::endl;
    a_number++; // This is ok now :)
  };
  this_is_a_lambda();
  std::cout << a_number << std::endl;
}

// Capturing an outside variable by value but make it mutable inside the lambda.
void lambda6() {
  print_function(__FUNCTION__);
  uint64_t a_number = 42;
  auto this_is_a_lambda = [a_number]() mutable {
    std::cout << a_number << std::endl;
    a_number++; // This is ok now :)
  };
  this_is_a_lambda();
  std::cout << a_number << std::endl;
}

// Capturing all outside variables by value.
void lambda7() {
  print_function(__FUNCTION__);
  uint64_t a_number = 42;
  auto this_is_a_lambda = [=]() { std::cout << a_number << std::endl; };
  this_is_a_lambda();
}

// Capturing all outside variable by reference.
void lambda8() {
  print_function(__FUNCTION__);
  uint64_t a_number = 42;
  auto this_is_a_lambda = [&]() {
    std::cout << a_number << std::endl;
    a_number++;
  };
  this_is_a_lambda();
  std::cout << a_number << std::endl;
}

// Lambdas and the STL.
void lambda9() {
  print_function(__FUNCTION__);
  auto v = std::vector<std::string>{"lambdas", "are", "awesome"};
  std::for_each(v.begin(), v.end(),
                [](const std::string it) { std::cout << it << std::endl; });
}

// Lambdas with initializers + mixed by value/reference capturing + shadowing of
// `a_number`.
void lambda10() {
  print_function(__FUNCTION__);
  uint64_t a_number = 1;

  auto this_is_a_lambda = [&ref = a_number,
                           a_number = a_number + 1]() -> uint64_t {
    ref += 2;
    return a_number + 2;
  }();

  std::cout << this_is_a_lambda << std::endl;
  std::cout << a_number << std::endl;
}

int main() {
  lambda1();
  lambda2();
  lambda3();
  lambda4();
  lambda5();
  lambda6();
  lambda7();
  lambda8();
  lambda9();
  lambda10();
  return EXIT_SUCCESS;
}
