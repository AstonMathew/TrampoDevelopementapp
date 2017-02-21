/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trampoprocess;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;

/**
 *
 * @author Administrator
 */
//                    3lines below needs trouble shooting 
//                    https://www.youtube.com/watch?v=aimd2tn0hLM        
//                    http://codingjunkie.net/java-7-copy-move/  
//                    https://dzone.com/articles/what%E2%80%99s-new-java-7-copy-and 
//                    http://stackoverflow.com/questions/3008043/list-all-files-from-directories-and-subdirectories-in-java  
public class JobFolderFileVisitor extends SimpleFileVisitor<Path> {

    Path src;
    Path dest;

    JobFolderFileVisitor(Path src, Path dest) {
        this.src = src;
        this.dest = dest;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        System.out.println("Just VisitedDir: " + dir.toString());
        //System.out.println("simulation folder copied");
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

//        Path target = dest.resolve(src.relativize(dir));
//        try {
//            Files.copy(dir, target);
//
//        } catch (FileAlreadyExistsException e) {
//            if (!Files.isDirectory(target)) {
//                throw e;
//            }
//        }
//
//        System.out.println("Directory copied: " + dir.toString().replaceAll(Matcher.quoteReplacement(src.toString()), ""));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (!Files.isRegularFile(file)) {
            Files.move(file, dest.resolve(src.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES);
            System.out.println("File Copied: " + file.toString().replaceAll(Matcher.quoteReplacement(src.toString()), ""));
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        System.err.println(e.getMessage());
        return FileVisitResult.CONTINUE;
    }

}
