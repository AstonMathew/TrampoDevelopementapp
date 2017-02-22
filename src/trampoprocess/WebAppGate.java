package trampoprocess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import constants.Database;
import constants.SimulationStatuses;

/**
 * Interaction with the webapp to update simulation status, query simulations,
 * etc
 * 
 * @author julien
 *
 */
public class WebAppGate {
	private static final Logger LOGGER = Logger.getLogger(WebAppGate.class.getName() );
	
	public String getSimulationStatus(Job sim) throws Exception {
		try {
			String result = null;
			Connection conn = getConnection();
			if (conn == null) {
				throw new Exception("No connection made");
			}
			PreparedStatement pstmt = conn.prepareStatement("SELECT " + Database.STATUS + " FROM simulation WHERE customer_id=? AND no=?;");
			pstmt.setString(1, sim._customerNumber);
			pstmt.setInt(2, sim._jobNumber);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				result = rs.getString(Database.STATUS).trim();
			} else {
				LOGGER.info("Database no longer has information on simulation " + sim._jobNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running");
				LOGGER.log(Level.SEVERE, "");
			}
			pstmt.close();
			conn.close();
			return result;
		} catch (SQLException e) {
			LOGGER.info("Unable to connect to database to check if simulation " + sim._jobNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running");
			LOGGER.log(Level.WARNING, null, e);
		}
		return "";
	}

	public boolean isSimulationCanceled(Job sim) throws Exception {
		try {
			Connection conn = getConnection();
			if (conn == null) {
				throw new Exception("No connection made");
			}
			PreparedStatement pstmt = conn.prepareStatement("SELECT " + Database.CANCELED_BY_USER + " FROM simulation WHERE customer_id=? AND no=?;");
			pstmt.setString(1, sim._customerNumber);
			pstmt.setInt(2, sim._jobNumber);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return (rs.getInt(Database.CANCELED_BY_USER) == 1);
			} else {
				LOGGER.info("Database no longer has information on simulation " + sim._jobNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running");
				LOGGER.log(Level.SEVERE, "");
			}
			pstmt.close();
			conn.close();
		} catch (SQLException e) {
			LOGGER.info("Unable to connect to database to check if simulation " + sim._jobNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running");
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
			PreparedStatement pstmt = conn.prepareStatement("SELECT " + Database.MAX_RUNTIME + " FROM simulation WHERE customer_id=? AND no=?;");
			pstmt.setString(1, sim._customerNumber);
			pstmt.setInt(2, sim._jobNumber);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(Database.MAX_RUNTIME);
			} else {
				LOGGER.info("Database no longer has information on simulation " + sim._jobNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running");
				LOGGER.log(Level.SEVERE, "");
			}
			pstmt.close();
			conn.close();
		} catch (SQLException e) {
			LOGGER.info("Unable to connect to database to check if simulation " + sim._jobNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running");
			LOGGER.log(Level.WARNING, null, e);
		}
		return null;
	}

	public LinkedList<Job> getSimulations() throws Exception {
		return getSimulations(SimulationStatuses.SUBMITED);
	}

	public LinkedList<Job> getSimulations(String status) throws Exception {
		LinkedList<Job> simulations = new LinkedList<Job>();
		try {
			Connection conn = getConnection();
			if (conn == null) {
				throw new Exception("No connection made");
			}
			PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM simulation WHERE status=? ORDER BY sub_date asc, sub_time asc;");
			pstmt.setString(1, status);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				rs.getMetaData();
				simulations.add(new Job(rs.getInt(Database.SIMULATION_NO),
						rs.getString(Database.CUSTOMER_ID),
						rs.getString(Database.SUBMISSION_DATE),
						rs.getInt(Database.MAX_RUNTIME),
						rs.getString(Database.FILE_NAME),
						rs.getInt(Database.FILE_COUNT)));				
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
		return customerList;
	}
	
	public void updateJobActualRuntime(Job sim, Integer actualRuntime) throws Exception {
		try {
			System.out.println("Update simulation run time for simulation " + sim._jobNumber);
			Connection conn = getConnection();
			if (conn == null) {
				throw new Exception("No connection made");
			}
			PreparedStatement pstmt = conn.prepareStatement("UPDATE simulation SET act_runtime=? WHERE customer_id=? AND no=?;");
			pstmt.setInt(1, actualRuntime);
			pstmt.setString(2, sim._customerNumber);
			pstmt.setInt(3, sim._jobNumber);
            pstmt.executeUpdate();
			pstmt.close();
			conn.close();
		} catch (SQLException e) {
			LOGGER.info("Unable to connect to database to check if simulation " + sim._jobNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running");
			LOGGER.log(Level.WARNING, null, e);
		}
	}

	public void updateSimulationStatus(Job sim, String status) throws Exception {
		try {
			System.out.println("Update simulation status for simulation " + sim._jobNumber + " to " + status);
			Connection conn = getConnection();
			if (conn == null) {
				throw new Exception("No connection made");
			}
			PreparedStatement pstmt = conn.prepareStatement("UPDATE simulation SET status=? WHERE customer_id=? AND no=?;");
			pstmt.setString(1, status);
			pstmt.setString(2, sim._customerNumber);
			pstmt.setInt(3, sim._jobNumber);
            pstmt.executeUpdate();
			pstmt.close();
			conn.close();
		} catch (SQLException e) {
			LOGGER.info("Unable to connect to database to check if simulation " + sim._jobNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running");
			LOGGER.log(Level.WARNING, null, e);
		}
	}

	private Connection getConnection() {
		Connection conn = null;
		try {
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection(Database.URL,
					Database.USERNAME,
					Database.PASSWORD);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE,null,e);
		}
		return conn;
	}
}
