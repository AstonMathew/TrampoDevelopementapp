package trampoprocess;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import constants.Database;
import constants.JobStatuses;

/**
 * Interaction with the webapp to update simulation status, query simulations,
 * etc
 * 
 * @author julien
 *
 */
public class WebAppGate {
	private static final Logger LOGGER = Logger.getLogger(WebAppGate.class.getName());

	//public static WebAppGateDatabase make() { return new WebAppGateDatabase(); }
   public static WebAppGateHttp make() { return new WebAppGateHttp(); }
	
	public static class WebAppGateHttp {
		private static JSONParser _jsonParser = new JSONParser();
		
		public WebAppGateHttp() {
		}
		
		public String getJobStatus(Job sim) throws Exception {
			try {
				System.out.println("Check job status for job " + sim._jobNumber.toString());
				String url = webAppBuildUrl("getjobStatus");
				Map<String, String> map = new HashMap<String, String>();
				map.put("customer_id", sim._customerNumber);
				map.put("sim_no", sim._jobNumber.toString());
				String resp = webAppHttpRequest(url, urlEncode(map));
				JSONObject resp2 = (JSONObject) _jsonParser.parse(resp);
				if (((String) resp2.get("result")).equals("success")) {
					return (String) resp2.get("status");
				} else {
					System.out.println("Web App report error message: " + (String) resp2.get("result"));
					return null;
				}
			} catch (Exception e) {
				LOGGER.info("Unable to connect to webApp");
				LOGGER.log(Level.WARNING, null, e);
			}
			return null;			
		}

		public boolean isJobCanceled(Job sim) throws Exception {
			try {
				System.out.println("Check if job has been canceled for job " + sim._jobNumber.toString());
				String url = webAppBuildUrl("isjobCanceled");
				Map<String, String> map = new HashMap<String, String>();
				map.put("customer_id", sim._customerNumber);
				map.put("sim_no", sim._jobNumber.toString());
				String resp = webAppHttpRequest(url, urlEncode(map));
				JSONObject resp2 = (JSONObject) _jsonParser.parse(resp);
				if (((String) resp2.get("result")).equals("success")) {
					return (boolean) resp2.get("isjobCanceled");
				} else {
					System.out.println("Web App report error message: " + (String) resp2.get("result"));
					return false;
				}
			} catch (Exception e) {
				LOGGER.info("Unable to connect to webApp");
				LOGGER.log(Level.WARNING, null, e);
			}
			return false;			
		}
		
		public Integer getJobMaxRuntime(Job sim) throws Exception {
			try {
				System.out.println("Get Job maximum runtime for simulation " + sim._jobNumber.toString());
				String url = webAppBuildUrl("getJobMaxRuntime");
				Map<String, String> map = new HashMap<String, String>();
				map.put("customer_id", sim._customerNumber);
				map.put("sim_no", sim._jobNumber.toString());
				String resp = webAppHttpRequest(url, urlEncode(map));
				JSONObject resp2 = (JSONObject) _jsonParser.parse(resp);
				if (((String) resp2.get("result")).equals("success")) {
					return (int) resp2.get("jobMaxRuntime");
				} else {
					System.out.println("Web App report error message: " + (String) resp2.get("result"));
					return null;
				}
			} catch (Exception e) {
				LOGGER.info("Unable to connect to webApp to Job maximum runtime for simulation " + sim._jobNumber.toString());
				LOGGER.log(Level.WARNING, null, e);
			}
			return null;			
		}
		
		public LinkedList<Job> getSimulations() throws Exception {
			return getSimulations(JobStatuses.SUBMITED);
		}

