CREATE SCHEMA `upgrad` DEFAULT CHARACTER SET utf8 ;

use upgrad;

-- Update table values



/*create relationship master table*/



CREATE TABLE `relationship_master` (
  `rel_id` int(11) NOT NULL AUTO_INCREMENT,
  `rel_name` varchar(50) NOT NULL,
  PRIMARY KEY (`rel_id`)
);

INSERT INTO `relationship_master` VALUES (1,'innovation'),(2,'mentor'),(3,'social'),(4,'learning'),(5,'All');


/*create Frequency master table*/  

CREATE TABLE `frequency_master` (
  `freq_id` int(11) NOT NULL AUTO_INCREMENT,
  `freq_name` varchar(50) NOT NULL,
  PRIMARY KEY (`freq_id`)
);

INSERT INTO `frequency_master` VALUES (1,'weekly'),(2,'biweekly'),(3,'monthly'),(4,'quaterly');


/*create Survey Batch table*/

CREATE TABLE `survey_batch` (
  `survey_batch_id` int(11) NOT NULL AUTO_INCREMENT,
  `freq_id` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `target_init_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`survey_batch_id`),
  KEY `freq_id` (`freq_id`),
  CONSTRAINT `survey_batch_ibfk_1` FOREIGN KEY (`freq_id`) REFERENCES `frequency_master` (`freq_id`)
);

INSERT INTO `survey_batch` VALUES (1,1,'2016-06-06','2017-06-06',NULL);


/*create Survey question table*/

CREATE TABLE `question` (
  `que_id` int(11) NOT NULL AUTO_INCREMENT,
  `question` varchar(300) NOT NULL,
  `que_type` int(11) NOT NULL,
  `rel_id` int(11) DEFAULT NULL,
  `survey_batch_id` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  PRIMARY KEY (`que_id`),
  KEY `rel_id` (`rel_id`),
  KEY `survey_batch_id` (`survey_batch_id`),
  CONSTRAINT `question_ibfk_1` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`),
  CONSTRAINT `question_ibfk_2` FOREIGN KEY (`survey_batch_id`) REFERENCES `survey_batch` (`survey_batch_id`)
);


-- Insert question from csv query

LOAD DATA LOCAL INFILE 'C:/Users/Hitendra/Desktop/upgrad/csv/question.csv' 
INTO TABLE question
Character set 'latin1'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(que_id,question,que_type,@rel_id1,survey_batch_id,start_date,end_date)
SET rel_id = nullif(@rel_id1,'');


 /*create Dimension master table*/


CREATE TABLE `dimension_master` (
  `dimension_id` int(11) NOT NULL AUTO_INCREMENT,
  `dimension_name` varchar(50) NOT NULL,
  PRIMARY KEY (`dimension_id`)
);

INSERT INTO `dimension_master` VALUES (1,'Function'),(2,'Position'),(3,'Zone');


 /*create Dimension value table*/

CREATE TABLE `dimension_value` (
  `dimension_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `dimension_id` int(11) NOT NULL,
  `dimension_val_name` varchar(50) NOT NULL,
  PRIMARY KEY (`dimension_val_id`),
  KEY `dimension_id` (`dimension_id`),
  CONSTRAINT `dimension_value_ibfk_1` FOREIGN KEY (`dimension_id`) REFERENCES `dimension_master` (`dimension_id`)
);

LOAD DATA LOCAL INFILE 'C:/Users/Hitendra/Desktop/upgrad/csv/dimension_value.csv' 
INTO TABLE dimension_value
Character set 'latin1'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS;				


/* create employee table*/

CREATE TABLE `employee` (
  `emp_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_int_id` varchar(20) NOT NULL,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `reporting_emp_id` int(11) NOT NULL,
  `cube_id` int(11) NOT NULL,
  `que_status` bit(1) DEFAULT 0,
  `last_notified` datetime DEFAULT NULL,
  PRIMARY KEY (`emp_id`),
  KEY `Reporting_emp_id` (`reporting_emp_id`)
);


LOAD DATA LOCAL INFILE 'C:/Users/Hitendra/Desktop/upgrad/csv/employee.csv' 
INTO TABLE employee 
Character set 'latin1'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS;				





/*create metric master table*/
CREATE TABLE `metric_master` (
  `metric_id` int(11) NOT NULL AUTO_INCREMENT,
  `metric_name` tinytext NOT NULL,
  `query` text,
  `category` varchar(20) NOT NULL,
  `alert_statement` varchar(255) DEFAULT NULL,
  `alert_team` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`metric_id`)
);

INSERT INTO `metric_master` VALUES (1,'Expertise','','Individual',NULL,NULL),(2,'Mentorship','','Individual',NULL,NULL),(3,'Retention','','Individual',NULL,NULL),(4,'Influence','','Individual',NULL,NULL),(5,'Sentiment','','Individual',NULL,NULL),(6,'Performance','','Team','are indicating a decline in Performance','%s - %s - %s'),(7,'Social Cohesion','','Team','are indicating a decline in Social Cohesion','%s - %s - %s'),(8,'Retention','','Team','are indicating a decline in Retention','%s - %s - %s'),(9,'Innovation','','Team','are indicating a decline in Innovation','%s - %s - %s'),(10,'Sentiment','','Team','are indicating a decline in Sentiment','%s - %s - %s');

/*create cube master table*/

CREATE TABLE `cube_master` (
  `cube_id` int(11) NOT NULL AUTO_INCREMENT,
  `Function` char(50) DEFAULT NULL,
  `Position` char(50) DEFAULT NULL,
  `Zone` char(50) DEFAULT NULL,
  PRIMARY KEY (`cube_id`)
);


LOAD DATA LOCAL INFILE 'C:/Users/Hitendra/Desktop/upgrad/csv/cube_master.csv' 
INTO TABLE cube_master
Character set 'latin1'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS;		


/* create me response table*/

CREATE TABLE `me_response` (
  `me_response_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_id` int(11) NOT NULL,
  `que_id` int(11) NOT NULL,
  `response_time` datetime NOT NULL,
  `sentiment_weight` int(11) NOT NULL,
  `rel_id` int(11) DEFAULT NULL,
  `feedback` text,
  PRIMARY KEY (`me_response_id`),
  KEY `emp_id` (`emp_id`),
  KEY `que_id` (`que_id`),
  KEY `rel_id` (`rel_id`),
  CONSTRAINT `me_response_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `me_response_ibfk_2` FOREIGN KEY (`que_id`) REFERENCES `question` (`que_id`),
  CONSTRAINT `me_response_ibfk_3` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)
);



/* create we response table*/


CREATE TABLE `we_response` (
  `we_response_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_id` int(11) NOT NULL,
  `que_id` int(11) NOT NULL,
  `response_time` datetime NOT NULL,
  `target_emp_id` int(11) NOT NULL,
  `rel_id` int(11) DEFAULT NULL,
  `weight` int(11) NOT NULL,
  PRIMARY KEY (`we_response_id`),
  KEY `emp_id` (`emp_id`),
  KEY `target_emp_id` (`target_emp_id`),
  KEY `que_id` (`que_id`),
  KEY `rel_id` (`rel_id`),
  CONSTRAINT `we_response_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `we_response_ibfk_2` FOREIGN KEY (`target_emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `we_response_ibfk_3` FOREIGN KEY (`que_id`) REFERENCES `question` (`que_id`),
  CONSTRAINT `we_response_ibfk_4` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)
);



/*create team metric value table*/

CREATE TABLE `team_metric_value` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `cube_id` int(11) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  `display_flag` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`metric_val_id`),
  KEY `cube_id` (`cube_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `team_metric_value_ibfk_1` FOREIGN KEY (`cube_id`) REFERENCES `cube_master` (`cube_id`),
  CONSTRAINT `team_metric_value_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
);



 /*create individual metric value table*/ 

 CREATE TABLE `individual_metric_value` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_id` int(11) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  PRIMARY KEY (`metric_val_id`),
  KEY `emp_id` (`emp_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `individual_metric_value_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `individual_metric_value_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
);



/*create dimension (plane) metric value table*/ 

 CREATE TABLE `dimension_metric_value` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `dimension_val_id` int(11) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  `display_flag` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`metric_val_id`),
  KEY `dimension_val_id` (`dimension_val_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `dimension_metric_value_ibfk_1` FOREIGN KEY (`dimension_val_id`) REFERENCES `dimension_value` (`dimension_val_id`),
  CONSTRAINT `dimension_metric_value_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
);




 /*create initiative metric value table*/ 

