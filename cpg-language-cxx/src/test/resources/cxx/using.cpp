namespace std
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

