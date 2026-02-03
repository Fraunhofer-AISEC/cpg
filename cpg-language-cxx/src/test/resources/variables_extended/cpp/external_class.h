#pragma once
using namespace std;
static string staticVarName;

class ExternalClass {
    public:
    string varName;
    static string staticVarName;

    ExternalClass();
};