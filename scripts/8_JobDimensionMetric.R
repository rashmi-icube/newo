library(RNeo4j)
library(igraph)
library(moments)
library(RMySQL)
library(reshape2)

args = commandArgs(trailingOnly=TRUE)

setwd("C:\\Users\\Hitendra\\Desktop\\R metric Function")
source('config.R')

JobDimensionMetric=function(CompanyId){
  
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
  
  #nodes of current Team
  vertexlist1=cypher(graph, querynode) 
  
  queryedge1 = "match (a:Employee)-[r]->(b:Employee) 
  return a.emp_id as from ,b.emp_id as to ,type(r) as relation,r.weight as weight"
  
  edgelist1 = cypher(graph, queryedge1)
  
  query="SELECT * FROM variable;"
  res <- dbSendQuery(mydb,query)
  variable=fetch(res,-1)
  
  query="select * from dimension_master"
  
  res <- dbSendQuery(mydb,query)
  
  dimension_master<- fetch(res,-1)
  
  query="select * from dimension_value"
  
  res <- dbSendQuery(mydb,query)
  
  dimension_value<- fetch(res,-1)
  
  query="select * from employee"
  
  res <- dbSendQuery(mydb,query)
  
  employee<- fetch(res,-1)
  
  query="select * from cube_master"
  
  res <- dbSendQuery(mydb,query)
  
  cube_master<- fetch(res,-1)
  
  
  query=paste("Select * from individual_metric_value where metric_id=5
              order by metric_val_id desc Limit ",
              nrow(employee),sep = "")
  
  res <- dbSendQuery(mydb,query)
  # individual sentiment
  sentiment_ind <- fetch(res,-1)
  
  query=paste("Select * from individual_metric_value where metric_id=3 
              order by metric_val_id desc Limit ",
              nrow(employee),sep = "")
  
  res <- dbSendQuery(mydb,query)
  # individual retention
  retention_ind <- fetch(res,-1)
  
  
  # sql query to get max instrength for relation learning
  query=paste("SELECT max(t1.nw_metric_value) as Score FROM individual_nw_metric_value as t1
              where t1.nw_metric_id=2 and t1.rel_id=4 and t1.calc_time=
              (select max(t2.calc_time) from individual_nw_metric_value as t2
              where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
              ,sep="")
  
  #run query
  res <- dbSendQuery(mydb,query)
  
  maxinstrength_learning <- fetch(res,-1)
  
  maxinstrength_learning=maxinstrength_learning[1,1]
  
  # sql query to get max instrength for relation social
  query=paste("SELECT max(t1.nw_metric_value) as Score FROM individual_nw_metric_value as t1
              where t1.nw_metric_id=2 and t1.rel_id=3 and t1.calc_time=
              (select max(t2.calc_time) from individual_nw_metric_value as t2
              where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
              ,sep="")
  
  #run query
  res <- dbSendQuery(mydb,query)
  
  maxinstrength_social <- fetch(res,-1)
  maxinstrength_social=maxinstrength_social[1,1]
  
  # sql query to get max instrength for relation innovation
  query=paste("SELECT max(t1.nw_metric_value) as Score FROM individual_nw_metric_value as t1
              where t1.nw_metric_id=2 and t1.rel_id=1 and t1.calc_time=
              (select max(t2.calc_time) from individual_nw_metric_value as t2
              where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
              ,sep="")
  
  #run query
  res <- dbSendQuery(mydb,query)
  
  maxinstrength_innovation <- fetch(res,-1)
  maxinstrength_innovation=maxinstrength_innovation[1,1]
  
  # filter for edge from master for all employee and innovation
  edgelist=edgelist1[edgelist1$relation=="innovation",c("from","to")]
  
  # create graph for all employee and innovation relation
  g1 <- graph.data.frame(edgelist, directed=TRUE, vertices=employee$emp_id)
  
  #oberall betweenness for innovation
  between=data.frame(betweenness(g1))
  
  # rename column
  names(between)="Betweenness"
  
  #add column for emp_id
  between$emp_id=row.names(between)
  
  #   # calcualte mu(mean) for betweeness
  #   mu=mean(between$Betweenness)
  #   # calculate threshold i.e mu+sigma
  #   threshold=mu+sd(between$Betweenness)
  #   
  #list of innovators in organization i.e betweenness above threshold
  innovators=between$emp_id[between$Rank<(nrow(between)*variable$value[variable$variable_name=="Metric9InnovatorPercentile"])]
  
  # function to find influence on (me) by (by)
  
  influence=function(me,by,edge){
    # subset edge from me
    sub=edge[edge$from==me,]  
    # % of weigth i.e. me influence by other
    sub$per=sub$weight/sum(sub$weight)
    #me influence by (by)
    influenceper=sub$per[sub$to==by]
    # if no (by) make influenceper to 0
    if(length(influenceper)==0){
      influenceper=0
    }
    return(influenceper)
  }
  
  
  op=data.frame(dimension_value_id=as.numeric(),metric_id=as.numeric(),score=as.numeric(),display_flag=as.integer(),stringsAsFactors = FALSE)
  
  for (k in 1:nrow(dimension_value)){
    print(k)
    dimension_value_id=dimension_value$dimension_val_id[k]
    dimension_id=dimension_value$dimension_id[k]
    dimension_val_name=dimension_value$dimension_val_name[k]
    dimension_name=dimension_master$dimension_name[dimension_master$dimension_id==dimension_id]
    cube_id=cube_master[cube_master[,dimension_name]==dimension_val_name,"cube_id"]
    emp_id_dim=employee$emp_id[employee$cube_id %in% cube_id]
    
    if(length(emp_id_dim)<=variable$value[variable$variable_name=="MinTeamSize"]){
      display_flag=0
    }else{
      display_flag=1
    }
    
    if(length(emp_id_dim)==0){
      
      op=rbind(op,data.frame(dimension_value_id=as.numeric(dimension_value_id),metric_id=c(6,7,8,9,10),score=0,display_flag))
      next
    }
    
    # Expertise
    edgelist=edgelist1[edgelist1$from %in% emp_id_dim &
                         edgelist1$to %in% emp_id_dim & 
                         edgelist1$relation=="learning",c("from","to","weight")]
    
    # create graph for current team and learning relation
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_dim)
    
    # calcualte indegree of ind in team (incomming)
    indegree=degree(g,mode="in")
    
    instrength=strength(g,mode="in")
    
    #average of instrength
    avginstrength=mean(instrength)
    
    # normalize by dividing with max of in strength overall
    avginstrength=avginstrength/maxinstrength_learning
    
    
    #claculate Skewness
    skew=skewness(indegree)
    
    if(is.nan(skew)){
      skew=3
    }
    #limit to +3 to -3 
    if(skew>3){
      skew=3
    }else{
      if(skew<(-3)){
        skew=(-3)
      }
    }
    
    # scale it 0 to 1
    skew=1-((skew+3)/6)
    
    #calcualte Performance for Team
    Performancescore=variable$value[variable$variable_name=="Metric6Wt1"]*avginstrength+variable$value[variable$variable_name=="Metric6Wt2"]*sqrt(skew)
    
    #scale to 0-100
    Performancescore=round(Performancescore*100,0)
    
    # copy performance score to op
    op=rbind(op,data.frame(dimension_value_id=as.numeric(dimension_value_id),metric_id=as.numeric(6),score=as.numeric(Performancescore),display_flag))
    
    
    # social Cohesion
    
    edgelist=edgelist1[edgelist1$from %in% emp_id_dim &
                         edgelist1$to %in% emp_id_dim & 
                         edgelist1$relation=="social",c("from","to","weight")]
    
    # create graph for current team and learning relation
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_dim)
    
    # calculate in strength
    instrength=strength(g,mode="in")
    
    #average of instrength
    avginstrength=mean(instrength)
    # normalize by dividing with max of in strength overall
    socialcohesionscore=avginstrength/maxinstrength_social
    
    # scale 0-100
    socialcohesionscore=round(socialcohesionscore*100,0)
    
    # copy social cohesion score to op
    op=rbind(op,data.frame(dimension_value_id=as.numeric(dimension_value_id),metric_id=as.numeric(7),score=as.numeric(socialcohesionscore),display_flag))
    
    
    #Retention
    
    # individual retention
    retention <-retention_ind[retention_ind$emp_id %in% emp_id_dim,]
    
    #to find people at risk based on Threshold
    PeopleAtRisk=retention[retention$metric_value<=variable$value[variable$variable_name=="Metric8RiskThreshold"],]
    #to find people not at risk 
    peopleNotRisk=retention[!(retention$emp_id %in% PeopleAtRisk$emp_id),]
    
    #  to filter edge for mentor and current team
    edgelist=edgelist1[edgelist1$from %in% emp_id_dim &
                         edgelist1$to %in% emp_id_dim & 
                         edgelist1$relation=="mentor",c("from","to","weight")]
    
   
    #initiate fraction to 0
    fraction=0
    if(nrow(PeopleAtRisk)>0){
      for (i in 1:nrow(PeopleAtRisk)){
        # emp who is inflencing
        riskemp=PeopleAtRisk$emp_id[i]
        # retention risk rate of emp who is inflencing
        riskrate=1-(PeopleAtRisk$metric_value[i]/100)
        for(j in 1:nrow(peopleNotRisk)){
          # emp who is inflenced
          meemp=peopleNotRisk$emp_id[j]
          # percent of inflence on me by influencer
          meinfluence=influence(meemp,riskemp,edgelist) 
          # product of inflencer retention , inflence percentage
          merisk=riskrate*meinfluence
          #add to fraction 
          fraction=fraction+merisk
        }
      }
    }  
    #Retention of tean = count of people at rsik + fraction of people whom they influence
    RetentionTeam=(nrow(PeopleAtRisk)+fraction)/length(emp_id_dim)
    
    # reverse The Retention scale
    RetentionTeam=1-RetentionTeam
    
    #Round and scale 0-100
    RetentionTeam=round(RetentionTeam*100,0)
    
    #add to op table
    op=rbind(op,data.frame(dimension_value_id=as.numeric(dimension_value_id),metric_id=as.numeric(8),score=as.numeric(RetentionTeam),display_flag))  
    # retention end
    
      # innovation
    # filte edge for current team and innovation relation from Master
    edgelist=edgelist1[edgelist1$from %in% emp_id_dim &
                         edgelist1$to %in% emp_id_dim & 
                         edgelist1$relation=="innovation",c("from","to")]
    
    # create graph
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_dim)
    
    # calculate in strength
    instrength=strength(g,mode="in")
    
    #average of instrength
    avginstrength=mean(instrength)
    
    # normalize by dividing with max of in strength overall
    avginstrength=avginstrength/maxinstrength_innovation
    
    
    # filter for edge from master for all employee and innovation
    edgelist=edgelist1[edgelist1$relation=="innovation",c("from","to")]
    
    # create graph for all employee and innovation relation
    g1 <- graph.data.frame(edgelist, directed=TRUE, vertices=employee$emp_id)
    
    #oberall betweenness for innovation
    between=data.frame(betweenness(g1))
    
    # rename column
    names(between)="Betweenness"
    
    #add column for emp_id
    between$emp_id=row.names(between)
    
  #   # calcualte mu(mean) for betweeness
  #   mu=mean(between$Betweenness)
  #   # calculate threshold i.e mu+sigma
  #   threshold=mu+sd(between$Betweenness)
  #   
    #list of innovators in organization i.e betweenness above threshold
    innovators=between$emp_id[between$Rank<(nrow(between)*variable$value[variable$variable_name=="Metric9InnovatorPercentile"])]
    # percentage of find innovators in team
    innovatorsinteam=length(emp_id_dim[emp_id_dim %in% innovators])/length(emp_id_dim)
    
    # innovators score
    innovationscore=variable$value[variable$variable_name=="Metric9Wt1"]*avginstrength+variable$value[variable$variable_name=="Metric9Wt2"]*sqrt(innovatorsinteam)
  
      #round and scale 0-100
    innovationscore=round(innovationscore*100,0)
    
    # add to op
    op=rbind(op,data.frame(dimension_value_id=as.numeric(dimension_value_id),metric_id=as.numeric(9),score=as.numeric(innovationscore),display_flag))  
    #sentiment
    
    # individual sentiment
    
    sentiment <- sentiment_ind[sentiment_ind$emp_id %in% emp_id_dim,]
    
    # filter for edge for current team alll all relation
    edgelist=edgelist1[edgelist1$from %in% emp_id_dim &
                         edgelist1$to %in% emp_id_dim ,c("from","to")]
    
    #aggregate edges for dif relatin
    edgelist=unique(edgelist)
    
    # create graph
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_dim)
    
    # calcualte in degree
    indegree=data.frame(degree(g,mode = "in"))
    
    names(indegree)="indegree"
    
    indegree$emp_id=row.names(indegree)
    indegree$emp_id=as.numeric(indegree$emp_id)
    
    #merge sentiment to indegree dataframe
    indegree=merge(indegree,sentiment, by = "emp_id")
    #product of indgeree and sentiment of ind
    indegree$product=indegree$indegree*indegree$metric_value
    
    # summation of product divide by summation of indegree
    sentimentScore=round(sum(indegree$product)/sum(indegree$indegree),0)
    
    if(is.nan(sentimentScore)){
      sentimentScore=0
    }
    
    # add to op
    op=rbind(op,data.frame(dimension_value_id=as.numeric(dimension_value_id),metric_id=as.numeric(10),score=as.numeric(sentimentScore),display_flag))
    
    
  }
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  query="CREATE TABLE `dimension_metric_value_temp` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `dimension_val_id` int(11) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  display_flag tinyint(1),
  PRIMARY KEY (`metric_val_id`),
  KEY `dimension_val_id` (`dimension_val_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `dimension_metric_value_temp_ibfk_1` FOREIGN KEY (`dimension_val_id`) REFERENCES `dimension_value` (`dimension_val_id`),
  CONSTRAINT `dimension_metric_value_temp_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
  );"
  
  dbGetQuery(mydb,query)
  
  values <- paste("(",op$dimension_value_id,",",op$metric_id,"," ,op$score,",'",currtime,"',",op$display_flag,")", sep="", collapse=",")
  
  queryinsert <- paste("insert into dimension_metric_value_temp (dimension_val_id,metric_id,metric_value,calc_time,display_flag) values ", values)
  
  dbGetQuery(mydb,queryinsert)
  
  query="insert into dimension_metric_value (dimension_val_id,metric_id,metric_value,calc_time,display_flag)
  select dimension_val_id,metric_id,metric_value,calc_time,display_flag from dimension_metric_value_temp"
  
  dbGetQuery(mydb,query)
  
  query="drop table dimension_metric_value_temp;"
  dbGetQuery(mydb,query)
  
  dbDisconnect(mydb)

}

JobDimensionMetric(args[1])