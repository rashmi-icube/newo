library(RNeo4j)
library(igraph)
library(moments)
library(RMySQL)
library(reshape2)

args = commandArgs(trailingOnly=TRUE)

setwd("C:\\Users\\Hitendra\\Desktop\\R metric Function")
source('config.R')

JobAlert=function(CompanyId){
  
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
  
#   com_neopath=CompanyConfig$neo_db_url[1]
#   com_neopath=paste(com_neopath,"/db/data/",sep = "")
#   
#   com_neousername=CompanyConfig$neo_user_name[1]
#   com_neopassword=CompanyConfig$neo_passsword[1]
#   
#   graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
#   
  query="select * from team_metric_value"
  
  res <- dbSendQuery(mydb,query)
  
  team_metric_value<- fetch(res,-1)
  
  query="select * from employee"
  
  res <- dbSendQuery(mydb,query)
  
  employee<- fetch(res,-1)
  
  query=paste("Select * from individual_metric_value where metric_id=3 
              order by metric_val_id desc Limit ",
              nrow(employee),sep = "")
  
  res <- dbSendQuery(mydb,query)
  
  retention_ind <- fetch(res,-1)
  
  query=paste("Select * from individual_metric_value where metric_id=5 
              order by metric_val_id desc Limit ",
              nrow(employee),sep = "")
  
  res <- dbSendQuery(mydb,query)
  
  sentiment_ind <- fetch(res,-1)
  
  team_metric_value$date=as.Date(team_metric_value$calc_time,format="%Y-%m-%d")
  
  team_metric_value=team_metric_value[,c("cube_id","metric_id","metric_value","date")]
  
  d1=dcast(team_metric_value, cube_id+metric_id~date, value.var="metric_value")
  
  d1$delta_n=d1[,ncol(d1)]-d1[,ncol(d1)-1]
  d1$delta_n_1=d1[,ncol(d1)-2]-d1[,ncol(d1)-3]
  d1$delta_n_2=d1[,ncol(d1)-4]-d1[,ncol(d1)-5]
  
  # variable
  
  query="SELECT * FROM variable;"
  res <- dbSendQuery(mydb,query)
  variable=fetch(res,-1)
  
  Threshold=variable$value[variable$variable_id==10]
  
  d1$a1=ifelse(d1$delta_n<=(Threshold*-1),1,0)
  d1$a2=ifelse(d1$delta_n<0 & d1$delta_n_1<0 & d1$delta_n_2<0,1,0)
  d1$a=ifelse(d1$a1==1,1,ifelse(d1$a2==1,1,0))
  dalert=d1[d1$a==1,]
  dalert$delta=ifelse(dalert$a1==0 & dalert$a2==1,dalert$delta_n+dalert$delta_n_1+dalert$delta_n_2,dalert$delta_n)
  
  for (i in 1:nrow(dalert)){
    # insert alert and get alert_id
    metric_id=dalert$metric_id[i]
    
    if(metric_id==8){
      cube_id=dalert$cube_id[i]
      emp_id_cube=employee$emp_id[employee$cube_id==cube_id]
      
      retention <-retention_ind[retention_ind$emp_id %in% emp_id_cube,]
      
      #to find people at risk based on Threshold
      PeopleAtRisk=retention$emp_id[retention$metric_value<=variable$value[variable$variable_name=="Metric8RiskThreshold"]]
      
    }
    
    if(metric_id==10){
      cube_id=dalert$cube_id[i]
      emp_id_cube=employee$emp_id[employee$cube_id==cube_id]
      
      sentiment <-sentiment_ind[sentiment_ind$emp_id %in% emp_id_cube,]
      
      #to find people at risk based on Threshold
      PeopleLowSen=sentiment$emp_id[sentiment$metric_value<=variable$value[variable$variable_name=="AlertSentimentThreshold"]]
      
    }
    
  }
  
  
  
  dbDisconnect(mydb)
}
