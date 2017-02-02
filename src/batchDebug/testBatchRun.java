/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package batchDebug;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Administrator
 */
public class testBatchRun {



    public static void main(String[] args) throws IOException, InterruptedException {
        // Prints "Hello, World" in the terminal window.
        System.out.println("Hello, World");
        Run r = new Run();
        r.run();
    }

    
}
