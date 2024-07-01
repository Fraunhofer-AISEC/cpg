class Integer {
public:
   Integer(int i) : i(i) {}
   void operator++(int) {
     i++;
   }

   Integer operator+(int j) {
     return Integer(this->i+j);
   }

   Integer operator+(Integer &j) {
     return Integer(this->i+j.i);
   }

   void test() {};

   int i;
};

int main() {
    Integer i(5);
    i++;

    Integer j = i + 2;
    auto k = i + j;
}