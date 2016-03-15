library(RMySQL)
library(dplyr)

setwd("C:\\Users\\Hitendra\\Desktop\\R metric Function")
source('config.R')

args = commandArgs(trailingOnly=TRUE)

calcualte_edge=function(CompanyId){
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
  
  query="SELECT * FROM variable where variable_id=9;"
  res <- dbSendQuery(mydb,query)
  decay=fetch(res,-1)
  decay=decay$value[1]
  
  query="SELECT * FROM question where survey_batch_id=1 and que_type=1;"
  res <- dbSendQuery(mydb,query)
  question=fetch(res,-1)
  
  question$start_date=as.Date(question$start_date,format="%Y-%m-%d")
  question$end_date=as.Date(question$end_date,format="%Y-%m-%d")
  question$end_date=as.Date(question$end_date,format="%Y-%m-%d")
  
  question=question[question$end_date<Sys.Date(),]
  
  question <- question %>%
    group_by(rel_id) %>%
    top_n(n = 4, wt = que_id)
  
  query=paste("SELECT * FROM we_response where que_id in (",paste(question$que_id,collapse = ","),")")
  res <- dbSendQuery(mydb,query)
  we_response=fetch(res,-1)
  
  we_response$response_time=strptime(we_response$response_time,format="%Y-%m-%d  %H:%M:%S")
  
  startdate=min(question$start_date)
  
  query=paste("SELECT * FROM appreciation_response where date(response_time)>'",startdate,"'",sep = "")
  res <- dbSendQuery(mydb,query)
  appreciation_response=fetch(res,-1)
  
  for  (j in 1:4){
    rel_id=j
    que_sub=question[question$rel_id==j,]
    que_sub=que_sub[order(que_sub$que_id),]
    for (k in 1:nrow(que_sub)){
      que_id=que_sub$que_id[k]
      startdate=que_sub$start_date[k]
      appreciation_response$que_id[appreciation_response$rel_id==j & 
                                     appreciation_response$response_time>=startdate]=que_id
    }
  }
  
  appreciation_response=appreciation_response[!is.na(appreciation_response$que_id),]
  
  response=rbind(we_response[,c("emp_id","que_id","response_time","target_emp_id","rel_id","weight")],
                 appreciation_response[,c("emp_id","que_id","response_time","target_emp_id","rel_id","weight")])
  
  emp_id_list=unique(response$emp_id)
  
  decayfun=function(D,currdecay,que){
    if (length(que_rel)>=que){
      currque=que_rel[que]
      D1=D[D$que_id==currque,]
      if(nrow(D1)>0){
        currweight=D1$weight[D1$response_time==max(D1$response_time)]
      }else{
        currweight=0
      }
      score=currweight*currdecay+decayfun(D,currdecay*decay,que+1)
      return(score)
    }else{
      return(0)
    }
  }
  
  opcal=data.frame(emp_id=as.numeric(),target_emp_id=as.numeric(),
                   rel_id=as.numeric(),weight=as.numeric())
  
  for ( j in 1:4){
    currrel=j
    que_rel=question$que_id[question$rel_id==currrel]
    que_rel=sort(que_rel,decreasing = TRUE)
    decayweight=0
    for(x in 1:length(que_rel)){
      decayweight=decayweight+decay^(x-1)
    }
    emp_id_list=unique(response$emp_id[response$rel_id==currrel])
    for ( i in 1:length(emp_id_list)){
      curremp=emp_id_list[i]
      print(paste("i=",i))
      sub=response[response$emp_id==curremp & response$rel_id==currrel,]
      target=unique(sub$target_emp_id)
      print(paste("j=",j))
      if(length(target)>0){
        for (k  in 1:length(target)){
          print(paste("k=",k))
          curtarget=target[k]
          sub1=sub[sub$target_emp_id==curtarget,]
          score=decayfun(sub1,1,1)/decayweight
          temp=data.frame(emp_id=curremp,target_emp_id=curtarget,rel_id=currrel,weight=score)
          opcal=rbind(opcal,temp)
        }
      }
    }
  }
  
  TableName=paste("calculated_edge_",format(Sys.Date(), "%Y_%m_%d"),sep = "")
  
  query=paste("CREATE TABLE ",TableName,"(
              `emp_from` int(11) NOT NULL,
              `emp_to` int(11) NOT NULL,
              `rel_id` int(11) NOT NULL,
              `weight` double DEFAULT NULL,
              KEY `emp_from` (`emp_from`),
              KEY `emp_to` (`emp_to`),
              CONSTRAINT ",TableName,"_ibfk_1 FOREIGN KEY (`emp_from`) REFERENCES `employee` (`emp_id`),
              CONSTRAINT ",TableName,"_ibfk_2 FOREIGN KEY (`emp_to`) REFERENCES `employee` (`emp_id`)
  );",sep="")

  dbGetQuery(mydb,query)
  
  values <- paste("(",opcal$emp_id,",",opcal$target_emp_id,"," ,opcal$rel_id,",",opcal$weight,")", sep="", collapse=",")
  
  queryinsert <- paste("insert into ",TableName," values ", values,sep = "")
  
  dbGetQuery(mydb,queryinsert)
  dbDisconnect(mydb)
} 

calcualte_edge(args[0])