CREATE TABLE `initiative_metric_value` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `initiative_id` int(11) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  `display_flag` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`metric_val_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `initiative_metric_value_ibfk_1` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
);


/*create initiative type master table*/ 

 
CREATE TABLE `initiative_type` (
  `init_type_id` int(11) NOT NULL AUTO_INCREMENT,
  `init_type` varchar(20) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `category` varchar(20) NOT NULL,
  PRIMARY KEY (`init_type_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `initiative_type_ibfk_1` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
);

INSERT INTO `initiative_type` VALUES (1,'Expertise',1,'Individual'),(2,'Mentorship',2,'Individual'),(3,'Retention',3,'Individual'),(4,'Influence',4,'Individual'),(5,'Sentiment',5,'Individual'),(6,'Performance',6,'Team'),(7,'Social Cohesion',7,'Team'),(8,'Retention',8,'Team'),(9,'Innovation',9,'Team'),(10,'Sentiment',10,'Team');


/* create table metric_relationship_map*/

CREATE TABLE `metric_relationship_map` (
  `metric_id` int(11) NOT NULL,
  `rel_id` int(11) DEFAULT NULL,
  KEY `rel_id` (`rel_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `metric_relationship_map_ibfk_1` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`),
  CONSTRAINT `metric_relationship_map_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
);

INSERT INTO `metric_relationship_map` VALUES (1,4),(2,2),(3,1),(3,2),(3,3),(3,4),(4,1),(5,NULL),(6,4),(7,3),(8,1),(8,2),(8,3),(8,4),(9,1),(10,NULL);


/*create login table*/ 

CREATE TABLE `login_table` (
  `login_id` varchar(32) NOT NULL,
  `password` varchar(20) NOT NULL,
  `emp_id` int(11) NOT NULL,
  `status` varchar(10) NOT NULL,
  PRIMARY KEY (`login_id`),
  KEY `emp_id` (`emp_id`),
  CONSTRAINT `login_table_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);


LOAD DATA LOCAL INFILE 'C:/Users/Hitendra/Desktop/upgrad/csv/login_table.csv' 
INTO TABLE login_table
Character set 'latin1'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS;		


/*create alert store table*/  	

CREATE TABLE `alert` (
  `alert_id` int(11) NOT NULL AUTO_INCREMENT,
  `cube_id` int(11) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `score` double DEFAULT NULL,
  `delta_score` double DEFAULT NULL,
  `people_count` int(11) NOT NULL,
  `alert_time` datetime NOT NULL,
  `status` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`alert_id`),
  KEY `cube_id` (`cube_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `alert_ibfk_1` FOREIGN KEY (`cube_id`) REFERENCES `cube_master` (`cube_id`),
  CONSTRAINT `alert_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
);



/*create alert people table to store peolpe tagged in alert*/	

CREATE TABLE `alert_people` (
  `alert_id` int(11) NOT NULL,
  `emp_id` int(11) NOT NULL,
  KEY `emp_id` (`emp_id`),
  KEY `alert_id` (`alert_id`),
  CONSTRAINT `alert_people_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `alert_people_ibfk_2` FOREIGN KEY (`alert_id`) REFERENCES `alert` (`alert_id`)
);






/*create table for nw metric master*/

CREATE TABLE `nw_metric_master` (
  `nw_metric_id` int(11) NOT NULL AUTO_INCREMENT,
  `nw_metric_name` tinytext NOT NULL,
  `query` text,
  `category` varchar(20) NOT NULL,
  PRIMARY KEY (`nw_metric_id`)
);

INSERT INTO `nw_metric_master` VALUES (1,'Degree','','Individual'),(2,'Strength','','Individual'),(3,'Betweenness','','Individual'),(4,'Closeness','','Individual'),(5,'Density','','Team'),(6,'Balance','','Team'),(7,'Average Path Length','','Team');


/*create team metric value table*/

CREATE TABLE `team_nw_metric_value` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `cube_id` int(11) NOT NULL,
  `nw_metric_id` int(11) NOT NULL,
  `nw_metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  `rel_id` int(11) NOT NULL,
  PRIMARY KEY (`metric_val_id`),
  KEY `cube_id` (`cube_id`),
  KEY `nw_metric_id` (`nw_metric_id`),
  Key `rel_id` (`rel_id`),
  CONSTRAINT `team_nw_metric_value_ibfk_1` FOREIGN KEY (`cube_id`) REFERENCES `cube_master` (`cube_id`),
  CONSTRAINT `team_nw_metric_value_ibfk_2` FOREIGN KEY (`nw_metric_id`) REFERENCES `nw_metric_master` (`nw_metric_id`),
  CONSTRAINT `team_nw_metric_value_ibfk_3` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)
);



 /*create individual metric value table*/ 

 CREATE TABLE `individual_nw_metric_value` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_id` int(11) NOT NULL,
  `nw_metric_id` int(11) NOT NULL,
  `nw_metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  `rel_id` int(11) NOT NULL,
    PRIMARY KEY (`metric_val_id`),
  KEY `emp_id` (`emp_id`),
  KEY `nw_metric_id` (`nw_metric_id`),
  Key `rel_id` (`rel_id`),
  CONSTRAINT `individual_nw_metric_value_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `individual_nw_metric_value_ibfk_2` FOREIGN KEY (`nw_metric_id`) REFERENCES `nw_metric_master` (`nw_metric_id`),
  CONSTRAINT `individual_nw_metric_value_ibfk_3` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)
);



/*create dimension (plane) metric value table*/ 

 
CREATE TABLE `dimension_nw_metric_value` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `dimension_val_id` int(11) NOT NULL,
  `nw_metric_id` int(11) NOT NULL,
  `nw_metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  `rel_id` int(11) NOT NULL,
  PRIMARY KEY (`metric_val_id`),
  KEY `dimension_val_id` (`dimension_val_id`),
  KEY `nw_metric_id` (`nw_metric_id`),
  Key `rel_id` (`rel_id`),
  CONSTRAINT `dimension_nw_metric_value_ibfk_1` FOREIGN KEY (`dimension_val_id`) REFERENCES `dimension_value` (`dimension_val_id`),
  CONSTRAINT `dimension_nw_metric_value_ibfk_2` FOREIGN KEY (`nw_metric_id`) REFERENCES `nw_metric_master` (`nw_metric_id`),
  CONSTRAINT `dimension_nw_metric_value_ibfk_3` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)
);



 /*create initiative metric value table*/ 

CREATE TABLE `initiative_nw_metric_value` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `initiative_id` int(11) NOT NULL,
  `nw_metric_id` int(11) NOT NULL,
  `nw_metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  `rel_id` int(11) NOT NULL,
  PRIMARY KEY (`metric_val_id`),
  KEY `nw_metric_id` (`nw_metric_id`),
  Key `rel_id` (`rel_id`),
  CONSTRAINT `initiative_nw_metric_value_ibfk_1` FOREIGN KEY (`nw_metric_id`) REFERENCES `nw_metric_master` (`nw_metric_id`),
  CONSTRAINT `initiative_nw_metric_value_ibfk_2` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)  
);



DROP TABLE IF EXISTS `employee_details`;

CREATE TABLE `employee_details` (
  `emp_id` int(11) NOT NULL,
  `salutation` varchar(10) DEFAULT NULL,
  `phone_no` varchar(20) DEFAULT NULL,
  `dob` date DEFAULT NULL,
  PRIMARY KEY (`emp_id`),
  CONSTRAINT `employee_details_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ;


LOAD DATA LOCAL INFILE 'C:/Users/Hitendra/Desktop/upgrad/csv/employee_details.csv' 
INTO TABLE employee_details
Character set 'latin1'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS;		


DROP TABLE IF EXISTS `variable`;

CREATE TABLE `variable` (
  `variable_id` int(11) NOT NULL AUTO_INCREMENT,
  `variable_name` varchar(50) NOT NULL,
  `value` double NOT NULL,
  PRIMARY KEY (`variable_id`)
);

INSERT INTO `variable` VALUES (1,'Metric5Window',6),(2,'Metric6Wt1',0.7),(3,'Metric6Wt2',0.3),(4,'Metric8RiskThreshold',30),(5,'Metric9Wt1',0.5),(6,'Metric9Wt2',0.5),(7,'Metric9InnovatorPercentile',0.2),(8,'MinTeamSize',5),(9,'decay',0.5),(10,'AlertDeltaThreshold',30),(11,'AlertSentimentThreshold',30),('12', 'Metric4Wt1', '0.5'),('13', 'Metric4Wt2', '0.5');


drop table if exists appreciation_response;

