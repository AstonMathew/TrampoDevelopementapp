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
	public boolean isSimulationCanceled(Simulation sim) throws Exception {
		try {
			Connection conn = getConnection();
			if (conn == null) {
				throw new Exception("No connection made");
			}
			PreparedStatement pstmt = conn.prepareStatement("SELECT canceled_by_user FROM simulation WHERE customer_id=? AND no=?;");
			pstmt.setString(1, sim._customerNumber);
			pstmt.setInt(2, sim._simulationNumber);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return (rs.getInt("canceled_by_user") == 1);
			} else {
				Logger.getLogger("Database no longer has information on simulation " + sim._simulationNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running").log(Level.SEVERE, "");
			}
			pstmt.close();
			conn.close();
		} catch (SQLException e) {
			Logger.getLogger("Unable to connect to database to check if simulation " + sim._simulationNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running").log(Level.WARNING, null, e);
		}
		return false;
	}

	public LinkedList<Simulation> getSimulations() throws Exception {
		return getSimulations(SimulationStatuses.SUBMITED);
		/**
		LinkedList<Simulation> simulations = new LinkedList<Simulation>();
		try {
			Connection conn = getConnection();
			if (conn == null) {
				throw new Exception("No connection made");
			}
			PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM simulation WHERE status=? ORDER BY sub_date asc, sub_time asc;");
			pstmt.setString(1, SimulationStatuses.SUBMITED);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				rs.getMetaData();
				simulations.add(new Simulation(rs.getInt(Database.SIMULATION_NO),
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
			Logger.getLogger("Unable to connect to database to query new simulation").log(Level.SEVERE, null, ex);
		}
		return simulations; */
	}

	public LinkedList<Simulation> getSimulations(String status) throws Exception {
		LinkedList<Simulation> simulations = new LinkedList<Simulation>();
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
				simulations.add(new Simulation(rs.getInt(Database.SIMULATION_NO),
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
			Logger.getLogger("Unable to connect to database to query new simulation").log(Level.SEVERE, null, ex);
		}
		return simulations;
	}

	public void updateSimulationActualRuntime(Simulation sim, Integer actualRuntime) throws Exception {
		try {
			System.out.println("Update simulation run time for simulation " + sim._simulationNumber);
			Connection conn = getConnection();
			if (conn == null) {
				throw new Exception("No connection made");
			}
			PreparedStatement pstmt = conn.prepareStatement("UPDATE simulation SET act_runtime=? WHERE customer_id=? AND no=?;");
			pstmt.setInt(1, actualRuntime);
			pstmt.setString(2, sim._customerNumber);
			pstmt.setInt(3, sim._simulationNumber);
            pstmt.executeUpdate();
			pstmt.close();
			conn.close();
		} catch (SQLException e) {
			Logger.getLogger("Unable to connect to database to check if simulation " + sim._simulationNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running").log(Level.WARNING, null, e);
		}
	}

	public void updateSimulationStatus(Simulation sim, String status) throws Exception {
		try {
			System.out.println("Update simulation status for simulation " + sim._simulationNumber + " to " + status);
			Connection conn = getConnection();
			if (conn == null) {
				throw new Exception("No connection made");
			}
			PreparedStatement pstmt = conn.prepareStatement("UPDATE simulation SET status=? WHERE customer_id=? AND no=?;");
			pstmt.setString(1, status);
			pstmt.setString(2, sim._customerNumber);
			pstmt.setInt(3, sim._simulationNumber);
            pstmt.executeUpdate();
			pstmt.close();
			conn.close();
		} catch (SQLException e) {
			Logger.getLogger("Unable to connect to database to check if simulation " + sim._simulationNumber + " by " + sim._customerNumber + " has been canceled. Keep simulation running").log(Level.WARNING, null, e);
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
		}
		return conn;
	}
}
