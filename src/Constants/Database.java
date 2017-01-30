package constants;

import trampoprocess.Simulation;

public class Database {
  public static String URL = "jdbc:postgresql://ec2-54-163-240-7.compute-1.amazonaws.com:5432/d96mtni1684ujv?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";
  public static String USERNAME = "pknholbxypzuqd";
  public static String PASSWORD = "f536787b38cb84f9dd5300060a2eb2e223de0e72eeea15e47a355a8f6dc3226b";
  
  public static String SIMULATION_NO = "no";
  public static String CUSTOMER_ID = "customer_id";
  public static String FILE_NAME = "file_name";
  public static String MAX_RUNTIME = "max_runtime";
  public static String ACTUAL_RUNTIME = "act_runtime";
  public static String FILE_COUNT = "file_count";
  public static String SUBMISSION_DATE = "sub_date";
  public static String CANCELED_BY_USER= "canceled_by_user";
  public static String STATUS = "status";
  
}