CREATE TABLE `appreciation_response` (
  `appreciation_response_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_id` int(11) NOT NULL,
  `response_time` datetime NOT NULL,
  `target_emp_id` int(11) NOT NULL,
  `rel_id` int(11) Default NULL,
  `weight` int(11) NOT NULL,
  PRIMARY KEY (`appreciation_response_id`),
  KEY `emp_id` (`emp_id`),
  KEY `target_emp_id` (`target_emp_id`),
  KEY `rel_id` (`rel_id`),
  CONSTRAINT `appreciation_response_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `appreciation_response_ibfk_2` FOREIGN KEY (`target_emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `appreciation_response_ibfk_4` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)
);




drop table if exists work_experience;

CREATE TABLE `work_experience` (
  `work_experience_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_id` int(11) NOT NULL,
  `organization_name` varchar(50) NOT NULL,
  `position` varchar(50) NOT NULL,
  `from_date` date NOT NULL,
  `to_date` date DEFAULT NULL,
  `location` varchar(50) NOT NULL,
  PRIMARY KEY (`work_experience_id`),
  KEY `emp_id` (`emp_id`),
  CONSTRAINT `work_experience_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);





drop table if exists education;

CREATE TABLE `education` (
  `education_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_id` int(11) NOT NULL,
  `institute_name` varchar(50) NOT NULL,
  `certification` varchar(50) NOT NULL,
  `from_date` date NOT NULL,
  `to_date` date DEFAULT NULL,
  `location` varchar(50) NOT NULL,
  PRIMARY KEY (`education_id`),
  KEY `emp_id` (`emp_id`),
  CONSTRAINT `education_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);



drop table if exists language_master;

CREATE TABLE `language_master` (
  `language_id` int(11) NOT NULL AUTO_INCREMENT,
  `language_name` varchar(20) NOT NULL,
  PRIMARY KEY (`language_id`)
);
 
INSERT INTO `language_master` VALUES (1,'Arabic'),(2,'Bengali'),(3,'Chinese'),(4,'English'),(5,'French'),(6,'German'),(7,'Gujarati'),(8,'Hindi'),(9,'Italian'),(10,'Japanese'),(11,'Javanese'),(12,'Korean'),(13,'Lahnda'),(14,'Marathi'),(15,'Portuguese'),(16,'Russian'),(17,'Spanish'),(18,'Tamil'),(19,'Telgu'),(20,'Urdu');


drop table if exists employee_language;

CREATE TABLE `employee_language` (
  `employee_language_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_id` int(11) NOT NULL,
  `language_id` int(11) NOT NULL,
  PRIMARY KEY (`employee_language_id`),
  KEY `emp_id` (`emp_id`),
  KEY `language_id` (`language_id`),
  CONSTRAINT `employee_language_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `employee_language_ibfk_2` FOREIGN KEY (`language_id`) REFERENCES `language_master` (`language_id`)
);




CREATE TABLE `batch_target` (
  `survey_batch_id` int(11) NOT NULL,
  `emp_id` int(11) NOT NULL,
  KEY `emp_id` (`emp_id`),
  KEY `survey_batch_id` (`survey_batch_id`),
  CONSTRAINT `batch_target_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `batch_target_ibfk_2` FOREIGN KEY (`survey_batch_id`) REFERENCES `survey_batch` (`survey_batch_id`)
);


LOAD DATA LOCAL INFILE 'C:/Users/Hitendra/Desktop/upgrad/csv/batch_target.csv' 
INTO TABLE batch_target
Character set 'latin1'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS;		



CREATE TABLE `login_log` (
  `emp_id` int(11) NOT NULL ,
  `login_time` datetime NOT NULL,
  `ip_address` varchar(50) NOT NULL,
  `role_id` int(11) NOT NULL ,
  KEY `emp_id` (`emp_id`),
  CONSTRAINT `login_log_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);



CREATE TABLE `role_master` (
  `role_id` int(11) NOT NULL AUTO_INCREMENT,
  `role` varchar(50) NOT NULL,
  PRIMARY KEY (`role_id`)
);


INSERT INTO `role_master` (`role_id`, `role`) VALUES ('1', 'Individual');
INSERT INTO `role_master` (`role_id`, `role`) VALUES ('2', 'HR');

CREATE TABLE `employee_role` (
  `emp_id` int(11) NOT NULL,
  `role_id` int(11) NOT NULL,
  KEY `role_id` (`role_id`),
  KEY `emp_id` (`emp_id`),
  CONSTRAINT `employee_role_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role_master` (`role_id`),
  CONSTRAINT `employee_role_ibfk_2` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`)
);


LOAD DATA LOCAL INFILE 'C:/Users/Hitendra/Desktop/upgrad/csv/employee_role.csv' 
INTO TABLE employee_role
Character set 'latin1'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS;		





-- Procedures



DELIMITER //
CREATE PROCEDURE getInitiativeTypelist
(IN cat varchar(20))
BEGIN
  SELECT init_type_id,init_type FROM initiative_type
  WHERE category= cat;
END //
DELIMITER ;




DELIMITER //
CREATE PROCEDURE getDimensionList()
BEGIN
  SELECT dimension_id,dimension_name FROM dimension_master;
END //
DELIMITER ;


DELIMITER //
CREATE PROCEDURE getDimensionValue
(IN dim_id int)
BEGIN
  SELECT dimension_val_id,dimension_val_name FROM dimension_value
  WHERE dimension_id= dim_id;
END //
DELIMITER ;


DELIMITER //
CREATE PROCEDURE getDimensionValueList()
BEGIN
  SELECT dimension_val_id,dimension_val_name,dimension_id FROM dimension_value;
END //
DELIMITER ;


DELIMITER //
CREATE PROCEDURE getMetricListForCategory
(IN cat varchar(20))
BEGIN
  SELECT metric_master.metric_id as metric_id,metric_master.metric_name as metric_name FROM initiative_type Left join metric_master
on initiative_type.metric_id=metric_master.metric_id where initiative_type.category=cat;  
END //
DELIMITER ;


DELIMITER //
CREATE PROCEDURE getInitiativePrimaryMetric
(IN init_id INT)
BEGIN
  SELECT metric_master.metric_id as metric_id,metric_master.metric_name as metric_name FROM initiative_type Left join metric_master
on initiative_type.metric_id=metric_master.metric_id where initiative_type.init_type_id=init_id;  
END //
DELIMITER ;



  
DELIMITER //
CREATE PROCEDURE getNeoConnectionUrl
(IN comp_id INT)
BEGIN
  SELECT db_url,port FROM neo_url where company_id=comp_id; 
END //
DELIMITER ;  


DELIMITER //
CREATE PROCEDURE getListColleague
(IN array VARCHAR(1000))
BEGIN
select emp_id from employee
where cube_id in (select DISTINCT(cube_id) from employee where FIND_IN_SET(emp_id, array));
END //
DELIMITER ;


DELIMITER //
CREATE PROCEDURE getBatchList()
BEGIN
SELECT survey_batch_id,survey_batch.freq_id,frequency_master.freq_name,start_date,end_date FROM survey_batch join frequency_master
on survey_batch.freq_id=frequency_master.freq_id;
END //
DELIMITER ;


DELIMITER //
CREATE PROCEDURE getBatchListById(
in batchid int)
BEGIN
SELECT survey_batch_id,survey_batch.freq_id,frequency_master.freq_name,start_date,end_date FROM survey_batch join frequency_master
on survey_batch.freq_id=frequency_master.freq_id where survey_batch.survey_batch_id=batchid;
END //
DELIMITER ;



drop procedure if exists getQuestionList;

DELIMITER //
CREATE PROCEDURE getQuestionList()
BEGIN
select * from (SELECT question.que_id,question.question,question.que_type,question.rel_id,question.survey_batch_id,
question.start_date,question.end_date,
round(count(distinct(me_response.emp_id))/(select count(emp_id) from employee)*100,2) as resp  FROM question LEft join me_response
on question.que_id=me_response.que_id where  question.que_type=0 group by question.que_id
union
SELECT question.que_id,question.question,question.que_type,question.rel_id,question.survey_batch_id,
question.start_date,question.end_date,
round(count(distinct(we_response.emp_id))/(select count(emp_id) from employee)*100,2) as resp  FROM question LEft join we_response
on question.que_id=we_response.que_id where  question.que_type=1  group by question.que_id) as t order by t.que_id;
END //
DELIMITER ;






drop procedure if exists getBatchQuestionList;

DELIMITER //
CREATE PROCEDURE getBatchQuestionList(
in batchid int)
BEGIN
select * from (SELECT question.que_id,question.question,question.que_type,question.rel_id,question.survey_batch_id,
question.start_date,question.end_date,
round(count(distinct(me_response.emp_id))/(select count(emp_id) from employee)*100,2) as resp  FROM question LEft join me_response
on question.que_id=me_response.que_id where  question.que_type=0 and question.survey_batch_id=batchid group by question.que_id
union
SELECT question.que_id,question.question,question.que_type,question.rel_id,question.survey_batch_id,
question.start_date,question.end_date,
round(count(distinct(we_response.emp_id))/(select count(emp_id) from employee)*100,2) as resp  FROM question LEft join we_response
on question.que_id=we_response.que_id where  question.que_type=1 and question.survey_batch_id=batchid  group by question.que_id) as t order by t.que_id;
END //
DELIMITER ;



