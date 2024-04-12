typedef long type;
type typeMemberOutside;

struct add_const {
    typedef const int type;
    const int typeMember1;
    type typeMember2;
};

add_const::type test = 2;

int main() {
typedef char *type;
type cptr2;
{
  typedef int* type;
}
}

