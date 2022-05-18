import javax.imageio.IIOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class main {
    public static void main(String[] args) throws FileNotFoundException {
        Simulation sim = new Simulation();
        try{
            sim.runSimulations();
        }catch(Exception e){
            System.out.println("exception");
        }
        File f1 = new File("/Users/jaridevos/Documents/Simulation/Exercise3/randomnumbersElective.txt");
        ArrayList<String> read = new ArrayList<>();
        Scanner sc = new Scanner(f1);
        while(sc.hasNextLine()){ read.add(sc.nextLine()); }
        sc.close();
        //System.out.println(read.size());
    }
}