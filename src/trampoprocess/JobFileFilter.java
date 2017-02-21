/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trampoprocess;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * @author Administrator
 */
public class JobFileFilter implements FileFilter{
 private final String[] simFileExtensions = new String[] {"sim"}; // put aditional sim file extension in this array

  public boolean accept(File file)
  {
    for (String extension : simFileExtensions )
    {
      if (file.getName().toLowerCase().endsWith(extension))
      {
        return true;
      }
    }
    return false;
  }
}
