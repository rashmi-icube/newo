library(RNeo4j)
library(igraph)
library(moments)
library(RMySQL)
library(reshape2)

args = commandArgs(trailingOnly=TRUE)

setwd("C:\\Users\\Hitendra\\Desktop\\R metric Function")
source('config.R')

JobIndividulaMetric=function(CompanyId){
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  query=paste("call getCompanyConfig(",CompanyId,");",sep = "")
  res <- dbSendQuery(mydb,query)
  CompanyConfig=fetch(res,-1)
  
  dbDisconnect(mydb)
  
  comp_sql_dbname=CompanyConfig$comp_sql_dbname[1]
  comp_sql_server=CompanyConfig$sql_server[1]
  comp_sql_user_id=CompanyConfig$sql_user_id[1]
  comp_sql_password=CompanyConfig$sql_password[1]
  
  mydb = dbConnect(MySQL(), user=comp_sql_user_id, password=comp_sql_password, dbname=comp_sql_dbname, host=comp_sql_server, port=mysqlport)
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  querynode = "match (a:Employee) return a.emp_id"
  
  vertexlist1=cypher(graph, querynode) 
  
  queryedge1 = "match (a:Employee)-[r]->(b:Employee) 
  return a.emp_id as from ,b.emp_id as to ,type(r) as relation,r.weight as weight"
  
  # master list of edge 
  edgelist1 = cypher(graph, queryedge1)
  
  # Expertise
  
  edgelist=edgelist1[edgelist1$relation=="learning",c("from","to","weight")]
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist1)
  
  op=data.frame(strength(g,mode="in"))
  
  names(op)="metric_value"
  
  op$emp_id=row.names(op)
  
  maxstrength=max(op$metric_value)
  
  op$metric_value=round(op$metric_value/maxstrength*100)
  
  op$metric_id=1

  op1=op
  # Mentorship
  
  edgelist=edgelist1[edgelist1$relation=="mentor",c("from","to","weight")]
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist1)
  
  op=data.frame(strength(g,mode="in"))
  
  names(op)="metric_value"
  
  op$emp_id=row.names(op)
  
  maxstrength=max(op$metric_value)
  
  op$metric_value=round(op$metric_value/maxstrength*100)
  
  op$metric_id=2
  
  op1=rbind(op1,op)
  # Retention
  
  #all relation
  
  query="select * from individual_nw_metric_value as t1
  where t1.nw_metric_id=2 and t1.rel_id=5"
  
  res <- dbSendQuery(mydb,query)
  # individual retention
  strengthall <- fetch(res,-1)
  
  strengthall$calc_time=strptime(strengthall$calc_time,"%Y-%m-%d %H:%M:%S")
  
  uniquetimestamp=unique(strengthall$calc_time)
  
  uniquedate=as.Date(as.POSIXct(uniquetimestamp))
  uniquedate=sort(uniquedate,decreasing = TRUE)
  
  currdate=uniquedate[1]
  previousdate=uniquedate[2]
  
  strengthall$date=as.Date(as.POSIXct(strengthall$calc_time))
  
  currstrength=strengthall[strengthall$date==currdate,c("emp_id","nw_metric_value")]
  
  prevstrength=strengthall[strengthall$date==previousdate,c("emp_id","nw_metric_value")]
  
  instrength=merge(currstrength,prevstrength,by="emp_id")
  names(instrength)[2:3]=c("current","previous")
  
  instrength$perc=(instrength$current-instrength$previous)/instrength$previous  
  instrength$perc=instrength$perc+1
  
  instrength$perc[instrength$perc>1]=1
  
  instrength$metric_value=round(instrength$perc*100,0)
  
  op=instrength[,c("metric_value","emp_id")]
  
  op$metric_id=3
  
  op1=rbind(op1,op)
  
  # Influence
  
  edgelist=edgelist1[edgelist1$relation=="innovation",c("from","to")]
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist1)
  
  op=data.frame(betweenness(g))
  names(op)="betweenness"
  op$emp_id=row.names(op)
  
  maxbetw=max(op$betweenness)
  op$betweenness=op$betweenness/maxbetw
  op$metric_value=round(op$betweenness*100,0)
  
  op=op[,c("metric_value","emp_id")]
  
  op$metric_id=4
  
  op1=rbind(op1,op)
  
  #sentiment
  
  query="select * from me_response"
  
  res <- dbSendQuery(mydb,query)
  # individual retention
  me_repsonse <- fetch(res,-1)
  
  query="SELECT * FROM variable;"
  res <- dbSendQuery(mydb,query)
  variable=fetch(res,-1)
  
  questionlist=unique(me_repsonse$que_id)
  questionlist=sort(questionlist,decreasing = TRUE)
  
  questionlist=questionlist[1:variable$value[variable$variable_name=="Metric5Window"]]
  
  me_repsonse=me_repsonse[me_repsonse$que_id %in% questionlist,]
  op=aggregate(me_repsonse$sentiment_weight,by=list(emp_id=me_repsonse$emp_id),mean)
  
  op$metric_value=round(op$x/5*100,0)
  
  op=op[,c("metric_value","emp_id")]
  
  op$metric_id=5
  
  op1=rbind(op1,op)
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  query=" CREATE TABLE `individual_metric_value_temp` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `emp_id` int(11) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  PRIMARY KEY (`metric_val_id`),
  KEY `emp_id` (`emp_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `individual_metric_value_temp_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `individual_metric_value_temp_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
  );"
  
  dbGetQuery(mydb,query)
  
  values <- paste("(",op1$emp_id,",",op1$metric_id,",", op1$metric_value,",'",currtime,"')", sep="", collapse=",")
  
  queryinsert <- paste("insert into individual_metric_value_temp (emp_id,metric_id,metric_value,calc_time) values ", values)
  
  dbGetQuery(mydb,queryinsert)
  
  query="insert into individual_metric_value (emp_id,metric_id,metric_value,calc_time)
  select emp_id,metric_id,metric_value,calc_time from individual_metric_value_temp"
  
  dbGetQuery(mydb,query)
  
  query="drop table individual_metric_value_temp;"
  dbGetQuery(mydb,query)
  
  dbDisconnect(mydb)
}

JobIndividulaMetric(args[1])