		public LinkedList<Job> getSimulations(String status) throws Exception {
			LinkedList<Job> simulations = new LinkedList<Job>();
			try {
				System.out.println("Get simulations with status " + status);
				String url = webAppBuildUrl("getJobs");
				Map<String, String> map = new HashMap<String, String>();
				map.put("status", status);
				String resp = webAppHttpRequest(url, urlEncode(map));
				JSONObject resp2 = (JSONObject) _jsonParser.parse(resp);
				if (((String) resp2.get("result")).equals("success")) {
					System.out.println("Response is : " + resp);
					JSONArray jobs = (JSONArray) resp2.get("jobs");
					Iterator<JSONObject> jobsIt = jobs.iterator();
					while(jobsIt.hasNext()) {
						JSONObject job = jobsIt.next();
						// JSONObject job = (JSONObject) _jsonParser.parse(jobInfo);
						simulations.add(new Job(((int)(long) job.get(Database.SIMULATION_NO)), 
								((String) job.get(Database.CUSTOMER_ID)).trim(),
								(String) job.get(Database.SUBMISSION_DATE), 
								(int)(long) job.get(Database.MAX_RUNTIME),
								(String) job.get(Database.FILE_NAME), 
								(int)(long) job.get(Database.FILE_COUNT)));
					}
				} else {
					System.out.println("Web App report error message: " + (String) resp2.get("result"));
				}
			} catch (Exception e) {
				LOGGER.info("Unable to connect to webApp to retrieve simulations with status " + status);
				LOGGER.log(Level.WARNING, null, e);
			}
			return simulations;						
		}
		
		public LinkedList<String> getCustomerList() throws Exception {
			throw new Exception("Function not implemented");
			// return null;						
		}
		
		public void updateJobActualRuntime(Job sim, Integer actualRuntime) throws Exception {
			try {
				System.out.println("Update simulation run time for simulation " + sim._jobNumber);
				Map<String, String> map = new HashMap<String, String>();
				map.put("method", "updateActualRuntime");
				map.put("customer_id", sim._customerNumber);
				map.put("sim_no", sim._jobNumber.toString());
				map.put("actual_runtime", actualRuntime.toString());
				String url = webAppBuildUrl(map);
				webAppHttpRequest(url);
			} catch (Exception e) {
				LOGGER.info("Unable to connect to webApp to update runtime for simulation " + sim._jobNumber + " by "
						+ sim._customerNumber + ". Keep simulation running");
				LOGGER.log(Level.WARNING, null, e);
			}
		}

		public void updateJobStatus(String customerNumber, Integer jobNumber, String status)
				throws Exception {
			try {
				System.out.println("Update simulation status for simulation " + jobNumber);
				Map<String, String> map = new HashMap<String, String>();
				map.put("method", "updateJobStatus");
				map.put("customer_id", customerNumber);
				map.put("sim_no", jobNumber.toString());
				map.put("update_job_status", status);
				String url = webAppBuildUrl(map);
				webAppHttpRequest(url);
			} catch (Exception e) {
				LOGGER.info("Unable to connect to webApp to update simulation status for simulation " + jobNumber
						+ " by " + customerNumber + " has been canceled. Keep simulation running");
				LOGGER.log(Level.WARNING, null, e);
			}
		}

		public void updateJobStatus(Job sim, String status) throws Exception {
			try {
				System.out.println("Update simulation status for simulation " + sim._jobNumber);
				Map<String, String> map = new HashMap<String, String>();
				map.put("method", "updateJobStatus");
				map.put("customer_id", sim._customerNumber);
				map.put("sim_no", sim._jobNumber.toString());
				map.put("update_job_status", status);
				String url = webAppBuildUrl(map);
				webAppHttpRequest(url);
			} catch (Exception e) {
				LOGGER.info("Unable to connect to webApp to update simulation status for simulation " + sim._jobNumber
						+ " by " + sim._customerNumber + " has been canceled. Keep simulation running");
				LOGGER.log(Level.WARNING, null, e);
			}
		}

		private String urlEncode(Map<String, String> hash) throws UnsupportedEncodingException {
			Iterator<Entry<String, String>> hashIt = hash.entrySet().iterator();
			Boolean addAmpersend = false;
			String url = "";
			while (hashIt.hasNext()) {
				Entry<String, String> entry = hashIt.next();
				if (addAmpersend) {
					url += "&";
				}
				url += URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
				addAmpersend = true;
			}
			return url;			
		}

		private String webAppBuildUrl(String method) throws UnsupportedEncodingException {
			Map<String, String> map = new HashMap<String, String>();
			map.put("method", method);
			return webAppBuildUrl(map);
		}

		private String webAppBuildUrl(Map<String, String> hash) throws UnsupportedEncodingException {
			String url = "https://trampo2app.herokuapp.com/a/trampo/simulations/?";
			Iterator<Entry<String, String>> hashIt = hash.entrySet().iterator();
			Boolean addAmpersend = false;
			while (hashIt.hasNext()) {
				Entry<String, String> entry = hashIt.next();
				if (addAmpersend) {
					url += "&";
				}
				url += URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
				addAmpersend = true;
			}
			return url;
		}

