package scan4macro;

import java.io.File;

public class Scan extends TypesExtractor{

    public Scan(File f) {
      super(f);
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
