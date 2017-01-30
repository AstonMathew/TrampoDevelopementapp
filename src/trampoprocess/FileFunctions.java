package trampoprocess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class FileFunctions {
	public static boolean fileIsAvailable(Path file) {
	  return Files.isRegularFile(file);
	}
	
	public static int countFiles(Path dir) {
		int count = 0;
		try {
			Iterator<Path> fileIt = Files.list(dir).iterator();
			while (fileIt.hasNext()) {
				Path file = fileIt.next();
				System.out.println("Going thorugh file " + file);
				if (Files.isRegularFile(file)) {count ++ ;}
			}
		} catch (IOException e) {
			System.out.println("Unable to count files in " + dir);
			count = 0;
		}
		return count;
	}

}
