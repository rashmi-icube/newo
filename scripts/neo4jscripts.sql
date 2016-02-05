/*smart list for individual*/
/* emplistcomes from sql op*/

match (a:Employee)<-[r:relaition]-(b:Employee) where a.emp_id in emplist and b.emp_id in emplist
return a.emp_id as employeeId, a.FirstName as firstName, a.LastName as lastName,
a.Reporting_emp_id as reportingManagerId, a.emp_int_id as companyEmployeeId, count(r) as Score


