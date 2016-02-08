/*smart list for individual*/


/* emplistcomes from sql op*/
call getListCollegue(emplist)


match (a:Employee)<-[r:relaition]-(b:Employee) where a.emp_id in emplist and b.emp_id in emplist
return a.emp_id as employeeId, a.FirstName as firstName, a.LastName as lastName,
a.Reporting_emp_id as reportingManagerId, a.emp_int_id as companyEmployeeId, count(r) as Score


/*get count of active and completyed by status category and initiative type*/
match (i:Init) where i.Status='Active' or i.Status='Completed' with  distinct(i.Status) as stat
match (z:Init) with distinct(z.Category) as cat,stat
match (j:Init {Category:cat}) with distinct(j.Type) as TYP,stat,cat
optional match (a:Init) where a.Status=stat and a.Type=TYP return cat as catergory,TYP as initiativeType,stat as status ,count(a) as totalInitiatives
