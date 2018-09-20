import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import com.sun.management.ThreadMXBean;

public class Extract {

    public static void main(String[] args) {

        try {
            for (Scanner sc = new Scanner(new File(args[0])); sc.hasNext();) {
                String line = sc.nextLine();
                if (line.contains("min")) {
                    String[] str = line.split(" ");
                    if (Integer.parseInt(str[0]) % 10000 == 0)
                        System.out.println(str[0] + "," + str[str.length-1]);
                }
                // else if (line.contains("* "+args[1]+":"))
                // System.out.println(","+line.substring(line.lastIndexOf(" ") + 1));
            }
        } catch (FileNotFoundException e) {
        }
    }
}