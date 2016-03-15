library(RMySQL)
library(RNeo4j)
library(igraph)
library(moments)

args = commandArgs(trailingOnly=TRUE)

setwd("C:\\Users\\Hitendra\\Desktop\\R metric Function")
source('config.R')

JobDimensionNwMetric=function(CompanyId){
  
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
  
  empquery="Select * from employee"
  
  res <- dbSendQuery(mydb,empquery)
  employee <- fetch(res)
  
  cubequery="Select * from cube_master"
  
  res <- dbSendQuery(mydb,cubequery)
  cube_master <- fetch(res)
  
  relquery="Select * from relationship_master"
  
  res <- dbSendQuery(mydb,relquery)
  relationship_master <- fetch(res)
  
  relquery="Select * from dimension_value"
  
  res <- dbSendQuery(mydb,relquery)
  dimension_value <- fetch(res)
  
  relquery="Select * from dimension_master"
  
  res <- dbSendQuery(mydb,relquery)
  dimension_master <- fetch(res)
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  querynode = "match (a:Employee) return a.emp_id"
  vertexlist=cypher(graph, querynode)
  
  queryedge = "match (a:Employee),(b:Employee)
  with a,b
  match a-[r]->b
  return a.emp_id as from ,b.emp_id as to,type(r) as Relation_Type,r.weight as weight"
  
  edgelist = cypher(graph, queryedge)
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  op=data.frame(nw_metric_value=as.numeric(),dimension_val_id=as.integer(),rel_id=as.integer(),nw_metric_id=as.integer())
  for (i in 1:nrow(dimension_value)){
    dimension_val_id=dimension_value$dimension_val_id[i]
    dimension_val_name=dimension_value$dimension_val_name[i]
    dimension_id=dimension_value$dimension_id[i]
    dimension_name=dimension_master$dimension_name[dimension_master$dimension_id==dimension_id]
    cube_id_list=cube_master$cube_id[cube_master[,dimension_name]==dimension_val_name]
    emplist=employee$emp_id[employee$cube_id %in% cube_id_list]
    for (j in 1:nrow(relationship_master)){
      rel_id=relationship_master$rel_id[j]
      if (relationship_master$rel_name[j]!="All"){
        rel=relationship_master$rel_name[j]
        g1<- induced_subgraph(g, which(V(g)$name %in% emplist))
        g2=subgraph.edges(g1, which(E(g1)$Relation_Type==rel), delete.vertices = FALSE)
      }else{
        g1<- induced_subgraph(g, which(V(g)$name %in% emplist))
        vert=get.vertex.attribute(g1)
        edg=data.frame(get.edgelist(g1))
        edg=unique(edg)
        g2 <- graph.data.frame(edg, directed=TRUE,vertices = vert)
        
      }
      # density
      nw_metric_value=graph.density(g2)
      op=rbind(op,data.frame(nw_metric_value,dimension_val_id,rel_id,nw_metric_id=5))
      
      #Balance
      instrength=strength(g2,mode="in")
      nw_metric_value=skewness(instrength)
      if(is.nan(nw_metric_value)){
        nw_metric_value=3
      }
      op=rbind(op,data.frame(nw_metric_value,dimension_val_id,rel_id,nw_metric_id=6))
      
      # Average Path Length
      nw_metric_value=average.path.length(g1)
      op=rbind(op,data.frame(nw_metric_value,dimension_val_id,rel_id,nw_metric_id=7))
    }
  }
  
  op[is.na(op)] <- 0
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  queryTemp="CREATE TABLE `dimension_nw_metric_value_temp` (
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
  CONSTRAINT `dimension_nw_metric_value_temp_ibfk_1` FOREIGN KEY (`dimension_val_id`) REFERENCES `dimension_value` (`dimension_val_id`),
  CONSTRAINT `dimension_nw_metric_value_temp_ibfk_2` FOREIGN KEY (`nw_metric_id`) REFERENCES `nw_metric_master` (`nw_metric_id`),
  CONSTRAINT `dimension_nw_metric_value_temp_ibfk_3` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)
  );"
  
  dbGetQuery(mydb,queryTemp)
  
  values <- paste("(",op$dimension_val_id,",",op$nw_metric_id,",", op$nw_metric_value,",'",currtime,"',",op$rel_id,")", sep="", collapse=",")
  
  queryinsert <- paste("insert into dimension_nw_metric_value_temp (dimension_val_id,nw_metric_id,nw_metric_value,calc_time,rel_id) values ", values)
  
  dbGetQuery(mydb,queryinsert)
  
  query="insert into dimension_nw_metric_value (dimension_val_id,nw_metric_id,nw_metric_value,calc_time,rel_id)
  select dimension_val_id,nw_metric_id,nw_metric_value,calc_time,rel_id from dimension_nw_metric_value_temp;"
  
  dbGetQuery(mydb,query)
  
  query="drop table dimension_nw_metric_value_temp;"
  dbGetQuery(mydb,query)
  
  dbDisconnect(mydb)
  
}

JobDimensionNwMetric(args[1])