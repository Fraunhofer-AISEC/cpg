/**
 * This comment clearly belongs to the class.
 */
public class Comments { // Class comment
    /** javadoc of arg */
    int arg;

    public Comments(int arg) {
        // We assign arg to this.arg
        // The comment needs 2 lines.
        this.arg = arg;
    }

    public static void main(String[] args) {
        // A for loop
        for(int i = 0 /* i decl*/; i < 2; i++) {
            System.out.print(i); // Crazy print
            // Comment which belongs to nothing
        }
    }
}