drop procedure if exists getQuestion;


DELIMITER //
CREATE PROCEDURE getQuestion(
in queid int)
BEGIN
select * from (SELECT question.que_id,question.question,question.que_type,question.rel_id,question.survey_batch_id,
question.start_date,question.end_date,
round(count(distinct(me_response.emp_id))/(select count(emp_id) from employee)*100,2) as resp  FROM question LEft join me_response
on question.que_id=me_response.que_id where  question.que_type=0 and question.que_id=queid group by question.que_id
union
SELECT question.que_id,question.question,question.que_type,question.rel_id,question.survey_batch_id,
question.start_date,question.end_date,
round(count(distinct(we_response.emp_id))/(select count(emp_id) from employee)*100,2) as resp  FROM question LEft join we_response
on question.que_id=we_response.que_id where  question.que_type=1 and question.que_id=queid  group by question.que_id) as t order by t.que_id;
END //
DELIMITER ;


DELIMITER //
CREATE PROCEDURE updateBatch(
in batchid INT,
in freq INT,
in sdate Date ,
in edate Date 
)
BEGIN
update survey_batch 
set freq_id=freq,start_date=sdate,end_date=edate
where survey_batch_id=batchid;
END //
DELIMITER ;



DELIMITER //
CREATE PROCEDURE updateQuestionDate(
in queid INT,
in sdate Date,
in edate Date
)
BEGIN
update question 
set start_date=sdate,end_date=edate
where que_id=queid;
END //
DELIMITER ;


DELIMITER //
CREATE PROCEDURE getFrequencyList(
)
BEGIN
SELECT * FROM frequency_master;
END //
DELIMITER ;


DELIMITER //
CREATE PROCEDURE getResponseData(
in queid INT
)
BEGIN
select que_id,count(Distinct(emp_id)) responses,date(response_time) as date from me_response
where que_id=queid
group by date(response_time) 
union
select que_id,count(Distinct(emp_id)) as responses ,date(response_time) as date from we_response
where que_id=queid
group by date(response_time); 
END //
DELIMITER ;



DELIMITER //
CREATE PROCEDURE getCubeIdFromDimension(
in fun INT,
in pos INT,
in zon INT
)
BEGIN
select cube_id from (select t2.cube_id,t2.Function,t2.Position,dim2.dimension_val_id as Zone from (select t1.cube_id,t1.Function,dim1.dimension_val_id as Position,t1.Zone from (SELECT cube_master.cube_id,dimension_value.dimension_val_id as Function,cube_master.Position,cube_master.Zone
 FROM cube_master Left join dimension_value on cube_master.Function=dimension_value.dimension_val_name) as t1  
 left join dimension_value as dim1 on t1.Position=dim1.dimension_val_name) as t2 
 Left join  dimension_value as dim2 on t2.Zone=dim2.dimension_val_name) as t3 where t3.Function=fun and t3.Position=pos and t3.Zone=zon;
END //
DELIMITER ;


drop procedure if exists getTeamInitiativeMetricValueAggregate;

DELIMITER //
CREATE PROCEDURE getTeamInitiativeMetricValueAggregate
(IN initlist VARCHAR(1000))
BEGIN
select c.initiative_id,c.metric_id,c.current_score,p.previous_score,c.calc_time,metric_master.metric_name from 
(SELECT t1.initiative_id,t1.metric_id,case when t1.display_flag=1 then t1.metric_value else -1 end  as current_score,t1.calc_time  FROM initiative_metric_value t1
where FIND_IN_SET(t1.initiative_id,initlist) and t1.calc_time=(select max(t2.calc_time) from  initiative_metric_value  as t2
where t2.initiative_id=t1.initiative_id and  t1.metric_id=t2.metric_id)) as c,

(SELECT t1.initiative_id,t1.metric_id,case when t1.display_flag=1 then t1.metric_value else -1 end  as previous_score  FROM initiative_metric_value t1
where FIND_IN_SET(t1.initiative_id,initlist) and t1.calc_time=(select max(t2.calc_time) from  initiative_metric_value  as t2
where t2.initiative_id=t1.initiative_id and  t1.metric_id=t2.metric_id and (t2.calc_time NOT in
(select max(t3.calc_time) from  initiative_metric_value  as t3
where t3.initiative_id=t2.initiative_id and  t2.metric_id=t3.metric_id) or 1=(select count(distinct(t4.calc_time)) from  initiative_metric_value  as t4
where t4.initiative_id=t2.initiative_id and  t2.metric_id=t4.metric_id))
)) as p, metric_master
 where c.initiative_id=p.initiative_id and c.metric_id=p.metric_id and p.metric_id=metric_master.metric_id;

END //
DELIMITER ;

 

drop procedure if exists getIndividualInitiativeMetricValueAggregate;

DELIMITER //
CREATE PROCEDURE getIndividualInitiativeMetricValueAggregate
(IN emplist VARCHAR(1000))
BEGIN
select c.emp_id,c.metric_id,c.current_score,p.previous_score,c.calc_time,metric_master.metric_name from (SELECT t1.emp_id,t1.metric_id,t1.metric_value as current_score,t1.calc_time  FROM individual_metric_value as t1
where FIND_IN_SET(t1.emp_id,emplist) and t1.calc_time=(select max(t2.calc_time) from  individual_metric_value as t2
where t2.emp_id=t1.emp_id and  t1.metric_id=t2.metric_id)) as c,

(SELECT t1.emp_id,t1.metric_id,t1.metric_value as previous_score  FROM individual_metric_value as t1
where FIND_IN_SET(t1.emp_id,emplist) and t1.calc_time=(select max(t2.calc_time) from  individual_metric_value as t2
where t2.emp_id=t1.emp_id and  t1.metric_id=t2.metric_id and (t2.calc_time NOT in 
(select max(t3.calc_time) from  individual_metric_value as t3
where t3.emp_id=t2.emp_id and  t2.metric_id=t3.metric_id) or 1=(select count(distinct(t4.calc_time)) from  individual_metric_value  as t4
where t4.emp_id=t2.emp_id and  t2.metric_id=t4.metric_id))
)) as p,metric_master
where c.emp_id=p.emp_id and c.metric_id=p.metric_id and p.metric_id=metric_master.metric_id;
END //
DELIMITER ;


drop procedure if exists getOrganizationMetricValueAggregate;

DELIMITER //
CREATE PROCEDURE getOrganizationMetricValueAggregate()
BEGIN
select c.metric_id,c.current_score,c.average_score,p.previous_score,metric_master.metric_name,c.calc_time from 
(select t1.metric_id,t1.metric_value as current_score,t1.metric_value as average_score,t1.calc_time as calc_time from initiative_metric_value as t1 where t1.initiative_id=-1
order by t1.metric_val_id desc limit 5) as c,
(select t1.metric_id,t1.metric_value as previous_score,t1.calc_time from initiative_metric_value as t1 where t1.initiative_id=-1 
and t1.calc_time=(select max(t2.calc_time) from  initiative_metric_value  as t2
where t2.initiative_id=t1.initiative_id and  t1.metric_id=t2.metric_id and (t2.calc_time NOT in
(select max(t3.calc_time) from  initiative_metric_value  as t3
where t3.initiative_id=t2.initiative_id and  t2.metric_id=t3.metric_id) or 1=(select count(distinct(t4.calc_time)) from  initiative_metric_value  as t4
where t4.initiative_id=t2.initiative_id and  t2.metric_id=t4.metric_id))
)) as p,
metric_master
where c.metric_id=p.metric_id and p.metric_id=metric_master.metric_id;

END //
DELIMITER ;





drop procedure if exists getDimensionMetricValueAggregate;

DELIMITER //
CREATE PROCEDURE getDimensionMetricValueAggregate(
in dimvalid int,
in dimid int)
BEGIN
select c.metric_id,c.current_score,p.previous_score,a.average_score,metric_master.metric_name,c.calc_time from 
(select t1.metric_id,case when t1.display_flag=1 then t1.metric_value else -1 end as current_score,t1.calc_time as calc_time from dimension_metric_value as t1
where t1.dimension_val_id=dimvalid order by  t1.metric_val_id desc limit 5) as c,

