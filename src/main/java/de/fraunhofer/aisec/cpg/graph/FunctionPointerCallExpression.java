package de.fraunhofer.aisec.cpg.graph;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Models a C/C++ call via a function pointer. These calls may either happen directly (C way: <code>
 * f_ptr(arg_a, arg_b)</code> or equivalently <code>(*f_ptr)(arg_a, arg_b)</code>) or may include a
 * specific object to call the function on (C++ way: <code>(instance.*f_ptr)(arg_a,
 * arg_b)</code> or <code>(*this.*f_ptr)(arg_a, arg_b)</code> or <code>(instance_ptr-&gt;*f_ptr)
 * (arg_a, arg_b)</code>)
 */
public class FunctionPointerCallExpression extends CallExpression {

  @Nullable private Expression instance;
  private Expression pointer;

  public Expression getInstance() {
    return instance;
  }

  public void setInstance(Expression instance) {
    this.instance = instance;
  }

  public Expression getPointer() {
    return pointer;
  }

  public void setPointer(Expression pointer) {
    this.pointer = pointer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FunctionPointerCallExpression)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    FunctionPointerCallExpression that = (FunctionPointerCallExpression) o;
    return Objects.equals(instance, that.instance) && Objects.equals(pointer, that.pointer);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
