package org.icube.owen.test.employee;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.icube.owen.ObjectFactory;
import org.icube.owen.employee.Employee;
import org.icube.owen.helper.UtilHelper;
import org.junit.Test;

public class EmployeeTest {
	Employee e = (Employee) ObjectFactory.getInstance("org.icube.owen.employee.Employee");
	int companyId = 1;

	@Test
	public void testGet() {
		Employee emp = new Employee();
		emp = e.get(companyId, 1);
		assertNotNull(emp.getEmployeeId());
		assertNotNull(emp.getFirstName());
		assertNotNull(emp.getLastName());
		assertNotNull(emp.getCompanyEmployeeId());
		assertNotNull(emp.getReportingManagerId());
		assertNotNull(emp.getScore());

	}

	@Test
	public void testGetImage() throws IOException, SQLException {
		Image image = e.getImage(1, 1);
		assertTrue(image instanceof Image);
		OutputStream out = null;
		int size = 0;
		try {
			out = new FileOutputStream(UtilHelper.getConfigProperty("test_image_get_path"));

		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
		byte[] b = new byte[size];
		try {
			out.write(b);
			ImageIO.write((RenderedImage) image, "jpg", out);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@Test
	public void testSaveImage() throws IOException {
		File sourceimage = new File(UtilHelper.getConfigProperty("test_image_save_path"));
		Image image = ImageIO.read(sourceimage);
		assertTrue(e.saveImage(1, 2, image));
	}
}
