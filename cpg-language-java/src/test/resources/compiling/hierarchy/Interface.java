public interface Interface {

    int getInt();

    default String print() {
        return "Default print!";
    }
}