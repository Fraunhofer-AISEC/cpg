// simple typedef
typedef unsigned long ulong;

// chained typedef
typedef ulong ulong2;
typedef ulong2 ulong3;

// the following objects have the same type
unsigned long l1;
ulong l2;
ulong2 l3;
ulong3 l4;

unsigned long *l1ptr;
ulong *l2ptr;
ulong2 *l3ptr;
ulong3 *l4ptr;

unsigned long l1arr[];
ulong l2arr[];
ulong2 l3arr[];
ulong3 l4arr[];

// special cases
typedef long *longp_t;
typedef int intarr[20];
typedef unsigned int (*uint_fp_t)(long, long);

long *longptr1;
longp_t longptr2;

int arr1[20];
intarr arr2;

unsigned int (*uintfp1)(long, long);
uint_fp_t uintfp2;

// more complicated typedef
typedef int int_t, *intp_t, (*fp)(int, ulong), arr_t[10];

int i1;
int_t i2;

// the following two objects have the same type
int a1[10];
arr_t a2;

// int pointers
int *intPtr1;
intp_t intPtr2;

// int function ptr
int fun(int i, ulong u) {
  return 0;
}

int (*intFptr1)(int, unsigned long);
fp intFptr2;

// common C idiom to avoid having to write "struct S"
typedef struct {int a; int b;} S, *pS;

// struct pointers
S *ps1;
pS ps2;

// typedef can be used anywhere in the decl-specifier-seq
unsigned long typedef long int ullong;
// more conventionally spelled "typedef unsigned long long int ullong;"
unsigned long long int someUllong1;
ullong someUllong2;

// std::add_const, like many other metafunctions, use member typedefs
typedef long type;
type typeMemberOutside;

// sample typedef with tabs
typedef uint8		test;

struct add_const {
    typedef const int type;
    const int typeMember1;
    type typeMember2;
};

// template, not to be confused with multiple typedef
typedef template_class_A<int, int> type_B;


int main() {
  typedef char *type;
  char *cptr1;
  type cptr2;
}