		private String webAppHttpRequest(String url) throws Exception {
			return webAppHttpRequest(url, null);
		}
		
		private String webAppHttpRequest(String url, String postdata) throws Exception {
			// send HTTP GET request
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			
			if (postdata == null) {
				//System.out.println("\nSending 'GET' request to URL : " + url);	
			} else {
				//System.out.println("\nSending 'POST' request to URL : " + url);
				con.setRequestMethod("POST");
				// con.setRequestProperty("User-Agent", USER_AGENT);
				con.setDoOutput(true);
				OutputStream os = con.getOutputStream();
				os.write(postdata.getBytes());
				os.flush();
				os.close();
			}

			// if responseCode = 200 is OK
			int responseCode = con.getResponseCode();
			//System.out.println("Response Code : " + responseCode);
			if (responseCode != 200) {
				throw new Exception("Unable to connect to server");
			}

			// Reading answer from Server if it need
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuffer response = new StringBuffer();
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			// System.out.println(response.toString());
			in.close();
			return response.toString();
		}
	}
	
	public static class WebAppGateDatabase {
		public String getJobStatus(Job sim) throws Exception {
			try {
				String result = null;
				Connection conn = getConnection();
				if (conn == null) {
					throw new Exception("No connection made");
				}
				PreparedStatement pstmt = conn.prepareStatement(
						"SELECT " + Database.STATUS + " FROM simulation WHERE customer_id=? AND no=?;");
				pstmt.setString(1, sim._customerNumber);
				pstmt.setInt(2, sim._jobNumber);
				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					result = rs.getString(Database.STATUS).trim();
				} else {
					LOGGER.info("Database no longer has information on simulation " + sim._jobNumber + " by "
							+ sim._customerNumber + " has been canceled. Keep simulation running");
					LOGGER.log(Level.SEVERE, "");
				}
				pstmt.close();
				conn.close();
				return result;
			} catch (SQLException e) {
				LOGGER.info("Unable to connect to database to check if simulation " + sim._jobNumber + " by "
						+ sim._customerNumber + " has been canceled. Keep simulation running");
				LOGGER.log(Level.WARNING, null, e);
			}
			return "";
		}

