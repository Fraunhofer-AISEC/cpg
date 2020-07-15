package variablesExtended;

import java.lang.RuntimeException;
import java.lang.Exception;

public class ScopeVariables {

    // The variables have mostly the same name to give the VariableUsageResolver a possibly hard time and uncover more faults.
    public String varName = "instance_field";
    public static String staticVarName = "static_field";

    public static void main(String[] args) {
        ScopeVariables scopeVariable = new ScopeVariables();
        String varName = "local_varName";
        scopeVariable.function1();
        scopeVariable.function2(varName);
        scopeVariable.function3(varName);
        scopeVariable.function4(varName);
        InnerClass innerClass = scopeVariable.new InnerClass();
        innerClass.function1();
        innerClass.function2(varName);
    }

    public void function1() {
        // Access field
        for (String varName = "first_loop_local"; varName.length() < 17; varName += " ") {
            printLog("func1_first_loop_varName", varName); // Should reference their respective loop scope
        }
        for (String varName = "second_loop_local"; varName.length() < 18; varName += " ") {
            printLog(
                    "func1_second_loop_varName", varName); // Should reference their respective loop scope
        }
        printLog("func1_imp_this_varName", varName);
    }

    public void function2(String varName) {
        printLog("func2_param_varName", varName);
        // Consumer accessOuterVarname =
        //    (lambdaVarName) -> printLog("func2_inLambda_param_varName", varName);
        // TODO implement test functions from Anonymous functions and lambdaExpressions
    }

    public void function3(String varName) {
        String staticVarName; // To try to shadow static varname in case the resolution behaves wrongly,
        // this should not shadow
        ExternalClass externalClass = new ExternalClass();
        printLog("func3_external_instance_varName", externalClass.varName);
        printLog(
                "func3_external_static_staticVarName",
                ExternalClass
                        .staticVarName); // Should reference static var in class and not be shadowed by this
        // class field
    }

    public void function4(String varName) {
        printLog(
                "func4_external_static_staticVarName",
                ExternalClass
                        .staticVarName); // Should reference static var in class and not be shadowed by this
        // class field
    }

    public class InnerClass {
        public String varName = "inner_instance_field";
        public String staticVarName =
                "inner_static_field"; // Not really a static variable but we name it like this to see if
        // variable resolution works correctly

        public void function1() {
            printLog("func1_inner_imp_this_varName", varName);
            printLog("func1_outer_this_varName", ScopeVariables.this.varName);
            printLog("func1_outer_static_staticVarName", ScopeVariables.staticVarName);
        }

        public void function2(String varName) {
            printLog("func2_inner_param_varName", varName);
            printLog("func2_inner_this_varName", this.varName);
        }

        public void function3(){
            try{
                throw new RuntimeException("excpetion");
            }catch(RuntimeException staticVarName){
                printLog("func3_inner_exception_staticVarName", staticVarName);
            }catch(Exception varName){
                printLog("func3_inner_exception_varName", varName);
            }
        }
    }

    public static void printLog(String logID, String message) {
        System.out.println(logID + ": " + message);
    }
    public static void printLog(String logID, Exception message) {
        System.out.println(logID + ": " + message.getMessage());
    }
}