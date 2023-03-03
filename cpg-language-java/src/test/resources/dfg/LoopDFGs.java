public class LoopDFGs {

    public void looping(int param){
        int a = 0;
        while(param % 6 == 5){
            if(param > 7){
                a = 1;
            }else{
                System.out.println(a); // Should have a dfg path from 0,1,2 but not 3
                a = 2;
            }
        }
        a = 3;
    }

    public void labeledBreakContinue(int param){
        int a = 0;
        lab1: while(param < 5){
            while(param > 6) {
                if (param > 7) {
                    a = 1;
                    continue lab1;
                } else {
                    System.out.println(a); // Should have a dfg path from 0, 1, 3
                    a = 2;
                    break lab1;
                }
                a = 4;
            }
            System.out.println(a); // Should have a dfg path from 0, 1, 3
            a = 3;
        }
        System.out.println(a); // Should have a dfg path from 0,1,2,3
    }
}
