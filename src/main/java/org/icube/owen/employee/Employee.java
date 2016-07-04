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

	private int employeeId;
	private String companyEmployeeId;
	private String firstName;
	private String lastName;
	private String reportingManagerId;
	private double score;
	private boolean active;
	private int companyId;
	private String companyName;
	private String grade; // can be high/medium/low
	private String function;
	private String position;
	private String zone;
	private boolean isFirstTimeLogin = false;

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

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
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

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public boolean isFirstTimeLogin() {
		return isFirstTimeLogin;
	}

	public void setFirstTimeLogin(boolean isFirstTimeLogin) {
		this.isFirstTimeLogin = isFirstTimeLogin;
	}

	/**
	 * Returns an employee object based on the employee ID given
	 * 
	 * @param employeeId - ID of the employee that needs to be retrieved
	 * @param companyId - Company ID of the employee 
	 * @return employee object
	 */
	public Employee get(int companyId, int employeeId) {
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		EmployeeList el = new EmployeeList();
		Employee e = new Employee();
		try {
			org.apache.log4j.Logger.getLogger(Employee.class).debug("get method started");
			CallableStatement cstmt = dch.companyConnectionMap.get(companyId).getSqlConnection().prepareCall("{call getEmployeeDetails(?)}");
			cstmt.setInt(1, employeeId);
			ResultSet res = cstmt.executeQuery();
			org.apache.log4j.Logger.getLogger(Employee.class).debug("query : " + cstmt);
			res.next();
			e = el.setEmployeeDetails(companyId, res);
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
		String imagePath = dch.companyConfigMap.get(companyId).getImagePath();
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
		org.apache.log4j.Logger.getLogger(Employee.class).debug("Entering the save employee image function");

		boolean imageSaved = false;
		DatabaseConnectionHelper dch = ObjectFactory.getDBHelper();
		dch.getCompanyConnection(companyId);
		String imagePath = dch.companyConfigMap.get(companyId).getImagePath();

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
		org.apache.log4j.Logger.getLogger(Employee.class).debug("Exiting the save employee image function" + imageSaved);
		return imageSaved;
	}

}