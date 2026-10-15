package inferred;

// "doesNotExist" is not declared by Provider. A single static import can name both a field and a
// method, so the import resolver has to infer BOTH members on the target class. We deliberately use
// it only as a field access here: a call would make an inferred method appear on Provider before
// the import resolver runs, so the "infer both" branch would already see a non-empty result and be
// skipped. A plain field access leaves Provider empty, which is what triggers the branch.
import static inferred.Provider.doesNotExist;

public class Consumer {

  public static void main(String[] args) {
    int y = doesNotExist; // resolves to the inferred field on Provider
  }
}
