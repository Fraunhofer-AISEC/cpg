public class InnerClass {

    public String s = "outer.s";

    private class InnerInnerClass {

        public InnerInnerClass(){

        }
        public void setS(String newS){s = newS;};
    }

    /**public static void main(String[] args){
        InnerClass outer = new InnerClass();
        InnerClass.InnerInnerClass inner = outer.new InnerInnerClass();
        inner.setS("new.s");
        System.out.println("os: " + outer.s);
    }**/
}