		public boolean isJobCanceled(Job sim) throws Exception {
			try {
				Connection conn = getConnection();
				if (conn == null) {
					throw new Exception("No connection made");
				}
				PreparedStatement pstmt = conn.prepareStatement(
						"SELECT " + Database.CANCELED_BY_USER + " FROM simulation WHERE customer_id=? AND no=?;");
				pstmt.setString(1, sim._customerNumber);
				pstmt.setInt(2, sim._jobNumber);
				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					return (rs.getInt(Database.CANCELED_BY_USER) == 1);
				} else {
					LOGGER.info("Database no longer has information on simulation " + sim._jobNumber + " by "
							+ sim._customerNumber + " has been canceled. Keep simulation running");
					LOGGER.log(Level.SEVERE, "");
				}
				pstmt.close();
				conn.close();
			} catch (SQLException e) {
				LOGGER.info("Unable to connect to database to check if simulation " + sim._jobNumber + " by "
						+ sim._customerNumber + " has been canceled. Keep simulation running");
				LOGGER.log(Level.WARNING, null, e);
			}
			return false;
		}

		public Integer getJobMaxRuntime(Job sim) throws Exception {
			try {
				Connection conn = getConnection();
				if (conn == null) {
					throw new Exception("No connection made");
				}
				PreparedStatement pstmt = conn.prepareStatement(
						"SELECT " + Database.MAX_RUNTIME + " FROM simulation WHERE customer_id=? AND no=?;");
				pstmt.setString(1, sim._customerNumber);
				pstmt.setInt(2, sim._jobNumber);
				ResultSet rs = pstmt.executeQuery();
				if (rs.next()) {
					return rs.getInt(Database.MAX_RUNTIME);
				} else {
					LOGGER.info("Database no longer has information on simulation " + sim._jobNumber + " by "
							+ sim._customerNumber + " has been canceled. Keep simulation running");
					LOGGER.log(Level.SEVERE, "");
				}
				pstmt.close();
				conn.close();
			} catch (SQLException e) {
				LOGGER.info("Unable to connect to database to check if simulation " + sim._jobNumber + " by "
						+ sim._customerNumber + " has been canceled. Keep simulation running");
				LOGGER.log(Level.WARNING, null, e);
			}
			return null;
		}

		public LinkedList<Job> getSimulations() throws Exception {
			return getSimulations(JobStatuses.SUBMITED);
		}

		public LinkedList<Job> getSimulations(String status) throws Exception {
			LinkedList<Job> simulations = new LinkedList<Job>();
			try {
				Connection conn = getConnection();
				if (conn == null) {
					throw new Exception("No connection made");
				}
				PreparedStatement pstmt = conn.prepareStatement(
						"SELECT * FROM simulation WHERE status=? ORDER BY sub_date asc, sub_time asc;");
				pstmt.setString(1, status);
				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					rs.getMetaData();
					simulations.add(new Job(rs.getInt(Database.SIMULATION_NO), rs.getString(Database.CUSTOMER_ID),
							rs.getString(Database.SUBMISSION_DATE), rs.getInt(Database.MAX_RUNTIME),
							rs.getString(Database.FILE_NAME), rs.getInt(Database.FILE_COUNT)));
				}
				rs.close();
				pstmt.close();
				conn.close();
			} catch (SQLException ex) {
				LOGGER.info("Unable to connect to database to query new simulation");
				LOGGER.log(Level.SEVERE, null, ex);
			}
			return simulations;
		}

		public LinkedList<String> getCustomerList() throws Exception {
			LinkedList<String> customerList = new LinkedList<String>();
			try {
				Connection conn = getConnection();
				if (conn == null) { throw new Exception("No connection made"); }
				PreparedStatement pstmt = conn.prepareStatement(
						"SELECT * FROM registered_users ORDER BY index;");
				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					rs.getMetaData();
					customerList.add(Integer.toString(rs.getInt("index")));
				}
				rs.close();
				pstmt.close();
				conn.close();
			} catch (SQLException ex) {
				LOGGER.info("Unable to connect to database to query new simulation");
				LOGGER.log(Level.SEVERE, null, ex);
			}
			return customerList;
		}

		public void updateJobActualRuntime(Job sim, Integer actualRuntime) throws Exception {
			try {
				System.out.println("Update simulation run time for simulation " + sim._jobNumber);
				Connection conn = getConnection();
				if (conn == null) {
					throw new Exception("No connection made");
				}
				PreparedStatement pstmt = conn
						.prepareStatement("UPDATE simulation SET act_runtime=? WHERE customer_id=? AND no=?;");
				pstmt.setInt(1, actualRuntime);
				pstmt.setString(2, sim._customerNumber);
				pstmt.setInt(3, sim._jobNumber);
				pstmt.executeUpdate();
				pstmt.close();
				conn.close();
			} catch (SQLException e) {
				LOGGER.info("Unable to connect to database for simulation " + sim._jobNumber + " by "
						+ sim._customerNumber + " to update runtime. Keep simulation running");
				LOGGER.log(Level.WARNING, null, e);
			}
		}

		public void updateJobStatus(Job sim, String status) throws Exception {
			try {
				System.out.println("Update simulation status for simulation " + sim._jobNumber + " to " + status);
				Connection conn = getConnection();
				if (conn == null) {
					throw new Exception("No connection made");
				}
				PreparedStatement pstmt = conn
						.prepareStatement("UPDATE simulation SET status=? WHERE customer_id=? AND no=?;");
				pstmt.setString(1, status);
				pstmt.setString(2, sim._customerNumber);
				pstmt.setInt(3, sim._jobNumber);
				pstmt.executeUpdate();
				pstmt.close();
				conn.close();
			} catch (SQLException e) {
				LOGGER.info("Unable to connect to database to check if simulation " + sim._jobNumber + " by "
						+ sim._customerNumber + " has been canceled. Keep simulation running");
				LOGGER.log(Level.WARNING, null, e);
			}
		}

		private Connection getConnection() {
			Connection conn = null;
			try {
				Class.forName("org.postgresql.Driver");
				conn = DriverManager.getConnection(Database.URL, Database.USERNAME, Database.PASSWORD);
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.log(Level.SEVERE, null, e);
			}
			return conn;
		}
	}
}
