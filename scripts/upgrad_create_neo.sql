-- to add Employee node

LOAD CSV WITH HEADERS FROM "file:///C:/Users/Hitendra/Desktop/upgrad/csv/neo_vertexemployee.csv" AS row
CREATE (:Employee {emp_id:toInt(row.emp_id),emp_int_id:toInt(row.emp_int_id),FirstName:row.FirstName,LastName:row.LastName,Reporting_emp_id:toInt(row.Reporting_emp_id),cube_id:toInt(row.cube_id)});

/*to add function nodes*/
LOAD CSV WITH HEADERS FROM "file:///C:/Users/Hitendra/Desktop/upgrad/csv/neo_vertexFunction.csv" AS row
CREATE (:Function {Id:toInt(row.Id),Name:row.Name});

/* edge between employee and function */

LOAD CSV WITH HEADERS FROM "file:///C:/Users/Hitendra/Desktop/upgrad/csv/neo_edgedimension.csv" AS row
MATCH (a:Employee),(b:Function)
WHERE a.emp_id = toInt(row.emp_id) AND b.Name = row.Function
CREATE (a)-[r:has_functionality]->(b)
RETURN r;

/*to add Position nodes*/

LOAD CSV WITH HEADERS FROM "file:///C:/Users/Hitendra/Desktop/upgrad/csv/neo_vertexPosition.csv" AS row
CREATE (:Position {Id:toInt(row.Id),Name:row.Name});


/* edge between employee and Position */
LOAD CSV WITH HEADERS FROM "file:///C:/Users/Hitendra/Desktop/upgrad/csv/neo_edgedimension.csv" AS row
MATCH (a:Employee),(b:Position)
WHERE a.emp_id = toInt(row.emp_id) AND b.Name = row.Position
CREATE (a)-[r:is_positioned]->(b)
RETURN r

/*to add zone nodes*/

LOAD CSV WITH HEADERS FROM "file:///C:/Users/Hitendra/Desktop/upgrad/csv/neo_vertexZone.csv" AS row
CREATE (:Zone  {Id:toInt(row.Id),Name:row.Name});


/* edge between employee and Position */

LOAD CSV WITH HEADERS FROM "file:///C:/Users/Hitendra/Desktop/upgrad/csv/neo_edgedimension.csv" AS row
MATCH (a:Employee),(b:Zone)
WHERE a.emp_id = toInt(row.emp_id) AND b.Name = row.Zone
CREATE (a)-[r:from_zone]->(b)
RETURN r

