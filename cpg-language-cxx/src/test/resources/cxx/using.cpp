namespace std
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
