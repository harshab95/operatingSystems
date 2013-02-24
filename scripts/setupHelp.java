import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class setupHelp {
    static File f;
    static Scanner s;
    static boolean verbose;
    static boolean poundFlag;
    static boolean squeeze = false;
    static boolean sectionFlag = false;
    static boolean waitForNextSection = false;
    static HashMap<String, String> sNames;
    static ArrayList<String> sList = new ArrayList<String>();
    static String fileString = "setup";
    static int tabSize = 70;

    public static void main(String[] args) {
        poundFlag = false;
        f = new File(fileString);
        sNames = new HashMap<String, String>();

        sNames.put("g", "General Use");
        sNames.put("1", "Project 1");
        sNames.put("2", "Project 2");
        sNames.put("3", "Project 3");
        sNames.put("4", "Project 4");

        try {
            s = new Scanner(f);
        } catch (FileNotFoundException f) {
            System.out.println("File Not Found!: " + fileString);
            System.exit(1);
        }

        if (args.length == 1) {
            int charCount = 0;
            String allFlags = args[0];
            while (charCount < allFlags.length()) {
                char flag = allFlags.charAt(charCount);
                if (flag == 'v') {
                    verbose = true;
                } else if (flag == 's') {
                    squeeze = true;
                } else if (flag == 'g') {
                    sectionFlag = true;
                    sList.add("g");
                } else if (flag == '1') {
                    sectionFlag = true;
                    sList.add("1");
                } else if (flag == '2') {
                    sectionFlag = true;
                    sList.add("2");
                } else if (flag == '3') {
                    sectionFlag = true;
                    sList.add("3");
                } else if (flag == '4') {
                    sectionFlag = true;
                    sList.add("4");
                } else {
                    System.err.println("Possible flags are: s, v ");
                    System.exit(1);
                }
                charCount++;
            }
        } else if (args.length > 1) {
            System.err.println("Usage: java setupHelp [ - flags]");
        }

        printHelp();
    }

    public static void printHelp() {

        while (s.hasNext()) {
            String nextLine = s.nextLine();
            if (nextLine.equals("")) {
                if (!squeeze && !waitForNextSection) {
                    System.out.println();
                }
            } else if (nextLine.charAt(0) != '#') {
                if (!waitForNextSection) {
                    String[] stringArray = nextLine.split("alias");
                    String command = null;
                    int spacesUsed = 0;
                    for (int j = 1; j < stringArray.length; j++) {
                        if (command == null) {
                            String[] commandArray = stringArray[j].split("=\"");
                            commandArray = commandArray[1].split("\";");
                            command = "\"" + commandArray[0] + "\"";
                        }

                        String[] aliasArray = stringArray[j].split("=");
                        aliasArray = aliasArray[0].split(" ");
                        String printCommand = aliasArray[1];
                        System.out.print(printCommand);
                        spacesUsed += printCommand.length();
                        if (j < (stringArray.length - 1)) {
                            System.out.print(", ");
                            spacesUsed += 2;
                        } else {
                            System.out.print(":");
                            spacesUsed += 1;
                        }
                    }
                    for (int k = 0; k < (tabSize - spacesUsed); k++) {
                        System.out.print(" ");
                    }
                    System.out.println(command);
                }
            } else {
                if (nextLine.charAt(1) == '#') {
                    poundFlag = !poundFlag;
                    if (!waitForNextSection && !poundFlag) {
                        System.out.println();
                    }
                } else {
                    if (poundFlag) {
                        if (!sectionFlag) {
                            if (!waitForNextSection) {
                                System.out.println(waitForNextSection);
                                System.out.println("\n#############################################################");
                                System.out.println(nextLine);
                                System.out.println("#############################################################");
                            }
                        } else {
                            boolean found = false;
                            for (int sNum = 0; sNum < sList.size(); sNum++) {
                                String secName = sNames.get(sList.get(sNum));
                                if (nextLine.contains(secName)) {
                                    System.out.println("\n#############################################################");
                                    System.out.println(nextLine);
                                    System.out.println("#############################################################");
                                    found = true;
                                    waitForNextSection = false;
                                    break;
                                }
                            }
                            if (!found) {
                                waitForNextSection = true;
                            }
                        }
                    } else if (verbose && !waitForNextSection) {
                        System.out.println(nextLine);
                    }
                }
            }
        }

        System.out.println();

    }
}
