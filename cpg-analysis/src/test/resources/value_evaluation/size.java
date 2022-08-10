public class MainClass {
    public static void main(String[] args) {
        int[] array = new int[3];
        for(int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        System.out.println(array[1]);
        return 0;
    }
 }