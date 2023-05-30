#include<string>
#include "external_class.h"
using namespace std;

ExternalClass::ExternalClass(){
    varName = "external_instance_field";
}

string ExternalClass::staticVarName = "external_static_field";