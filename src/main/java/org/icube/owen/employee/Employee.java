package org.icube.owen.employee;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class Employee extends TheBorg {

	// TODO: retrieve all employee details from SQL
	// TODO hpatel:  inactive employees to be filtered out 
	private int employeeId;
	private String companyEmployeeId;
	private String firstName;
	private String lastName;
	private String reportingManagerId;
	private long score;
	private boolean active;
	private int companyId;

	public int getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(int employeeId) {
		this.employeeId = employeeId;
	}

	public String getCompanyEmployeeId() {
		return companyEmployeeId;
	}

	public void setCompanyEmployeeId(String companyEmployeeId) {
		this.companyEmployeeId = companyEmployeeId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getReportingManagerId() {
		return reportingManagerId;
	}

	public void setReportingManagerId(String reportingManagerId) {
		this.reportingManagerId = reportingManagerId;
	}

	public long getScore() {
		return score;
	}

	public void setScore(long score) {
		this.score = score;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getCompanyId() {
		return companyId;
	}

	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	}

	/**
	 * Returns an employee object based on the employee ID given
	 * 
	 * @param employeeId - ID of the employee that needs to be retrieved
	 * @return employee object
	 */
	public Employee get(int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		EmployeeList el = new EmployeeList();
		Employee e = new Employee();
		try {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("get method started");
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getEmployeeDetails(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet res = cstmt.executeQuery();
			org.apache.log4j.Logger.getLogger(EmployeeList.class).debug("query : " + cstmt);
			res.next();
			e = el.setEmployeeDetails(res, false);
			org.apache.log4j.Logger.getLogger(Employee.class).debug(
					"Employee  : " + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getLastName());

		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(EmployeeList.class).error("Exception while retrieving employee object with employeeId : " + employeeId,
					e1);

		}
		return e;
	}
	
	
	//TODO return and save image file for employee image
	public Blob getImage(int companyId, int employeeId) throws IOException, SQLException{
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		String imagePath = dch.companyImagePath.get(companyId);
		
		File sourceimage = new File("c:\\mypic.jpg");
		Image image = ImageIO.read(sourceimage);

		
		URL url = new URL("http://www.mkyong.com/image/mypic.jpg");
		image = ImageIO.read(url);
		
		
		BufferedImage buffered = new BufferedImage(employeeId, employeeId, employeeId);
		buffered.getGraphics().drawImage(image, 0, 0 , null);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(buffered, "jpg", baos );
		byte[] imageInByte = baos.toByteArray();
		
		Blob blob = dch.companySqlConnectionPool.get(companyId).createBlob();
		blob.setBytes(1, imageInByte);
		return blob;
	}
}