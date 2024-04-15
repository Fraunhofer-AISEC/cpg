namespace std
<<<<<<< HEAD
 {
   class string
   {
   public:
     const char *c_str() const
     {
       return "mock";
     }
   };
 }

void function1()
{
   using namespace std;
   string s;
   s.c_str();
}

void function2()
  {
    using std::string;
    string s;
    s.c_str();
  }

=======
{
  namespace inner {
    class secret {
      void someFunction() {
        secret* s = new secret();
      }
    };
  }

  class string
  {
  public:
    const char *c_str() const
    {
      return "mock";
    }
  };

  class other {
    class sub {};
  private:
    void doSomething() {
      inner::secret secret;
      sub sub;
      other::sub sub2;
    }
  };
}

using namespace std;

int main()
{
  inner::secret secret;
  string s;
  s.c_str();
}
>>>>>>> 19ac8b9925 (Draft for type normalisation)
