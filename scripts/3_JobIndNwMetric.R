library(RMySQL)
library(RNeo4j)
library(igraph)
library(moments)

args = commandArgs(trailingOnly=TRUE)

setwd("C:\\Users\\Hitendra\\Desktop\\R metric Function")
source('config.R')


JobIndNwMetric=function(CompanyId){
   
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
  
  relquery="Select * from relationship_master"
  
  res <- dbSendQuery(mydb,relquery)
  relationship_master <- fetch(res)
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  
  querynode = "match (a:Employee) return a.emp_id"
  vertexlist=cypher(graph, querynode)
  
  
  op=data.frame(nw_metric_value=as.numeric(),emp_id=as.integer(),rel_id=as.integer(),nw_metric_id=as.integer())
  for (i in 1:nrow(relationship_master)){
    rel_id=relationship_master$rel_id[i]
    if (relationship_master$rel_name[i]!="All"){
      rel=relationship_master$rel_name[i]
    }else{
      rel="innovation|mentor|social|learning"
    }
    queryedge = paste("match (a:Employee),(b:Employee)
                      with a,b
                      match a-[r:",rel,"]->b
                      return a.emp_id as from ,b.emp_id as to ",sep="")
    
    edgelist = cypher(graph, queryedge)
    
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
    # degree
    deg=data.frame(degree(g,mode=c("in")))
    names(deg)="nw_metric_value"
    deg$emp_id=row.names(deg)
    deg$rel_id=rel_id
    deg$nw_metric_id=1
    op=rbind(op,deg)
    
    # betweenness
    
    between=data.frame(betweenness(g,normalized = FALSE))
    # not normalized
    names(between)="nw_metric_value"
    between$emp_id=row.names(between)
    between$rel_id=rel_id
    between$nw_metric_id=3
    op=rbind(op,between)
    
    #closeness
    clos=data.frame(closeness(g,normalized = TRUE))
    # normalized
    names(clos)="nw_metric_value"
    clos$emp_id=row.names(clos)
    clos$rel_id=rel_id
    clos$nw_metric_id=4
    op=rbind(op,clos)
    
    
    # strength
    queryedge = paste("match (a:Employee),(b:Employee)
                        with a,b
                      match a-[r:",rel,"]->b
                      return a.emp_id as from ,b.emp_id as to, r.weight as weight",sep="")
    
    edgelist = cypher(graph, queryedge)
    
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
    
    stren=data.frame(strength(g,mode=c("in")))
    names(stren)="nw_metric_value"
    stren$emp_id=row.names(stren)
    stren$rel_id=rel_id
    stren$nw_metric_id=2
    op=rbind(op,stren)  
    
  }
  
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  QueryTemp=" CREATE TABLE `individual_nw_metric_value_temp` (
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
  CONSTRAINT `individual_nw_metric_value_temp_ibfk_1` FOREIGN KEY (`emp_id`) REFERENCES `employee` (`emp_id`),
  CONSTRAINT `individual_nw_metric_value_temp_ibfk_2` FOREIGN KEY (`nw_metric_id`) REFERENCES `nw_metric_master` (`nw_metric_id`),
  CONSTRAINT `individual_nw_metric_value_temp_ibfk_3` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)
  );"
  
  dbGetQuery(mydb,QueryTemp)
  
  values <- paste("(",op$emp_id,",",op$nw_metric_id,",", op$nw_metric_value,",'",currtime,"',",op$rel_id,")", sep="", collapse=",")
  
  queryinsert <- paste("insert into individual_nw_metric_value_temp (emp_id,nw_metric_id,nw_metric_value,calc_time,rel_id) values ", values)
  
  dbGetQuery(mydb,queryinsert)
  
  query="insert into individual_nw_metric_value (emp_id,nw_metric_id,nw_metric_value,calc_time,rel_id)
  select emp_id,nw_metric_id,nw_metric_value,calc_time,rel_id from individual_nw_metric_value_temp"
  
  dbGetQuery(mydb,query)
  
  query="drop table individual_nw_metric_value_temp;"
  dbGetQuery(mydb,query)
  
  dbDisconnect(mydb)
  
}

JobIndNwMetric(args[1])