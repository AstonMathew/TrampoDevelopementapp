package main.resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

//this file is a switch to change from DEV setting to PROD settings.
public class Config {
  
    public static final String environment = "DEV"; //DEV or PROD
    Properties prop;
    
    public Config() {
        prop = new Properties();
        InputStream input = null;

        try {
            if(environment.equals("DEV")){
              input = new FileInputStream("src/main/resources/dev.properties");
            }else{
              input = new FileInputStream("src/main/resources/prod.properties");
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
