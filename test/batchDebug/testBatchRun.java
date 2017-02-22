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

    public static void main(String[] args) throws IOException, InterruptedException, Exception {
        // Prints "Hello, World" in the terminal window.
        System.out.println("Hello, World");
         // public JobTest(Integer jobNumber, String customerNumber, String submissionDate, int maxSeconds,             String simulation, int fileCount) 
        JobTest j = new JobTest(100,"5543813196","test", 100, "Cube.sim",1);
        j.runJobWorkflow();
    }

    
}
