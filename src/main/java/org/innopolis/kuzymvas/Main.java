package org.innopolis.kuzymvas;

import org.innopolis.kuzymvas.outofmemory.HeapOOMEGenerator;
import org.innopolis.kuzymvas.outofmemory.MetaspaceOOMEGenerator;

import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        boolean generateInMetaSpace = true;
        Scanner scanner = new Scanner(System.in);
        System.out.println("If you wish to use a profiler, attach it to the JVM process now");
        System.out.println("Input 'm' to start generating a metaspace Out of Memory Error" +
                                   "or 'h' to start generating a heap Out of Memory Error: [m]");
        String line = scanner.nextLine();
        while (!line.equals("m") && !line.equals("h") && !line.equals("")) {
            line = scanner.nextLine();
        }
        if (line.equals("h")) {
            generateInMetaSpace = false;
        }

        if (generateInMetaSpace) {
            System.out.println("Overfilling the metaspace:");
            MetaspaceOOMEGenerator generator = new MetaspaceOOMEGenerator(
                    100, System.out, Paths.get(""));
            generator.generateOOME();
        } else {
            System.out.println("Overfilling the heap");
            HeapOOMEGenerator generator = new HeapOOMEGenerator(
                    10240, 20480, 2000, 1000,
                    System.out);
            generator.generateOOME();
        }
    }
}