(SELECT t1.metric_id,case when t1.display_flag=1 then t1.metric_value else -1 end  as previous_score  FROM dimension_metric_value t1
where t1.dimension_val_id=dimvalid and t1.calc_time=(select max(t2.calc_time) from  dimension_metric_value  as t2
where t2.dimension_val_id=t1.dimension_val_id and  t1.metric_id=t2.metric_id and (t2.calc_time NOT in
(select max(t3.calc_time) from  dimension_metric_value  as t3
where t3.dimension_val_id=t2.dimension_val_id and  t2.metric_id=t3.metric_id) or 1=(select count(distinct(t4.calc_time)) from  dimension_metric_value  as t4
where t4.dimension_val_id=t2.dimension_val_id and  t2.metric_id=t4.metric_id))
)) as p,

(select t3.metric_id,round(avg(t3.metric_value)) as average_score  from dimension_metric_value as t3
where t3.display_flag=1 and t3.dimension_val_id in (select dimension_value.dimension_val_id  from dimension_value where dimension_value.dimension_id=dimid) and calc_time=(select max(t4.calc_time) from dimension_metric_value as t4
where t4.dimension_val_id=t3.dimension_val_id and t4.metric_id=t3.metric_id)
group by t3.metric_id) as a,metric_master

where c.metric_id=p.metric_id and p.metric_id=a.metric_id and a.metric_id=metric_master.metric_id;
END //
DELIMITER ;


drop procedure if exists getTeamMetricValueAggregate;

DELIMITER //
CREATE PROCEDURE getTeamMetricValueAggregate(
in fun int,
in pos int,
in zon int
)
BEGIN
select c.metric_id,c.current_score,p.previous_score,a.average_score,metric_master.metric_name,c.calc_time from 

(select t.metric_id,m.metric_name,case when t.display_flag=1 then t.metric_value else -1 end  as current_score,t.calc_time from team_metric_value as t
left join metric_master as m on m.metric_id=t.metric_id
where t.cube_id=(select cube_id from cube_master where 
cube_master.Function=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=fun) and 
cube_master.Position=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=pos) and
cube_master.Zone=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=zon)) 
order by t.metric_val_id desc limit 5) as c,

(select t.metric_id,m.metric_name,case when t.display_flag=1 then t.metric_value else -1 end  as previous_score,t.calc_time from team_metric_value as t
left join metric_master as m on m.metric_id=t.metric_id
where t.cube_id=(select cube_id from cube_master where 
cube_master.Function=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=fun) and 
cube_master.Position=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=pos) and
cube_master.Zone=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=zon)) and 
t.calc_time=(select max(t2.calc_time) from  team_metric_value as t2
where t2.cube_id=t.cube_id and  t.metric_id=t2.metric_id and (t2.calc_time NOT in 
(select max(t3.calc_time) from  team_metric_value as t3
where t3.cube_id=t2.cube_id and  t2.metric_id=t3.metric_id) or 1=(select count(distinct(t4.calc_time)) from  team_metric_value  as t4
where t4.cube_id=t2.cube_id and  t2.metric_id=t4.metric_id))
))
 as p,

(select t.metric_id,m.metric_name,round(avg(t.metric_value)) as average_score,t.calc_time from team_metric_value as t
left join metric_master as m on m.metric_id=t.metric_id
where t.cube_id=(select cube_id from cube_master where 
cube_master.Function=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=fun) and 
cube_master.Position=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=pos) and
cube_master.Zone=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=zon))
group by t.metric_id
) as a,
metric_master
where c.metric_id=p.metric_id and p.metric_id=a.metric_id and a.metric_id=metric_master.metric_id;
END //
DELIMITER ;






DELIMITER //
CREATE PROCEDURE getAlertList()
begin
select a.alert_id,a.cube_id,c1.dimension_id_1,c1.dimension_name_1,c1.dimension_val_id_1,c1.dimension_val_name_1,
c1.dimension_id_2,c1.dimension_name_2,c1.dimension_val_id_2,c1.dimension_val_name_2,
c1.dimension_id_3,c1.dimension_name_3,c1.dimension_val_id_3,c1.dimension_val_name_3,
a.metric_id,metric_master.metric_name,metric_master.alert_team,metric_master.alert_statement,metric_master.category,a.score,a.delta_score,a.alert_time as calc_time,count(e.emp_id) as team_size,a.status,
i.init_type_id,i.init_type
 from alert as a left Join
(select c.cube_id,c.Function as dimension_val_name_1,d1.dimension_val_id as dimension_val_id_1,d1.dimension_id as dimension_id_1,dim1.dimension_name as dimension_name_1,
c.Position as dimension_val_name_2,d2.dimension_val_id as dimension_val_id_2,d2.dimension_id as dimension_id_2,dim2.dimension_name as dimension_name_2,
c.Zone as dimension_val_name_3,d3.dimension_val_id as dimension_val_id_3,d3.dimension_id as dimension_id_3,
dim3.dimension_name as dimension_name_3
from cube_master as c left join dimension_value as d1 on c.Function=d1.dimension_val_name 
left join dimension_value as d2  on c.Position=d2.dimension_val_name 
left join dimension_value as d3  on c.Zone=d3.dimension_val_name 
left join dimension_master as dim1 on d1.dimension_id=dim1.dimension_id
left join dimension_master as dim2 on d2.dimension_id=dim2.dimension_id
left join dimension_master as dim3 on d3.dimension_id=dim3.dimension_id) as c1
on c1.cube_id=a.cube_id
left join metric_master on a.metric_id=metric_master.metric_id
left join employee as e on a.cube_id=e.cube_id
left join  initiative_type as i on  a.metric_id=i.metric_id
where a.status='Active'
group by a.alert_id;
end ; //
delimiter ;






DELIMITER //
CREATE PROCEDURE getAlert(
in alertid int)
begin
select a.alert_id,a.cube_id,c1.dimension_id_1,c1.dimension_name_1,c1.dimension_val_id_1,c1.dimension_val_name_1,
c1.dimension_id_2,c1.dimension_name_2,c1.dimension_val_id_2,c1.dimension_val_name_2,
c1.dimension_id_3,c1.dimension_name_3,c1.dimension_val_id_3,c1.dimension_val_name_3,
a.metric_id,metric_master.metric_name,metric_master.alert_team,metric_master.alert_statement,metric_master.category,a.score,a.delta_score,a.alert_time as calc_time,count(e.emp_id) as team_size,a.status,
i.init_type_id,i.init_type
 from alert as a left Join
(select c.cube_id,c.Function as dimension_val_name_1,d1.dimension_val_id as dimension_val_id_1,d1.dimension_id as dimension_id_1,dim1.dimension_name as dimension_name_1,
c.Position as dimension_val_name_2,d2.dimension_val_id as dimension_val_id_2,d2.dimension_id as dimension_id_2,dim2.dimension_name as dimension_name_2,
c.Zone as dimension_val_name_3,d3.dimension_val_id as dimension_val_id_3,d3.dimension_id as dimension_id_3,
dim3.dimension_name as dimension_name_3
from cube_master as c left join dimension_value as d1 on c.Function=d1.dimension_val_name 
left join dimension_value as d2  on c.Position=d2.dimension_val_name 
left join dimension_value as d3  on c.Zone=d3.dimension_val_name 
left join dimension_master as dim1 on d1.dimension_id=dim1.dimension_id
left join dimension_master as dim2 on d2.dimension_id=dim2.dimension_id
left join dimension_master as dim3 on d3.dimension_id=dim3.dimension_id) as c1
on c1.cube_id=a.cube_id
left join metric_master on a.metric_id=metric_master.metric_id
left join employee as e on a.cube_id=e.cube_id
left join  initiative_type as i on  a.metric_id=i.metric_id
where a.status='Active' and a.alert_id=alertid
group by a.alert_id;
end ; //
delimiter ;



DELIMITER //
CREATE  PROCEDURE `getListOfPeopleForAlert`(
in alertid varchar(256)
)
BEGIN
select alert_people.alert_id,alert_people.emp_id,employee.first_name,employee.last_name from alert_people left join employee
on alert_people.emp_id=employee.emp_id
where FIND_IN_SET(alert_people.alert_id,alertid);
END//
DELIMITER ;



DELIMITER //
CREATE PROCEDURE deleteAlert(
in alertid INT
)
BEGIN
update alert
set status="Deleted"
where alert_id=alertid;
END //
DELIMITER ;



DELIMITER //
CREATE PROCEDURE getIndividualMetricTimeSeries(
in empid varchar(256)
)
BEGIN
SELECT  i.emp_id,i.metric_id,m.metric_name,i.metric_value as score,i.calc_time FROM individual_metric_value as i 
left join metric_master as m on m.metric_id=i.metric_id
where FIND_IN_SET(i.emp_id,empid);
END //
DELIMITER ;


drop procedure if exists getIndividualMetricValue;

