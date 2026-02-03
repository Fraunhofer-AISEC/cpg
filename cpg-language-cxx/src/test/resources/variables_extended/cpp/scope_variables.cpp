#include<iostream>
#include<string>
#include<cstring>
#include "external_class.h"

using namespace std;

// TODO also create a Struct and an Enum with the variables
// The variables have mostly the same name to give the VariableUsageResolver a possibly hard time and uncover more faults.

void printLog(string logId, string message){
    cout << logId << ": " << message << endl;
}

class error;

class ScopeVariables{
    public:
        string varName = "instance_field";
        static string staticVarName;

        int functionX();

        void function1(){
            printLog("func1_impl_this_varName", varName);
            printLog("func1_static_staticVarName", staticVarName);
            for(string varName = "first_loop_local"; varName.size() < 17 ; varName += " "){
                printLog("func1_first_loop_varName", varName);
            }

            for(string varName = "second_loop_local"; varName.size() < 18 ; varName += " "){
                {
                    string varName = "local_in_inner_block";
                    printLog("func1_nested_block_shadowed_local_varName", varName);
                }
                printLog("func1_second_loop_varName", varName);
            }
        }

        void function2(string varName){
            printLog("func2_param_varName", varName);
            printLog("func2_this_varName", this->varName);
            // Initializer-Statement that is allowed in newer C++ versions
            if(string varName = "if_local"; varName.size() > 0){
                printLog("func2_if_varName", varName);
            }

            try {
                throw new error();
            } catch (error* varName) {
                printLog("func2_catch_varName", varName);
            };
            ScopeVariables scopeVariables;
            printLog("func2_instance_varName", scopeVariables.varName);
            printLog("func2_imp_this_varName", varName);

        }

        void function3(string varName){
            ScopeVariables scopeVariables;
            printLog("func3_instance_varName", scopeVariables.varName);

            ExternalClass externalClass;
            printLog("func3_external_instance_varName", externalClass.varName);

        }

        void function4(){
            printLog("func4_static_staticVarName", ScopeVariables::staticVarName);
            printLog("func4_external_staticVarName", ExternalClass::staticVarName);
            ExternalClass externalClass;
            printLog("func4_external_instance_varName", externalClass.varName);
            printLog("func4_second_external_staticVarName", ExternalClass::staticVarName);
        }

        void function5(){
            ScopeVariables first;
            ScopeVariables second;
            first.staticVarName = "staticVarName_Of_Both";

            printLog("func5_staticVarName_throughInstance_first", first.staticVarName);
            printLog("func5_staticVarName_throughInstance_second", second.staticVarName);
        }

        // C++ inner classes are currently not parsed
        class InnerClass {
            public:
                string varName = "inner_instance_field";
                static string staticVarName;

                void function1(){
                    printLog("func1_inner_imp_this_varName", varName);
                    InnerClass inner;
                    ScopeVariables scopeVariables;
                    printLog("func1_inner_instance_varName", inner.varName);
                    printLog("func1_outer_instance_varName", scopeVariables.varName);
                    printLog("func1_outer_static_staticVarName", ScopeVariables::staticVarName);
                    printLog("func1_inner_static_staticVarName", ScopeVariables::InnerClass::staticVarName); // Can i remove the ScopeVariables:: ??
                // There is no special reference to the outer-class like ClassName.this.varname as in Java

                }


                void function2(string varName){
                    string staticVarName = "inner_local_named_static";
                    InnerClass inner;
                    ScopeVariables scopeVariables;
                    printLog("func2_inner_instance_varName_with_shadows", inner.varName);
                    printLog("func2_outer_instance_varName_with_shadows", scopeVariables.varName);
                    printLog("func2_outer_static_staticVarName_with_shadows", ScopeVariables::staticVarName);
                    printLog("func2_inner_static_staticVarName_with_shadows", ScopeVariables::InnerClass::staticVarName); // Can i remove the ScopeVariables:: ??
                // There is no special reference to the outer-class like ClassName.this.varname as in Java

                }

        };
};
int main (int argc, char *argv[]) {
    string varName = "parameter";
    ScopeVariables scopeVariables;
    scopeVariables.function1();
    scopeVariables.function2(varName);
    scopeVariables.function3(varName);
    scopeVariables.function4();
    scopeVariables.function5();
    ScopeVariables::InnerClass innerClass;
    innerClass.function1();
    innerClass.function2(varName);
    printLog("main_local_varName", varName);
}

string ScopeVariables::staticVarName = "static_field";
string ScopeVariables::InnerClass::staticVarName = "inner_static_field";

int ScopeVariables::functionX(){
    return 0;
}
