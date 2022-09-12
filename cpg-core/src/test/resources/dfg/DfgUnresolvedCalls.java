public class DfgUnresolvedCalls {
    public static void main(String[] args) {
        Optional<String> os = RandomClass.getOptionalString();
        String s = os.get();
        String s2 = os.get(4);
    }
}