import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import com.sun.management.ThreadMXBean;

public class Extract {

    public static void main(String[] args) {

        try {
            for (Scanner sc = new Scanner(new File(args[0])); sc.hasNext();) {
                String line = sc.nextLine();
                switch (args[1]) {
                case ("sol"):
                    if (line.contains("solution")) {
                        String[] str = line.split(" ");
                        if (Integer.parseInt(str[1]) % Integer.parseInt(args[2]) == 0)
                            System.out.println(str[1] + "," + sc.nextLine().split("/")[2].trim());
                    }
                    break;
                case ("sq"):
                    if (line.contains("stats")) {
                        System.out.print(line.substring(9,line.length()-1).trim()+",");
                        do
                            line = sc.nextLine();
                        while (!line.contains("* " + args[2]));
                            System.out.println(line.substring(6));
                    }
                }
            }
        } catch (FileNotFoundException e) {
        }
    }
}