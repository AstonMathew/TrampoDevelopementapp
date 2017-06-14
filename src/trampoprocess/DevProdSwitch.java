package trampoprocess;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
// this fil is a SWITCH between PROD and DEV settings
public class DevProdSwitch {
  
    public static final String environment = "PROD"; //DEV or PROD
    Properties prop;
    
    public DevProdSwitch() {
        prop = new Properties();
        InputStream input = null;

        try {
            if(environment.equals("DEV")){
                input = this.getClass().getClassLoader().getResourceAsStream("DevProdSwitchSettings/dev.properties");
//              input = new FileInputStream("src/DevProdSwitchSettings/dev.properties"); //text file with key,value for DEVELOPMENT settings
            }else{
              input = this.getClass().getClassLoader().getResourceAsStream("DevProdSwitchSettings/prod.properties"); //text file with key,value for PRODUCTION settings
            }
            
            // load a properties file
            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
  
    public String getProperty(String key){
      return prop.getProperty(key);
    }
}
