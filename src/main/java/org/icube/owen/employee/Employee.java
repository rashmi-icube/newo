package org.icube.owen.employee;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.icube.owen.ObjectFactory;
import org.icube.owen.TheBorg;
import org.icube.owen.helper.DatabaseConnectionHelper;

public class Employee extends TheBorg {

	// TODO hpatel: inactive employees to be filtered out
	private int employeeId;
	private String companyEmployeeId;
	private String firstName;
	private String lastName;
	private String reportingManagerId;
	private double score;
	private boolean active;
	private int companyId;
	private String grade; // can be high/medium/low

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

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
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
			org.apache.log4j.Logger.getLogger(Employee.class).debug("get method started");
			CallableStatement cstmt = dch.mysqlCon.prepareCall("{call getEmployeeDetails(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet res = cstmt.executeQuery();
			org.apache.log4j.Logger.getLogger(Employee.class).debug("query : " + cstmt);
			res.next();
			e = el.setEmployeeDetails(res);
			org.apache.log4j.Logger.getLogger(Employee.class).debug(
					"Employee  : " + e.getEmployeeId() + "-" + e.getFirstName() + "-" + e.getLastName());

		} catch (SQLException e1) {
			org.apache.log4j.Logger.getLogger(Employee.class).error("Exception while retrieving employee object with employeeId : " + employeeId, e1);

		}
		return e;
	}

	/**
	 * Get the image of an employee
	 * @param companyId - companyId
	 * @param employeeId - employeeId
	 * @return image object
	 */
	public Image getImage(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		String imagePath = dch.companyImagePath.get(companyId);
		Image image = null;

		try {
			String filePath = imagePath + companyId + "_" + employeeId + ".jpg";
			org.apache.log4j.Logger.getLogger(Employee.class).debug("Path for retrieving the image for employeeId : " + filePath);
			File sourceimage = new File(filePath);
			image = ImageIO.read(sourceimage);
			org.apache.log4j.Logger.getLogger(Employee.class).debug("Successfully read the image for employeeId " + employeeId);
		} catch (IOException e) {
			org.apache.log4j.Logger.getLogger(Employee.class).error("Exception while retrieving employee image with employeeId : " + employeeId, e);
			return image;
		}

		return image;
	}

	/**
	 * Save the image of the given employee in a pre-defined format
	 * @param companyId - companyId
	 * @param employeeId - employeeId of the given employee
	 * @param image - Image object
	 * @return boolean value if the image is stored or not
	 */
	public boolean saveImage(int companyId, int employeeId, Image image) {
		boolean imageSaved = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		String imagePath = dch.companyImagePath.get(companyId);

		OutputStream out = null;
		int size = 0;
		try {
			out = new FileOutputStream(imagePath + companyId + "_" + employeeId + ".jpg");
			org.apache.log4j.Logger.getLogger(Employee.class).debug(
					"Path for the image to be stored : " + imagePath + companyId + "_" + employeeId + ".jpg");

		} catch (FileNotFoundException ex) {
			org.apache.log4j.Logger.getLogger(Employee.class).error("Exception while saving employee image with employeeId : " + employeeId, ex);
		}
		byte[] b = new byte[size];
		try {
			out.write(b);
			ImageIO.write((RenderedImage) image, "jpg", out);
			imageSaved = true;
			org.apache.log4j.Logger.getLogger(Employee.class).debug(
					"Image was successfully stored at " + imagePath + companyId + "_" + employeeId + ".jpg");
		} catch (Exception ex) {
			org.apache.log4j.Logger.getLogger(Employee.class).error("Exception while saving employee image with employeeId : " + employeeId, ex);
		}

		return imageSaved;
	}
}