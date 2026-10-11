package inferred;

// "doesNotExist" is not declared by Provider. A single static import can name both a field and a
// method, so we do not know what to infer when resolving the import. Instead, the members are
// inferred on Provider (and not on Consumer) once they are used: here, both as a field and a method.
import static inferred.Provider.doesNotExist;

public class Consumer {

  public static void main(String[] args) {
    int y = doesNotExist; // resolves to the inferred field on Provider
    doesNotExist(); // resolves to the inferred method on Provider
  }
}
