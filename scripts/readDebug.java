import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class readDebug {
    static String fileString = "debugHelp";
    static File f;
    static Scanner s;

    public static void main(String[] args) {
        try {
            f = new File(fileString);
            s = new Scanner(f);
        } catch (FileNotFoundException fe) {
            System.err.println("File not found!");
            System.exit(1);
        }

        while (s.hasNext()) {
            String nextLine = s.nextLine();
            System.out.println(nextLine.replace('#',' '));
        }
    }
}