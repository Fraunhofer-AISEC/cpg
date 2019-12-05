package compiling;

class Processor {

  private int field;

  int process(int input) {
    int local = 1;

    local += input;

    return field + local;
  }
}