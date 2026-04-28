public class RecordExample {
    public static void main(String[] args) {
        // Create a valid record
        Record r = new Record("Param1", 2);
        System.out.println(r);
    }
}

// Record definition
record Record(String p1, int p2) {

    // Custom constructor with validation
    public Record {
        if (p1 == null || p1.isBlank()) {
            throw new IllegalArgumentException("p1 must not be blank");
        }
        if (p2 < 0) {
            throw new IllegalArgumentException("p2 must be non-negative");
        }
    }

    public String recordFunction() {
        return "Some additional record function.";
    }
}