DELIMITER //
CREATE PROCEDURE getIndividualMetricValue(
in empid varchar(256)
)
BEGIN
SELECT  i.emp_id,i.metric_id,m.metric_name,i.metric_value as current_score,i.calc_time FROM individual_metric_value as i 
left join metric_master as m on m.metric_id=i.metric_id
where FIND_IN_SET(i.emp_id,empid) and i.calc_time=(select max(t1.calc_time) from individual_metric_value as t1 where t1.emp_id=i.emp_id and 
t1.metric_id=i.metric_id);
END //
DELIMITER ;



drop procedure if exists getTeamMetricTimeSeries;

DELIMITER //
CREATE PROCEDURE getTeamMetricTimeSeries(
in fun int,
in pos int,
in zon int
)
BEGIN
select t.metric_id,m.metric_name,case when t.display_flag=1 then t.metric_value else -1 end   as score,t.calc_time from team_metric_value as t
left join metric_master as m on m.metric_id=t.metric_id
where t.cube_id=(select cube_id from cube_master where 
cube_master.Function=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=fun) and 
cube_master.Position=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=pos) and
cube_master.Zone=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=zon));
END //
DELIMITER ;



drop procedure if exists getTeamMetricValue;

DELIMITER //
CREATE PROCEDURE getTeamMetricValue(
in fun int,
in pos int,
in zon int
)
BEGIN
select t.metric_id,m.metric_name,case when t.display_flag=1 then t.metric_value else -1 end  as current_score,t.calc_time from team_metric_value as t
left join metric_master as m on m.metric_id=t.metric_id
where t.cube_id=(select cube_id from cube_master where 
cube_master.Function=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=fun) and 
cube_master.Position=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=pos) and
cube_master.Zone=(select d1.dimension_val_name from dimension_value as d1 where d1.dimension_val_id=zon)) and 
t.calc_time=(select max(t1.calc_time) from team_metric_value as t1 where t1.cube_id=t.cube_id and t1.metric_id=t.metric_id);
END //
DELIMITER ;



drop procedure if exists getDimensionMetricTimeSeries;

DELIMITER //
CREATE PROCEDURE getDimensionMetricTimeSeries(
in dimid int
)
BEGIN
SELECT dm.metric_id,mm.metric_name,case when dm.display_flag=1 then dm.metric_value else -1 end as score,dm.calc_time FROM dimension_metric_value as dm left join metric_master as mm
on dm.metric_id=mm.metric_id 
where dm.dimension_val_id=dimid;
END //
DELIMITER ;




drop procedure if exists getDimensionMetricValue;

DELIMITER //
CREATE PROCEDURE getDimensionMetricValue(
in dimid int
)
BEGIN
SELECT dm.metric_id,mm.metric_name,case when dm.display_flag=1 then dm.metric_value else -1 end as current_score,dm.calc_time FROM dimension_metric_value as dm left join metric_master as mm
on dm.metric_id=mm.metric_id 
where dm.dimension_val_id=dimid and dm.calc_time=(select max(calc_time) from dimension_metric_value as dm1 
where dm.dimension_val_id=dm1.dimension_val_id and dm.metric_id=dm1.metric_id);
END //
DELIMITER ;





DELIMITER //
CREATE PROCEDURE getOrganizationMetricTimeSeries()
BEGIN
SELECT im.metric_id,mm.metric_name,im.metric_value as score,im.calc_time FROM initiative_metric_value as im 
left join metric_master as mm on mm.metric_id=im.metric_id
where im.initiative_id=-1;
END //
DELIMITER ;


drop procedure if exists getOrganizationMetricValue;

DELIMITER //
CREATE PROCEDURE getOrganizationMetricValue()
BEGIN
SELECT im.metric_id,mm.metric_name,im.metric_value as current_score,im.calc_time FROM initiative_metric_value as im 
left join metric_master as mm on mm.metric_id=im.metric_id
where im.initiative_id=-1 and im.calc_time=(select max(im1.calc_time) from initiative_metric_value as im1 where 
im.initiative_id=im1.initiative_id and im.metric_id=im1.metric_id);
END //
DELIMITER ;



drop procedure if exists verifyLogin;

DELIMITER //
CREATE PROCEDURE verifyLogin(
in loginid varchar(32),
in pass varchar(32),
in curr_time datetime,
in ip varchar(50),
in roleid int
)
BEGIN
declare emp int;
if exists(select lt.emp_id  from login_table as lt
where Binary lt.login_id=loginid and Binary lt.password=pass and (lt.status='active' or lt.status='owen') and lt.emp_id in 
(select er.emp_id from employee_role as er where er.emp_id=lt.emp_id and er.role_id=roleid))
then 
set emp=(select lt.emp_id  from login_table as lt
where Binary lt.login_id=loginid and Binary lt.password=pass and (lt.status='active'  or lt.status='owen') and lt.emp_id in 
(select er.emp_id from employee_role as er where er.emp_id=lt.emp_id and er.role_id=roleid));
insert into login_log values (emp,curr_time,ip,roleid);
select emp as emp_id;
else
select 0 as emp_id;
end if;
END //
DELIMITER ;



DELIMITER //
CREATE PROCEDURE getEmpQuestionList(
in empid int,
in currdate date
)
BEGIN
SELECT * FROM question where 
que_id not in (select que_id from ((select Distinct(que_id) from me_response where emp_id=empid)
union (select Distinct(que_id) from we_response where emp_id=empid)) as t) and 
start_date<=currdate  and end_date>=currdate
and survey_batch_id in (select survey_batch_id from batch_target where
batch_target.emp_id=empid) ;
END //
DELIMITER ;


drop procedure if exists getEmpFromDimension;

DELIMITER //
CREATE PROCEDURE getEmpFromDimension(
in fun INT,
in pos INT,
in zon INT)
BEGIN
select employee.emp_id from employee,login_table where employee.emp_id=login_table.emp_id and login_table.status='active' and 
employee.cube_id in (select cube_id from (select t2.cube_id,t2.Function,t2.Position,dim2.dimension_val_id as Zone from (select t1.cube_id,t1.Function,dim1.dimension_val_id as Position,t1.Zone from (SELECT cube_master.cube_id,dimension_value.dimension_val_id as Function,cube_master.Position,cube_master.Zone
 FROM cube_master Left join dimension_value on cube_master.Function=dimension_value.dimension_val_name) as t1  
 left join dimension_value as dim1 on t1.Position=dim1.dimension_val_name) as t2 
 Left join  dimension_value as dim2 on t2.Zone=dim2.dimension_val_name) as t3
 where 
 (t3.Function=fun and t3.Position=pos and  t3.Zone=zon  ) or 
 (t3.Function=fun and t3.Position=pos and  zon=0) or 
 (t3.Function=fun and pos=0 and  t3.Zone=zon  ) or 
 (fun=0 and t3.Position=pos and  t3.Zone=zon  ) or 
 (t3.Function=fun and pos=0 and  zon=0  ) or 
 (fun=0 and t3.Position=pos and  zon=0  ) or 
 (fun=0 and pos=0 and  t3.Zone=zon  ) or 
 (fun=0 and pos=0 and  zon=0  ) 
 ) ;
END //
DELIMITER ;



DELIMITER //
CREATE PROCEDURE insertMeResponse(
in empid INT,
in queid INT,
in responsetime datetime,
in score int,
in relid int,
in feedbck varchar(512)
)
BEGIN
insert into me_response (emp_id,que_id,response_time,sentiment_weight,rel_id,feedback)
values (empid,queid,responsetime,score,relid,feedbck);
SELECT LAST_INSERT_ID() ;
END //
DELIMITER ;



DELIMITER //
CREATE PROCEDURE insertWeResponse(
in empid INT,
in queid INT,
in responsetime datetime,
in targetid int,
in relid int,
in wt int
)
BEGIN
insert into we_response (emp_id,que_id,response_time,target_emp_id,rel_id,weight)
values (empid,queid,responsetime,targetid,relid,wt);
SELECT LAST_INSERT_ID() ;
END //
DELIMITER ;



drop procedure if exists getEmployeeDetails;

DELIMITER //
CREATE PROCEDURE getEmployeeDetails(
in empid varchar(1000))
begin
SELECT e.emp_id,e.emp_int_id,e.first_name,e.last_name,e.reporting_emp_id,l.status,c.Function,c.Position,c.Zone from 
(select * from employee where FIND_IN_SET(emp_id,empid)) as e left join login_table as l on e.emp_id=l.emp_id 
left join cube_master as c on e.cube_id=c.cube_id;
end ; //
delimiter ;


drop procedure if exists getEmployeeList;

