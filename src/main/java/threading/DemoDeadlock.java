package threading;

public class DemoDeadlock {
    public static void main(String [] args){

        Deadlock d=new Deadlock();
        try {
            d.runTest();;
        }catch (InterruptedException _){

        }
    }
}


