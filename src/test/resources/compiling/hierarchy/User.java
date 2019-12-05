public class User {

    private Interface defaultInterface;
    private Interface interface1, interface2;
    private Super s;

    public User(boolean useNewInterface) {
        defaultInterface = useNewInterface ? new Implementor2() : new Implementor1();
        interface1 = new Implementor1();
        interface2 = new Implementor2();
        s = new Sub(123);
    }

    public int getLocalInt() {
        var localInterface = new Implementor1();
        return localInterface.getInt();
    }

    public int getInt() {
        return defaultInterface.getInt();
    }

    public int getFirst() {
        return interface1.getInt();
    }

    public int getSecond() {
        return interface2.getInt();
    }

    public int getSuperInt() {
        return s.getSuperInt();
    }

    @Override
    public String toString() {
        return "User[s=" + s.toString() + "]";
    }
}