DELIMITER //
CREATE PROCEDURE getEmployeeList()
begin
SELECT e.emp_id,e.emp_int_id,e.first_name,e.last_name,e.reporting_emp_id,l.status,c.Function,c.Position,c.Zone
FROM employee as e left join login_table as l on e.emp_id=l.emp_id left join cube_master as c on c.cube_id=e.cube_id
where l.status='active';
end ; //
delimiter ;

drop procedure if exists getRelationTypeList;

DELIMITER //
CREATE PROCEDURE getRelationTypeList()
begin
SELECT * FROM relationship_master where rel_id<>5;
end ; //
delimiter ;


drop procedure if exists getIndividualMetricTimeSeriesForIndividual;


DELIMITER //
CREATE PROCEDURE getIndividualMetricTimeSeriesForIndividual(
in empid varchar(256)
)
BEGIN
SELECT  i.emp_id,i.metric_id,m.metric_name,i.metric_value as score,i.calc_time FROM individual_metric_value as i 
left join metric_master as m on m.metric_id=i.metric_id
where FIND_IN_SET(i.emp_id,empid) and i.metric_id in (1,2,4) ;
END //
DELIMITER ;


drop procedure if exists getIndividualMetricValueForIndividual;

DELIMITER //
CREATE PROCEDURE getIndividualMetricValueForIndividual(
in empid varchar(256)
)
BEGIN
SELECT  i.emp_id,i.metric_id,m.metric_name,i.metric_value as current_score,i.calc_time FROM individual_metric_value as i 
left join metric_master as m on m.metric_id=i.metric_id
where FIND_IN_SET(i.emp_id,empid) and i.metric_id in (1,2,4) and i.calc_time=(select max(t1.calc_time) from individual_metric_value as t1 where t1.emp_id=i.emp_id and 
t1.metric_id=i.metric_id);
END //
DELIMITER ;



drop procedure if exists getMetricRelationshipType;

DELIMITER //
CREATE PROCEDURE getMetricRelationshipType()
BEGIN
select 1 as metric_id,4 as rel_id
union
select 2 as metric_id,2 as rel_id
union 
select 4 as metric_id,1 as rel_id;
END //
DELIMITER ; 




drop procedure if exists getEmployeeBasicDetails;

DELIMITER //
CREATE PROCEDURE getEmployeeBasicDetails(
in empid int(11))
BEGIN
SELECT e.emp_id,e.emp_int_id,ed.salutation,e.first_name,e.last_name,ed.dob,l.login_id,ed.phone_no,c.Function,c.Position,c.Zone FROM employee as e left join employee_details as ed 
on e.emp_id=ed.emp_id left join login_table as l on e.emp_id=l.emp_id
left join cube_master as c on e.cube_id=c.cube_id where  e.emp_id=empid; 
END //
DELIMITER ;


drop procedure if exists getEmployeeWorkExperience;

DELIMITER //
CREATE PROCEDURE getEmployeeWorkExperience(
in empid int(11))
BEGIN
SELECT * FROM work_experience where emp_id=empid;
END //
DELIMITER ;


drop procedure if exists getEmployeeEducation;

DELIMITER //
CREATE PROCEDURE getEmployeeEducation(
in empid int(11))
BEGIN
SELECT * FROM education where emp_id=empid;
END //
DELIMITER ;



drop procedure if exists getLanguageList;

DELIMITER //
CREATE PROCEDURE getLanguageList()
BEGIN
SELECT * FROM language_master;
END //
DELIMITER ;



drop procedure if exists getEmployeeLanguage;

DELIMITER //
CREATE PROCEDURE getEmployeeLanguage(
in empid int(11))
BEGIN
SELECT el.*,lm.language_name FROM employee_language as el join language_master as lm on el.language_id=lm.language_id
where emp_id=empid;
END //
DELIMITER ;


/*update to transaction*/

drop procedure if exists updateEmployeeBasicDetails;

DELIMITER //
CREATE PROCEDURE updateEmployeeBasicDetails(
in empid int(11),
in phoneno varchar(20))
BEGIN
DECLARE phonenoExist varchar(50);
update employee_details set phone_no=phoneno where emp_id=empid;
select phone_no into  phonenoExist from employee_details where emp_id=empid;
if (row_count()=1) then
select 'TRUE' as op;
else select 'FALSE' as op;
end if;
END //
DELIMITER ;


DROP procedure if exists updateEmployeePassword;

DELIMITER //
CREATE PROCEDURE updateEmployeePassword(
in empid int(11),
in currentpwd varchar(50),
in newpwd varchar(20))
BEGIN
DECLARE checkpwd varchar(20);
select password into checkpwd from login_table where emp_id=empid;
if (checkpwd=currentpwd) then 
update login_table set password=newpwd where emp_id=empid;
if (row_count()=1) then
select 'TRUE' as op;
else select 'FALSE' as op; end if;
else select 'FALSE' as op; end if ;
END //
DELIMITER ;



DROP procedure if exists removeEducation;

DELIMITER //
CREATE PROCEDURE removeEducation(
in educationid INT
)
BEGIN
delete FROM education where education_id=educationid;
if (row_count()>=1) then select 'TRUE' as op;
else select 'FALSE' as op;
end if;
END //
DELIMITER ;


DROP procedure if exists removeWorkExperience;

DELIMITER //
CREATE PROCEDURE removeWorkExperience(
in work_experienceid INT
)
BEGIN
delete FROM work_experience where work_experience_id=work_experienceid;
if (row_count()>=1) then select 'TRUE' as op;
else select 'FALSE' as op;
end if;
END //
DELIMITER ;


DROP procedure if exists removeLanguage;

DELIMITER //
CREATE PROCEDURE removeLanguage(
in employee_languageid INT
)
BEGIN
delete FROM employee_language where employee_language_id=employee_languageid;
if (row_count()>=1) then select 'TRUE' as op;
else select 'FALSE' as op;
end if;
END //
DELIMITER ;


DROP procedure if exists insertWorkExperience;

DELIMITER //
CREATE PROCEDURE insertWorkExperience(
in emp_id_ip INT,
in organization_name_ip varchar(50),
in position_ip varchar(50),
in from_date_ip date,
in to_date_ip date,
in location_ip varchar(50)
)
BEGIN
INSERT INTO `work_experience` (`emp_id`, `organization_name`, `position`, `from_date`, `to_date`, `location`)
values (emp_id_ip,organization_name_ip,position_ip,from_date_ip,to_date_ip,location_ip);
if (row_count()>=1) then select 'TRUE' as op;
else select 'FALSE' as op;
end if;
END //
DELIMITER ;


DROP procedure if exists insertEducation;

DELIMITER //
CREATE PROCEDURE insertEducation(
in emp_id_ip INT,
in institute_name_ip varchar(50),
in certification_ip varchar(50),
in from_date_ip date,
in to_date_ip date,
in location_ip varchar(50)
)
BEGIN
INSERT INTO `education` (`emp_id`, `institute_name`, `certification`, `from_date`, `to_date`, `location`)
values (emp_id_ip,institute_name_ip,certification_ip,from_date_ip,to_date_ip,location_ip);
if (row_count()>=1) then select 'TRUE' as op;
else select 'FALSE' as op;
end if;
END //
DELIMITER ;


DROP procedure if exists insertLanguage;

DELIMITER //
CREATE PROCEDURE insertLanguage(
in emp_id_ip INT,
in language_id_ip int(11)
)
BEGIN
INSERT INTO `employee_language` (`emp_id`,`language_id`)
values (emp_id_ip,language_id_ip);
if (row_count()>=1) then select 'TRUE' as op;
else select 'FALSE' as op;
end if;
END //
DELIMITER ;



DROP procedure if exists insertAppreciation;

DELIMITER //
CREATE PROCEDURE insertAppreciation(
in empid INT,
in responsetime datetime,
in targetid int,
in relid int,
in wt int
)
BEGIN
insert into appreciation_response (emp_id,response_time,target_emp_id,rel_id,weight)
values (empid,responsetime,targetid,relid,wt);
if (row_count()>=1) then select 'TRUE' as op;
else select 'FALSE' as op;
end if;
END //
DELIMITER ;

 
drop procedure if exists getAppreciationActivity;
 
DELIMITER //
CREATE PROCEDURE getAppreciationActivity(
in empid INT
)
BEGIN
SELECT w.response_time as response_time,mm.metric_name as metric_name from we_response as w
left join metric_relationship_map as m on w.rel_id=m.rel_id left join metric_master as mm on mm.metric_id=m.metric_id
where w.target_emp_id=empid and w.rel_id in (1,2,4) and m.metric_id in (1,2,4)
union
SELECT w.response_time as response_time,mm.metric_name as metric_name from appreciation_response as w
left join metric_relationship_map as m on w.rel_id=m.rel_id left join metric_master as mm on mm.metric_id=m.metric_id
where w.target_emp_id=empid and w.rel_id in (1,2,4) and m.metric_id in (1,2,4) ;

