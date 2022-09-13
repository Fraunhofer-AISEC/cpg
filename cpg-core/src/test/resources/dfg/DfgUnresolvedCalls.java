public class DfgUnresolvedCalls {
    private int i;
    public DfgUnresolvedCalls(int i) {
        this.i = i;
    }

    public int knownFunction(int arg) {
        return this.i + arg;
    }

    public static void main(String[] args) {
        Optional<String> os = RandomClass.getOptionalString();
        String s = os.get();
        String s2 = os.get(4);

        DfgUnresolvedCalls duc = new DfgUnresolvedCalls(3);
        int i = duc.knownFunction(2);
    }
}