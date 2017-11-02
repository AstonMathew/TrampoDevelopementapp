package com.trampo.process.util;

import java.io.File;
import scan4macro.TypesExtractor;
import scan4macro.WhiteClasses;
import scan4macro.WhitePackages;

public class Scan extends TypesExtractor{

    public Scan(File f) {
      super(f);
      File csvFiles = new File("src\\csv"); // open csv path 
      File[] csvs = csvFiles.listFiles();
      for( int i=0; i<csvs.length; i++ ) {
              if( !csvs[i].getName().endsWith(".csv") ) // check if not csv file
                  continue;
              if( csvs[i].getName().contains("whitelist") ) { // if filename has whitelist in its file name then it's package declaration csv
                   new WhitePackages(csvs[i].getPath());
              }else {
                   new WhiteClasses(csvs[i].getPath(),csvs[i].getName().replace("csv", ""));
              }
      }
    }
     
    public boolean scan(){
      try {
        extractAll();
      } catch (Exception ex) {
        ex.printStackTrace(); 
      }
      if(unsafeList.size() == 0){
        return true;
      }
      return false;
    }
}