END //
DELIMITER ;


DROP procedure if exists insertInitiativeMetricValue;

DELIMITER //
CREATE PROCEDURE insertInitiativeMetricValue(
in initiativeid INT,
in metricid int,
in metricvalue int,
in calctime datetime,
in noemp int
)
BEGIN

DECLARE threshold INT;
Declare dflag int;
 
SELECT value into threshold FROM variable where variable_id=8;

if (noemp<=threshold) then  set dflag=0; else set dflag=1; end if;

insert into initiative_metric_value (initiative_id,metric_id,metric_value,calc_time,display_flag)

values (initiativeid,metricid,metricvalue,calctime,dflag);
if (row_count()>=1) then select 'TRUE' as op;
else select 'FALSE' as op;
end if;
END //
DELIMITER ;


drop procedure if exists updateNotificationTime;

DELIMITER //
CREATE PROCEDURE updateNotificationTime(
in empid int,
in noti_time datetime
)
BEGIN
update employee
set last_notified=noti_time where emp_id=empid;
if (row_count()=1) then
select 'TRUE' as op;
else select 'FALSE' as op; end if;
END //
DELIMITER ;


-- drop procedure if exists getAppreciationActivityLatestCount;
 
-- DELIMITER //
-- CREATE PROCEDURE getAppreciationActivityLatestCount(
-- in empid INT
-- )
-- BEGIN
-- select sum(t.count_response) as appreciation_count,t1.last_notified as last_notified from (SELECT count(w.response_time) as count_response from we_response as w
-- left join metric_relationship_map as m on w.rel_id=m.rel_id
-- where w.target_emp_id=empid and w.rel_id in (1,2,4) and w.response_time>(select last_notified from employee where employee.emp_id=empid)
-- union
-- SELECT count(w.response_time) as count_response from appreciation_response as w
-- left join metric_relationship_map as m on w.rel_id=m.rel_id
-- where w.target_emp_id=empid and w.rel_id in (1,2,4) and w.response_time>(select last_notified from employee where employee.emp_id=empid)) as t,employee as t1 where t1.emp_id=empid;
-- END //
-- DELIMITER ;

drop procedure if exists getAppreciationActivityLatestCount;
delimiter //
CREATE PROCEDURE getAppreciationActivityLatestCount(
in empid INT
)
BEGIN
select sum(t.count_response) as appreciation_count,t1.last_notified as last_notified from (SELECT count(w.response_time) as count_response from we_response as w
where w.target_emp_id=empid and w.rel_id in (1,2,4) and w.response_time>(select last_notified from employee where employee.emp_id=empid)
union
SELECT count(w.response_time) as count_response from appreciation_response as w
where w.target_emp_id=empid and w.rel_id in (1,2,4) and w.response_time>(select last_notified from employee where employee.emp_id=empid)) as t,employee as t1 where t1.emp_id=empid;
END; //
delimiter ;




drop procedure if exists getBatch;

DELIMITER //
CREATE  PROCEDURE getBatch(
in batchid int)
BEGIN
SELECT survey_batch_id,survey_batch.freq_id,frequency_master.freq_name,start_date,end_date FROM survey_batch join frequency_master
on survey_batch.freq_id=frequency_master.freq_id where survey_batch.survey_batch_id=batchid;
END //
DELIMITER ;





drop procedure if exists getWallFeedTeam;

DELIMITER //
CREATE PROCEDURE getWallFeedTeam(
in fun int,
in pos int,
in zon int,
in page_no int,
in page_size int,
in top_bottom varchar(10),
in perc int,
in metricid int
)
BEGIN

declare n int;
declare offst int;
set offst=(page_no-1)*page_size;
set n=(select ceil(count(cube_id)*perc/100) from cube_master 
where 
case when fun<>0 then cube_master.Function=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=fun) else 1=1 end  and 
case when pos<>0 then cube_master.Position=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=pos) else 1=1 end and 
case when zon<>0 then cube_master.Zone=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=zon) else 1=1 end 
);

select t.cube_id,t.metric_id,t.metric_value,i.init_type_id,
dim1.dimension_id as dimension_id_1,d1.dimension_name as dimension_name_1,dim1.dimension_val_id as dimension_val_id_1,dim1.dimension_val_name as dimension_val_name_1,
dim2.dimension_id as dimension_id_2,d2.dimension_name as dimension_name_2,dim2.dimension_val_id as dimension_val_id_2,dim2.dimension_val_name as dimension_val_name_2,
dim3.dimension_id as dimension_id_3,d3.dimension_name as dimension_name_3,dim3.dimension_val_id as dimension_val_id_3,dim3.dimension_val_name as dimension_val_name_3
 from (select t1.cube_id,t1.metric_id,t1.metric_value,t1.display_flag from  team_metric_value t1 
where t1.metric_id=metricid and t1.calc_time=(select max(calc_time) from team_metric_value as t2 
where t2.metric_id=t1.metric_id and t2.cube_id=t1.cube_id) and 
cube_id in (select cube_id from cube_master where 
case when fun<>0 then cube_master.Function=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=fun) else 1=1 end  and 
case when pos<>0 then cube_master.Position=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=pos) else 1=1 end and 
case when zon<>0 then cube_master.Zone=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=zon) else 1=1 end 
)
order by (case when top_bottom='Top' then t1.metric_value end)  desc,
(case when top_bottom='Bottom' then t1.metric_value end) asc  limit n) as t 
left join cube_master as c on t.cube_id=c.cube_id 
left join dimension_value as dim1 on dim1.dimension_val_name=c.Function
left join dimension_value as dim2 on dim2.dimension_val_name=c.Position
left join dimension_value as dim3 on dim3.dimension_val_name=c.Zone
left join initiative_type as i on i.metric_id=t.metric_id
left join dimension_master as d1 on d1.dimension_id=dim1.dimension_id
left join dimension_master as d2 on d2.dimension_id=dim2.dimension_id
left join dimension_master as d3 on d3.dimension_id=dim3.dimension_id
where t.display_flag=1
order by (case when top_bottom='Top' then t.metric_value end)  desc,
(case when top_bottom='Bottom' then t.metric_value end) asc limit offst,page_size;
END //
DELIMITER ;


drop procedure if exists getWallFeedIndividual;

DELIMITER //
CREATE PROCEDURE getWallFeedIndividual(
in fun int,
in pos int,
in zon int,
in page_no int,
in page_size int,
in top_bottom varchar(10),
in perc int,
in metricid int
)
BEGIN

declare n int;
declare offst int;
set offst=(page_no-1)*page_size;
set n=(select ceil(count(emp_id)*perc/100) from employee 
where employee.cube_id in (select cube_id from cube_master where 
case when fun<>0 then cube_master.Function=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=fun) else 1=1 end  and 
case when pos<>0 then cube_master.Position=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=pos) else 1=1 end and 
case when zon<>0 then cube_master.Zone=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=zon) else 1=1 end 
));

select t.emp_id,t.metric_id,t.metric_value,i.init_type_id,e.first_name,e.last_name,c.Function,c.Position,c.Zone from 
(select t1.emp_id,t1.metric_id,t1.metric_value from  individual_metric_value t1,login_table 
where t1.metric_id=metricid and t1.calc_time=(select max(calc_time) from individual_metric_value as t2
where t2.metric_id=t1.metric_id and t2.emp_id=t1.emp_id) and t1.emp_id=login_table.emp_id and login_table.status='active' and
t1.emp_id in (select employee.emp_id from employee 
where employee.cube_id in (select cube_id from cube_master where 
case when fun<>0 then cube_master.Function=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=fun) else 1=1 end  and 
case when pos<>0 then cube_master.Position=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=pos) else 1=1 end and 
case when zon<>0 then cube_master.Zone=(select dimension_val_name from dimension_value 
where dimension_value.dimension_val_id=zon) else 1=1 end 
)
)
order by (case when top_bottom='Top' then t1.metric_value end)  desc,
(case when top_bottom='Bottom' then t1.metric_value end) asc  limit n) as t left join employee as e
on e.emp_id=t.emp_id left join cube_master as c on e.cube_id=c.cube_id
left join initiative_type as i on i.metric_id=t.metric_id
order by (case when top_bottom='Top' then t.metric_value end)  desc,
(case when top_bottom='Bottom' then t.metric_value end) asc limit offst,page_size;
END //
DELIMITER 



DROP procedure if exists getRoleList;

DELIMITER //
CREATE PROCEDURE getRoleList(
)
BEGIN
select * from role_master;
END //
DELIMITER ;
