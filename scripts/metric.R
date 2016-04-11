library(RNeo4j)
library(igraph)
library(moments)
library(RMySQL)
library(reshape2)
library(dplyr)

#setwd("C:\\Users\\Hitendra\\Desktop\\R metric Function")
# Function=c(1)
# Position=c(4)
# Zone=c(8)

source('config.R')

TeamMetric=function(Function,Position,Zone){
  # remove this
  CompanyId=1
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  query=paste("call getCompanyConfig(",CompanyId,");",sep = "")
  res <- dbSendQuery(mydb,query)
  CompanyConfig=fetch(res,-1)
  
  dbDisconnect(mydb)
  
  comp_sql_dbname=CompanyConfig$comp_sql_dbname[1]
  comp_sql_server=CompanyConfig$sql_server[1]
  comp_sql_user_id=CompanyConfig$sql_user_id[1]
  comp_sql_password=CompanyConfig$sql_password[1]
  
  # sql DB connection
  mydb = dbConnect(MySQL(), user=comp_sql_user_id, password=comp_sql_password, dbname=comp_sql_dbname, host=comp_sql_server, port=mysqlport)
  
  if(Function==0 || Position==0 || Zone==0){
    if(Function==0){
      query="SELECT dimension_val_id FROM dimension_value where dimension_id=1;"
      res <- dbSendQuery(mydb,query)
      Func=fetch(res,-1)
      Function=Func$dimension_val_id
    }
    if(Position==0){
      query="SELECT dimension_val_id FROM dimension_value where dimension_id=2;"
      res <- dbSendQuery(mydb,query)
      Pos=fetch(res,-1)
      Position=Pos$dimension_val_id
    }
    if(Zone==0){
      query="SELECT dimension_val_id FROM dimension_value where dimension_id=3;"
      res <- dbSendQuery(mydb,query)
      Zon=fetch(res,-1)
      Zone=Zon$dimension_val_id
    }
    
  }
  
  query="SELECT * FROM variable;"
  res <- dbSendQuery(mydb,query)
  variable=fetch(res,-1)
  
  #op data frame
  op=data.frame(metric_id=as.numeric(),score=as.numeric())
  
  # graph DB connection
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  # Query to get nodes of current Team
  querynode = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                    a-[:is_positioned]->(p:Position) 
                    where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                    return a.emp_id",sep="")
  
  #nodes of current Team
  vertexlist=cypher(graph, querynode) 
  # To return -1 if team size is small
  if (nrow(vertexlist)<variable$value[variable$variable_name=="MinTeamSize"]){
    currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
    op=data.frame(metric_id=c(6,7,8,9,10),score=c(-1,-1,-1,-1,-1),calc_time=currtime)
    dbDisconnect(mydb)
    return(op)
  }
  
  
  # Query to get all edge with weigth and relation
  queryedge1 = "match (a:Employee),(b:Employee) 
  with a,b
  match a-[r]->b
  return a.emp_id as from ,b.emp_id as to ,type(r) as relation,r.weight as weight"
  
  # master list of edge 
  edgelist1 = cypher(graph, queryedge1)
  
  # filte edge for current team and learning relation from Master
  edgelist=edgelist1[edgelist1$from %in% vertexlist$a.emp_id &
                       edgelist1$to %in% vertexlist$a.emp_id & 
                       edgelist1$relation=="learning",c("from","to","weight")]
  
  # create graph for current team and learning relation
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  
  # calcualte degree of ind in team (incomming)
  indegree=degree(g,mode="in")
  
  instrength=strength(g,mode="in")
  
  #average of instrength
  avginstrength=mean(instrength)
  
  #   # sql query to get max instrength for relation learning
  #   query=paste("SELECT max(t1.nw_metric_value) as Score FROM individual_nw_metric_value as t1
  #               where t1.nw_metric_id=2 and t1.rel_id=4 and t1.calc_time=
  #               (select max(t2.calc_time) from individual_nw_metric_value as t2
  #               where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
  #               ,sep="")
  #   
  #   #run query
  #   res <- dbSendQuery(mydb,query)
  #   
  #   maxinstrength <- fetch(res,-1)
  #   maxinstrength=maxinstrength[1,1]
  
  edgelist=edgelist1[edgelist1$relation=="learning",c("from","to","weight")]
  
  # create graph for current team and learning relation
  g <- graph.data.frame(edgelist, directed=TRUE)
  
  maxinstrength=max(strength(g,mode="in"))
  
  # normalize by dividing with max of in strength overall
  avginstrength=avginstrength/maxinstrength
  
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
  op=rbind(op,data.frame(metric_id=6,score=Performancescore))
  
  
  # Socaial Cohesion start
  
  # filte edge for current team and learning relation from Master
  edgelist=edgelist1[edgelist1$from %in% vertexlist$a.emp_id &
                       edgelist1$to %in% vertexlist$a.emp_id & 
                       edgelist1$relation=="social",c("from","to","weight")]
  
  # create graph for current team and learning relation
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  
  # calculate in strength
  instrength=strength(g,mode="in")
  
  #average of instrength
  avginstrength=mean(instrength)
  
  #   # sql query to get max instrength for relation social
  #   query=paste("SELECT max(t1.nw_metric_value) as Score FROM individual_nw_metric_value as t1
  #               where t1.nw_metric_id=2 and t1.rel_id=3 and t1.calc_time=
  #               (select max(t2.calc_time) from individual_nw_metric_value as t2
  #               where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
  #               ,sep="")
  #   
  #   #run query
  #   res <- dbSendQuery(mydb,query)
  #   
  #   maxinstrength <- fetch(res,-1)
  #   maxinstrength=maxinstrength[1,1]
  #   # normalize by dividing with max of in strength overall
  
  edgelist=edgelist1[edgelist1$relation=="social",c("from","to","weight")]
  
  # create graph for current team and learning relation
  g <- graph.data.frame(edgelist, directed=TRUE)
  
  maxinstrength=max(strength(g,mode="in"))
  
  socialcohesionscore=avginstrength/maxinstrength
  # scale 0-100
  socialcohesionscore=round(socialcohesionscore*100,0)
  
  # copy social cohesion score to op
  op=rbind(op,data.frame(metric_id=7,score=socialcohesionscore))
  
  
  # retention
  
  # query to get individual retention for people in current team from individual_metric_value in sql
  query=paste("Select * from individual_metric_value where metric_id=3 and emp_id in (",
              paste(vertexlist$a.emp_id,sep = "",collapse = ","),") order by metric_val_id desc Limit ",
              nrow(vertexlist),sep = "")
  
  res <- dbSendQuery(mydb,query)
  # individual retention
  indretention <- fetch(res,-1)
  
  
  #change threshold ?????50
  #to find people at risk based on Threshold
  PeopleAtRisk=indretention[indretention$metric_value<=variable$value[variable$variable_name=="Metric8RiskThreshold"],]
  #to find people not at risk 
  peopleNotRisk=indretention[!(indretention$emp_id %in% PeopleAtRisk$emp_id),]
  
  #  to filter edge for mentor and current team
  edgelist=edgelist1[edgelist1$from %in% vertexlist$a.emp_id &
                       edgelist1$to %in% vertexlist$a.emp_id & 
                       edgelist1$relation=="mentor",c("from","to","weight")]
  
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
  
  #initiate fraction to 0
  fraction=0
  if(nrow(PeopleAtRisk)>0){
    for (i in 1:nrow(PeopleAtRisk)){
      # emp who is inflencing
      riskemp=PeopleAtRisk$emp_id[i]
      # retention risk rate of emp who is inflencing
      riskrate=1-(PeopleAtRisk$metric_value[i]/100)
      if (nrow(peopleNotRisk)>0){
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
    
  }
  
  #Retention of tean = count of people at rsik + fraction of people whom they influence
  RetentionTeam=(nrow(PeopleAtRisk)+fraction)/nrow(vertexlist)
  
  # reverse The Retention scale
  RetentionTeam=1-RetentionTeam
  
  #Round and scale 0-100
  RetentionTeam=round(RetentionTeam*100,0)
  
  #add to op table
  op=rbind(op,data.frame(metric_id=8,score=RetentionTeam))
  
  # retention end
  
  
  
  # innovation
  # filte edge for current team and innovation relation from Master
  edgelist=edgelist1[edgelist1$from %in% vertexlist$a.emp_id &
                       edgelist1$to %in% vertexlist$a.emp_id & 
                       edgelist1$relation=="innovation",c("from","to")]
  
  # create graph
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  
  # calculate in strength
  instrength=strength(g,mode="in")
  
  #average of instrength
  avginstrength=mean(instrength)
  
  # sql query to get max instrength for relation innovation
  #   query=paste("SELECT max(t1.nw_metric_value) as Score FROM individual_nw_metric_value as t1
  #               where t1.nw_metric_id=2 and t1.rel_id=1 and t1.calc_time=
  #               (select max(t2.calc_time) from individual_nw_metric_value as t2
  #               where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
  #               ,sep="")
  #   
  #   #run query
  #   res <- dbSendQuery(mydb,query)
  #   
  #   maxinstrength <- fetch(res,-1)
  #   maxinstrength=maxinstrength[1,1]
  
  edgelist=edgelist1[edgelist1$relation=="innovation",c("from","to","weight")]
  
  # create graph for current team and learning relation
  g <- graph.data.frame(edgelist, directed=TRUE)
  
  maxinstrength=max(strength(g,mode="in"))
  
  # normalize by dividing with max of in strength overall
  avginstrength=avginstrength/maxinstrength
  # query to get list of all node
  querynode = "match (a:Employee) return a.emp_id"
  
  # get list of all node
  vertexlist1=cypher(graph, querynode)
  
  # filter for edge from master for all employee and innovation
  edgelist=edgelist1[edgelist1$relation=="innovation",c("from","to")]
  
  # create graph for all employee and innovation relation
  g1 <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist1)
  
  #oberall betweenness for innovation
  between=data.frame(betweenness(g1))
  
  # rename column
  names(between)="Betweenness"
  
  #add column for emp_id
  between$emp_id=row.names(between)
  
  between$Rank=rank(-between$Betweenness,ties.method= "random")
  
  # calcualte mu(mean) for betweeness
  #mu=mean(between$Betweenness)
  # calculate threshold i.e mu+sigma
  #threshold=mu+sd(between$Betweenness)
  
  
  #list of innovators in organization i.e top 20 percentile
  innovators=between$emp_id[between$Rank<(nrow(between)*variable$value[variable$variable_name=="Metric9InnovatorPercentile"])]
  
  # percentage of find innovators in team
  innovatorsinteam=length(vertexlist$a.emp_id[vertexlist$a.emp_id %in% innovators])/nrow(vertexlist)
  
  # innovators score
  innovationscore=variable$value[variable$variable_name=="Metric9Wt1"]*avginstrength+variable$value[variable$variable_name=="Metric9Wt2"]*sqrt(innovatorsinteam)
  
  #round and scale 0-100
  innovationscore=round(innovationscore*100,0)
  
  # add to op
  op=rbind(op,data.frame(metric_id=9,score=innovationscore))
  
  #sentiment
  
  
  # query to get individual sentiment for people in current team from individual_metric_value in sql
  query=paste("Select * from individual_metric_value where metric_id=5 and emp_id in (",
              paste(vertexlist$a.emp_id,sep = "",collapse = ","),") order by metric_val_id desc Limit ",
              nrow(vertexlist),sep = "")
  
  res <- dbSendQuery(mydb,query)
  # individual sentiment
  sentiment <- fetch(res,-1)
  
  # filter for edge for current team alll all relation
  edgelist=edgelist1[edgelist1$from %in% vertexlist$a.emp_id &
                       edgelist1$to %in% vertexlist$a.emp_id ,c("from","to")]
  
  #aggregate edges for dif relatin
  edgelist=unique(edgelist)
  
  # create graph
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  
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
  op=rbind(op,data.frame(metric_id=10,score=sentimentScore))
  
  dbDisconnect(mydb)
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  op$calc_time=currtime
  
  return(op)
  
}


SmartListResponse=function(emp_id,rel_id){
  
  CompanyId=1
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  query=paste("call getCompanyConfig(",CompanyId,");",sep = "")
  res <- dbSendQuery(mydb,query)
  CompanyConfig=fetch(res,-1)
  
  dbDisconnect(mydb)
  
  comp_sql_dbname=CompanyConfig$comp_sql_dbname[1]
  comp_sql_server=CompanyConfig$sql_server[1]
  comp_sql_user_id=CompanyConfig$sql_user_id[1]
  comp_sql_password=CompanyConfig$sql_password[1]
  
  # sql DB connection
  mydb = dbConnect(MySQL(), user=comp_sql_user_id, password=comp_sql_password, dbname=comp_sql_dbname, host=comp_sql_server, port=mysqlport)
  
  query=paste("select rel_name from relationship_master where rel_id=",rel_id,";",sep="")
  
  res <- dbSendQuery(mydb,query)
  
  relname<- fetch(res,-1)
  
  dbDisconnect(mydb)  
  
  relname=relname[1,1]
  
  # graph DB connection
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  querynode = paste("match (a:Employee {emp_id:",emp_id,"})-[r:",relname,"]->(b:Employee) 
                    return b.emp_id as emp_id,r.weight as weight"
                    ,sep="")
  
  FirstConn=cypher(graph, querynode)
  if (!is.null(FirstConn)){
    FirstConn$Rank=rank(-FirstConn$weight,ties.method = "random")
    
    querynode = paste("match (a:Employee {emp_id:",emp_id,"})-[r:",relname,"]->(b:Employee)-[:",relname,"]->(c:Employee) 
                      return b.emp_id,c.emp_id,r.weight"
                      ,sep="")
    
    SecondConn=cypher(graph, querynode)
    
    SecondConn=aggregate(SecondConn$r.weight,by=list(emp_id=SecondConn$c.emp_id),max)
    
    names(SecondConn)[2]="weight"
    
    SecondConn=SecondConn[SecondConn$emp_id!=emp_id,]
    
    SecondConn=SecondConn[!(SecondConn$emp_id %in% FirstConn$emp_id),]
    
    SecondConn$Rank=rank(-SecondConn$weight,ties.method = "random")
    
    op=FirstConn[FirstConn$Rank<=5,]
    
    nextfive=SecondConn[SecondConn$Rank<=5,]
    nextfive$Rank=nextfive$Rank+nrow(op)
    
    op=rbind(op,nextfive)
    
    nextfive=FirstConn[FirstConn$Rank>5,]
    nextfive$Rank=nextfive$Rank+5
    
    op=rbind(op,nextfive)
    
    nextfive=SecondConn[SecondConn$Rank>5,]
    nextfive$Rank=nextfive$Rank+nrow(op)-5
    
    op=rbind(op,nextfive)
    
    op=op[,c("emp_id","Rank")]
    
  }else{
    op=data.frame(emp_id=as.numeric(),Rank=as.numeric())
  }
  
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  query=paste("call owen.getListColleague('",emp_id,"')",sep="")
  
  res <- dbSendQuery(mydb,query)
  
  employeeCube<- fetch(res,-1)
  
  dbDisconnect(mydb)
  
  employeeCube=data.frame(employeeCube[!(employeeCube$emp_id %in% emp_id),])
  names(employeeCube)[1]="emp_id"
  
  employeeCube=data.frame(employeeCube[!(employeeCube$emp_id %in% op$emp_id),])
  names(employeeCube)="emp_id"
  
  if(nrow(employeeCube)>0){
    employeeCube$Rank=(nrow(op)+1):(nrow(employeeCube)+nrow(op))
    op=rbind(op,employeeCube)
  }
  
  op$emp_id=as.integer(op$emp_id)
  op$Rank=as.integer(op$Rank)
  
  return(op)
}



#Team
TeamSmartList=function(Function,Position,Zone,init_type_id){
  CompanyId=1
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  query=paste("call getCompanyConfig(",CompanyId,");",sep = "")
  res <- dbSendQuery(mydb,query)
  CompanyConfig=fetch(res,-1)
  
  dbDisconnect(mydb)
  
  comp_sql_dbname=CompanyConfig$comp_sql_dbname[1]
  comp_sql_server=CompanyConfig$sql_server[1]
  comp_sql_user_id=CompanyConfig$sql_user_id[1]
  comp_sql_password=CompanyConfig$sql_password[1]
  
  # sql DB connection
  mydb = dbConnect(MySQL(), user=comp_sql_user_id, password=comp_sql_password, dbname=comp_sql_dbname, host=comp_sql_server, port=mysqlport)
  
  # condition to replace all(0) with der dimension_id
  cat("\nData Received Function=",Function,"Position=",Position,"Zone=",Zone,"init_type=",init_type_id,file="Rlog.txt",sep=" ",append=TRUE)
  if(Function==0 || Position==0 || Zone==0){
    
    if(Function==0){
      query="SELECT dimension_val_id FROM dimension_value where dimension_id=1;"
      res <- dbSendQuery(mydb,query)
      Func=fetch(res,-1)
      Function=Func$dimension_val_id
      cat("\nFunction 0 replaced with all id",Function,file="Rlog.txt",sep=" ",append=TRUE)
    }
    if(Position==0){
      query="SELECT dimension_val_id FROM dimension_value where dimension_id=2;"
      res <- dbSendQuery(mydb,query)
      Pos=fetch(res,-1)
      Position=Pos$dimension_val_id
      cat("\nPosition 0 replaced with all id",Position,file="Rlog.txt",sep=" ",append=TRUE)
    }
    if(Zone==0){
      query="SELECT dimension_val_id FROM dimension_value where dimension_id=3;"
      res <- dbSendQuery(mydb,query)
      Zon=fetch(res,-1)
      Zone=Zon$dimension_val_id
      cat("\nPosition 0 replaced with all id",Zone,file="Rlog.txt",sep=" ",append=TRUE)
    }
    
  }
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  # query to  get list of emp(node list) belonging to dynamic cube
  querynode = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                    a-[:is_positioned]->(p:Position) 
                    where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                    return a.emp_id",sep="")
  
  # run query and store reslut in vertexlist
  vertexlist=cypher(graph, querynode)
  
  # performance
  if(init_type_id==6){
    cat("\n calculating for type 6",file="Rlog.txt",sep=" ",append=TRUE)  
    #query to  get list of edges of learning relation belonging to dynamic cube
    queryedge = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                      (z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]->(f:Function),
                      a-[:is_positioned]->(p:Position)<-[:is_positioned]-b
                      where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                      with a,b
                      match a-[r:learning]->b
                      return a.emp_id as from ,b.emp_id as to ",sep="")
    
    # run query and store reslut in edgelist
    edgelist = cypher(graph, queryedge)
    
    # create graph of dynamic cube with learning relation
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
    
    # calculate closeness within team
    op=data.frame(closeness(g))
    # name column Score
    names(op)="Score"
    # add column emp_id
    op$emp_id=row.names(op)
    cat("\n done calculating for type 6",file="Rlog.txt",sep=" ",append=TRUE)  
    
  }
  
  # Social Cohesion
  if(init_type_id==7){
    cat("\n calculating for type 7",file="Rlog.txt",sep=" ",append=TRUE)  
    #query to  get list of edges of social relation belonging to dynamic cube
    queryedge = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                      (z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]->(f:Function),
                      a-[:is_positioned]->(p:Position)<-[:is_positioned]-b
                      where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                      with a,b
                      match a-[r:social]->b
                      return a.emp_id as from ,b.emp_id as to ",sep="")
    
    # run query and store reslut in edgelist
    edgelist = cypher(graph, queryedge)
    
    # create graph of dynamic cube with social relation
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
    
    # calculate betweenness for team
    op=data.frame(betweenness(g))
    # name column Score
    names(op)="Score"
    # add column emp_id
    op$emp_id=row.names(op)
    cat("\n done calculating for type 7",file="Rlog.txt",sep=" ",append=TRUE)  
  }
  
  # Retention and Sentiment
  if(init_type_id==8 || init_type_id==10){
    cat("\n calculating for type 8,10",file="Rlog.txt",sep=" ",append=TRUE)  
    #query to  get list of edges of mentor relation belonging to dynamic cube
    queryedge = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                      (z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]->(f:Function),
                      a-[:is_positioned]->(p:Position)<-[:is_positioned]-b
                      where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                      with a,b
                      match a-[r:mentor]->b
                      return a.emp_id as from ,b.emp_id as to ,r.weight as weight",sep="")
    # run query and store reslut in edgelist
    edgelist = cypher(graph, queryedge)
    
    # create graph of dynamic cube with mentor relation
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
    # calculate instrength for team
    op=data.frame(strength(g,mode = "in"))
    # name column Score
    names(op)="Score"
    # add column emp_id
    op$emp_id=row.names(op)
    cat("\n done calculating for type 8,10",file="Rlog.txt",sep=" ",append=TRUE)  
  }
  
  # innovation
  if(init_type_id==9){
    cat("\n calculating for type 9",file="Rlog.txt",sep=" ",append=TRUE)  
    # sql db connection
    
    # sql query to get betweenness(overall) score from sql table nw_metric_value
    query=paste("SELECT t1.emp_id,t1.nw_metric_value as Score FROM individual_nw_metric_value as t1
                where t1.nw_metric_id=3 and t1.rel_id=1 and t1.emp_id in (",paste(vertexlist$a.emp_id,collapse=","),") and t1.calc_time=
                (select max(t2.calc_time) from individual_nw_metric_value as t2
                where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
                ,sep="")
    
    #run sql query
    res <- dbSendQuery(mydb,query)
    #extract result
    op<- fetch(res,-1)
    #disconnect sql data
    
    cat("\n done calculating for type 10",file="Rlog.txt",sep=" ",append=TRUE)  
  }
  
  # Rank Score in descending order
  op$Rank=rank(-op$Score,ties.method= "random")
  # flag high medium low
  op$flag=ifelse(op$Rank<=nrow(op)/3,"High fit",ifelse(op$Rank<=nrow(op)*2/3,"Med fit","Low fit"))
  # return op
  op$emp_id=as.integer(op$emp_id)
  op$Rank=as.integer(op$Rank)
  cat("\n Returning op for Team SmartList",file="Rlog.txt",sep=" ",append=TRUE)  
  
  dbDisconnect(mydb)
  return(op)
}

# individual

IndividualSmartList=function(emp_id,init_type_id){
  CompanyId=1
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  query=paste("call getCompanyConfig(",CompanyId,");",sep = "")
  res <- dbSendQuery(mydb,query)
  CompanyConfig=fetch(res,-1)
  
  dbDisconnect(mydb)
  
  comp_sql_dbname=CompanyConfig$comp_sql_dbname[1]
  comp_sql_server=CompanyConfig$sql_server[1]
  comp_sql_user_id=CompanyConfig$sql_user_id[1]
  comp_sql_password=CompanyConfig$sql_password[1]
  
  # sql DB connection
  mydb = dbConnect(MySQL(), user=comp_sql_user_id, password=comp_sql_password, dbname=comp_sql_dbname, host=comp_sql_server, port=mysqlport)
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  
  # Expertise
  if(init_type_id==1){
    
    # query to  get list of people form same cube
    query=paste("call getListColleague('",emp_id,"')",sep="")
    
    # run query 
    res <- dbSendQuery(mydb,query)
    # list of people from same cube
    emoloyeeCube<- fetch(res,-1)
    
    # query to get edge list for people in that cube and learning 
    queryedge = paste("match (a:Employee),(b:Employee) where
                      a.emp_id in [",paste(emoloyeeCube$emp_id,collapse=","),"] and 
                      b.emp_id in [",paste(emoloyeeCube$emp_id,collapse=","),"]
                      with a,b
                      match a-[r:learning]->b
                      return a.emp_id as from ,b.emp_id as to,r.weight as weight ",sep="")
    
    # run query
    edgelist = cypher(graph, queryedge)
    # create graph of people from same cube and learning nw
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emoloyeeCube$emp_id)
    
    # calculte instrength
    op=data.frame(strength(g,mode = "in"))
    # rename column 
    names(op)="Score"
    # add column emp_id
    op$emp_id=row.names(op)
    
  }  
  
  # Mentorship
  if(init_type_id==2){
    
    # query to get second degree peolpe for all relation
    querynode = paste("match (a:Employee {emp_id:",emp_id,"})-[*1..2]-(b:Employee) 
                      return distinct(b.emp_id) as emp_id",sep="")
    # reiun query
    vertexlist=cypher(graph, querynode)
    
    # query to get Mentorship metric score for second degree people
    query=paste("SELECT t1.emp_id,t1.metric_value as Score  FROM individual_metric_value as t1 where t1.metric_id=2 and t1.emp_id in (",paste(vertexlist$emp_id,collapse = ","),")
                and t1.calc_time=(select max(t2.calc_time) from  individual_metric_value as t2 
                where t2.metric_id=t1.metric_id and t2.emp_id=t1.emp_id);",sep="")
    # run query
    res <- dbSendQuery(mydb,query)
    # op with mentorship score
    op<- fetch(res,-1)
    # remove employee on whom init created
    op=op[op$emp_id!=emp_id,]
  }  
  
  # Retention Sentiment
  if(init_type_id==3 || init_type_id==5){
    # query to get people from same cube
    query=paste("call getListColleague('",emp_id,"')",sep="")
    # run query
    res <- dbSendQuery(mydb,query)
    # fetch reslt
    emoloyeeCube<- fetch(res,-1)
    
    # query to get edgelist for people in cube and mentor relatin
    queryedge = paste("match (a:Employee),(b:Employee) where
                      a.emp_id in [",paste(emoloyeeCube$emp_id,collapse=","),"] and 
                      b.emp_id in [",paste(emoloyeeCube$emp_id,collapse=","),"]
                      with a,b
                      match a-[r:mentor]->b
                      return a.emp_id as from ,b.emp_id as to,r.weight as weight ",sep="")
    # run query
    edgelist = cypher(graph, queryedge)
    # create graph fro cube and mentor rel
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emoloyeeCube$emp_id)
    # calculate instrength 
    op=data.frame(strength(g,mode = "in"))
    # rename column
    names(op)="Score"
    # add column emp_id
    op$emp_id=row.names(op)
  }
  
  # influence
  if(init_type_id==4){
    
    #query to get people upto 2nd degree conntn and all relation
    querynode = paste("match (a:Employee {emp_id:",emp_id,"})-[*1..2]-(b:Employee) 
                      return distinct(b.emp_id) as emp_id",sep="")
    #run query
    vertexlist=cypher(graph, querynode)
    # query to get inflence score form sql for people upto 2nd degree conntn
    query=paste("SELECT t1.emp_id,t1.metric_value as Score  FROM individual_metric_value as t1 where t1.metric_id=4 and t1.emp_id in (",paste(vertexlist$emp_id,collapse = ","),")
                and t1.calc_time=(select max(t2.calc_time) from  individual_metric_value as t2 
                where t2.metric_id=t1.metric_id and t2.emp_id=t1.emp_id);",sep="")
    # run query
    res <- dbSendQuery(mydb,query)
    # fetch result
    op<- fetch(res,-1)
  }
  # disconnect db
  dbDisconnect(mydb)
  
  # rempve employee on whom initiative created
  op=op[op$emp_id!=emp_id,]
  # rank score
  op$Rank=rank(-op$Score,ties.method= "random")
  # flag high medium low
  op$flag=ifelse(op$Rank<=nrow(op)/3,"High fit",ifelse(op$Rank<=nrow(op)*2/3,"Med fit","Low fit"))
  
  op$emp_id=as.integer(op$emp_id)
  op$Rank=as.integer(op$Rank)
  
  # return op
  return(op)
}

calculate_edge=function(CompanyId){
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
  
  appreciation_response$response_time=strptime(appreciation_response$response_time,format="%Y-%m-%d  %H:%M:%S")
  
  for  (j in 1:4){
    rel_id=j
    que_sub=question[question$rel_id==j,]
    que_sub=que_sub[order(que_sub$que_id),]
    for (k in 1:nrow(que_sub)){
      que_id=que_sub$que_id[k]
      startdate=que_sub$start_date[k]
      appreciation_response$que_id[appreciation_response$rel_id==j & 
                                     as.Date(appreciation_response$response_time)>=startdate]=que_id
    }
  }
  
  appreciation_response=appreciation_response[!is.na(appreciation_response$que_id),]
  
  response=rbind(we_response[,c("emp_id","que_id","response_time","target_emp_id","rel_id","weight")],
                 appreciation_response[,c("emp_id","que_id","response_time","target_emp_id","rel_id","weight")])
  
  
  opcal=data.frame(emp_id=as.numeric(),target_emp_id=as.numeric(),
                   rel_id=as.numeric(),weight=as.numeric())
  
  for ( j in 1:4){
    currrel=j
    que_rel=question$que_id[question$rel_id==currrel]
    que_rel=sort(que_rel,decreasing = TRUE)
    decayweight=0
    d=1
    op=data.frame()
    for(x in 1:length(que_rel)){
      decayweight=decayweight+decay^(x-1)
      currque=que_rel[x]
      sub=response[response$rel_id==currrel & response$que_id==currque,]
      sub$response_time=as.POSIXct(sub$response_time)
      sub <- sub %>%
        group_by(emp_id,target_emp_id) %>%
        filter(response_time == max(response_time))
      sub$weight=sub$weight*d
      op=rbind(op,sub)
      d=d*decay
    }
    
    op1=aggregate(op$weight,by=list(emp_id=op$emp_id,target_emp_id=op$target_emp_id,rel_id=op$rel_id),sum)
    names(op1)[4]="weight"
    op1$weight=op1$weight/decayweight
    
    opcal=rbind(opcal,op1)
  }
  
  
  TableName=paste("calculated_edge_",format(Sys.Date(), "%Y_%m_%d"),sep = "")
  
  query=paste("drop table if exists ",TableName,";")
  
  dbGetQuery(mydb,query)
  
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
  return(TRUE)
} 

calcualte_edge_old=function(CompanyId){
  # connect to Owen_master sqldb
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  # get company's connection details
  query=paste("call getCompanyConfig(",CompanyId,");",sep = "")
  res <- dbSendQuery(mydb,query)
  CompanyConfig=fetch(res,-1)
  
  dbDisconnect(mydb)
  
  comp_sql_dbname=CompanyConfig$comp_sql_dbname[1]
  comp_sql_server=CompanyConfig$sql_server[1]
  comp_sql_user_id=CompanyConfig$sql_user_id[1]
  comp_sql_password=CompanyConfig$sql_password[1]
  # connect to Company's sqldb
  mydb = dbConnect(MySQL(), user=comp_sql_user_id, password=comp_sql_password, dbname=comp_sql_dbname, host=comp_sql_server, port=mysqlport)
  
  # get decay variable
  query="SELECT * FROM variable where variable_id=9;"
  res <- dbSendQuery(mydb,query)
  decay=fetch(res,-1)
  decay=decay$value[1]
  
  # get quesion list for batch 1 and we type
  query="SELECT * FROM question where survey_batch_id=1 and que_type=1;"
  res <- dbSendQuery(mydb,query)
  question=fetch(res,-1)
  
  question$start_date=as.Date(question$start_date,format="%Y-%m-%d")
  question$end_date=as.Date(question$end_date,format="%Y-%m-%d")
  #question$end_date=as.Date(question$end_date,format="%Y-%m-%d")
  
  # filter question which are completed
  question=question[question$end_date<Sys.Date(),]
    
  # filter top latest 4 question for each rel_id
  question <- question %>%
    group_by(rel_id) %>%
    top_n(n = 4, wt = que_id)
  
  # we_response form sql for filtered question
  query=paste("SELECT * FROM we_response where que_id in (",paste(question$que_id,collapse = ","),")")
  res <- dbSendQuery(mydb,query)
  we_response=fetch(res,-1)
  
  we_response$response_time=strptime(we_response$response_time,format="%Y-%m-%d  %H:%M:%S")
  #we_response$response_time=strptime(we_response$response_time,format="%Y-%m-%d  %H:%M:%S")
  
  # oldest date of question started
  startdate=min(question$start_date)
  
  # appreciatio from sql from startdate to today
  query=paste("SELECT * FROM appreciation_response where date(response_time)>'",startdate,"'",sep = "")
  res <- dbSendQuery(mydb,query)
  appreciation_response=fetch(res,-1)
  
  #appreciation_response$response_time=strptime(appreciation_response$response_time,format="%Y-%m-%d  %H:%M:%S")
  
  
  # to link appreciatio response to latest question
  for  (j in 1:4){
    rel_id=j
    que_sub=question[question$rel_id==j,]
    que_sub=que_sub[order(que_sub$que_id),]
    for (k in 1:nrow(que_sub)){
      que_id=que_sub$que_id[k]
      startdate=que_sub$start_date[k]
      appreciation_response$que_id[appreciation_response$rel_id==j & 
                                     as.Date(appreciation_response$response_time)>=startdate]=que_id
    }
  }
  
  # remove appreciation which is not linked to any question
  appreciation_response=appreciation_response[!is.na(appreciation_response$que_id),]
  
  # combine we_response and appreciation_response
  response=rbind(we_response[,c("emp_id","que_id","response_time","target_emp_id","rel_id","weight")],
                 appreciation_response[,c("emp_id","que_id","response_time","target_emp_id","rel_id","weight")])
  
  # list of employee who responded
  emp_id_list=unique(response$emp_id)
  
  # deacay Function
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
  
  #empty dataframe
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

update_neo=function(CompanyId){
  
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
  
  
  TableName=paste("calculated_edge_",format(Sys.Date(), "%Y_%m_%d"),sep = "")
  
  query=paste("SELECT * FROM ",TableName," ;",sep = "")
  res <- dbSendQuery(mydb,query)
  edgelist=fetch(res,-1)
  
  query="SELECT * FROM relationship_master"
  res <- dbSendQuery(mydb,query)
  relationship_master=fetch(res,-1)
  
  edgelist=merge(edgelist,relationship_master,by="rel_id")
  
  dbDisconnect(mydb)
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  
  querynode = "match (a:Employee)-[r:innovation|learning|mentor|social]->(b:Employee) delete r"
  
  cypher(graph, querynode) 
  
  for (k in 1:4){
    edgelistrel=edgelist[edgelist$rel_id==k,]
    
    relname=relationship_master$rel_name[relationship_master$rel_id==k]
    
    write.csv(edgelist,"edgelist.csv",row.names = FALSE)
    
    link=getwd()
    
    query=paste('LOAD CSV WITH HEADERS FROM "file:///',link,'/edgelist.csv" AS row
                MATCH (a:Employee),(b:Employee)
                WHERE a.emp_id = toInt(row.emp_from) AND b.emp_id = toInt(row.emp_to)
                CREATE (a)-[r:',relname,' {weight:toInt(row.weight)}]->(b)',sep = "")
    
    cypher(graph, query) 
    
  }
  
  return(TRUE)
}


update_neo_old=function(CompanyId){
  
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
  
  
  TableName=paste("calculated_edge_",format(Sys.Date(), "%Y_%m_%d"),sep = "")
  
  query=paste("SELECT * FROM ",TableName," ;",sep = "")
  res <- dbSendQuery(mydb,query)
  edgelist=fetch(res,-1)
  
  query="SELECT * FROM relationship_master"
  res <- dbSendQuery(mydb,query)
  relationship_master=fetch(res,-1)
  
  edgelist=merge(edgelist,relationship_master,by="rel_id")
  
  dbDisconnect(mydb)
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  
  querynode = "match (a:Employee)-[r:innovation|learning|mentor|social]->(b:Employee) delete r"
  
  cypher(graph, querynode) 
  
  for (k in 1:4){
    edgelistrel=edgelist[edgelist$rel_id==k,]
    
    relname=relationship_master$rel_name[relationship_master$rel_id==k]
    
    query = paste("MATCH (a:Employee {emp_id:{from}}),(b:Employee {emp_id:{to}})
                  CREATE (a)-[r:",relname," {weight:toFloat({wt})}]->(b)",sep="")
    
    
    tx = newTransaction(graph)
    
    for (i in 1:nrow(edgelistrel)) {
      # Upload in blocks of 1000.
      if(i %% 100 == 0) {
        # Commit current transaction.
        commit(tx)
        print(paste("Batch", i / 1000, "committed."))
        # Open new transaction.
        tx = newTransaction(graph)
      }
      
      # Append paramaterized Cypher query to transaction.
      appendCypher(tx,
                   query,
                   from = edgelistrel$emp_from[i],
                   to = edgelistrel$emp_to[i],
                   wt=edgelistrel$weight[i]
      )
      
    }
    
    # Commit last transaction.
    commit(tx)
    print("Last batch committed.")
    print("All done! for ")
    print(relname)
    
  }
  return(TRUE)
}

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
  return(TRUE)
}

JobCubeNwMetric=function(CompanyId){
  
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
  op=data.frame(nw_metric_value=as.numeric(),cube_id=as.integer(),rel_id=as.integer(),nw_metric_id=as.integer())
  for (i in 1:nrow(cube_master)){
    cube_id=cube_master$cube_id[i]
    emplist=employee$emp_id[employee$cube_id==cube_id]
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
      op=rbind(op,data.frame(nw_metric_value,cube_id,rel_id,nw_metric_id=5))
      
      #Balance
      instrength=strength(g2,mode="in")
      nw_metric_value=skewness(instrength)
      if(is.nan(nw_metric_value)){
        nw_metric_value=3
      }
      op=rbind(op,data.frame(nw_metric_value,cube_id,rel_id,nw_metric_id=6))
      
      # Average Path Length
      nw_metric_value=average.path.length(g1)
      op=rbind(op,data.frame(nw_metric_value,cube_id,rel_id,nw_metric_id=7))
    }
  }
  
  op[is.na(op)] <- 0
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  queryTemp="CREATE TABLE `team_nw_metric_value_temp` (
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
  CONSTRAINT `team_nw_metric_value_temp_ibfk_1` FOREIGN KEY (`cube_id`) REFERENCES `cube_master` (`cube_id`),
  CONSTRAINT `team_nw_metric_value_temp_ibfk_2` FOREIGN KEY (`nw_metric_id`) REFERENCES `nw_metric_master` (`nw_metric_id`),
  CONSTRAINT `team_nw_metric_value_temp_ibfk_3` FOREIGN KEY (`rel_id`) REFERENCES `relationship_master` (`rel_id`)
  );"
  
  dbGetQuery(mydb,queryTemp)
  
  values <- paste("(",op$cube_id,",",op$nw_metric_id,",", op$nw_metric_value,",'",currtime,"',",op$rel_id,")", sep="", collapse=",")
  
  queryinsert <- paste("insert into team_nw_metric_value_temp (cube_id,nw_metric_id,nw_metric_value,calc_time,rel_id) values ", values)
  
  dbGetQuery(mydb,queryinsert)
  
  query="insert into team_nw_metric_value (cube_id,nw_metric_id,nw_metric_value,calc_time,rel_id)
  select cube_id,nw_metric_id,nw_metric_value,calc_time,rel_id from team_nw_metric_value_temp;"
  
  dbGetQuery(mydb,query)
  
  query="drop table team_nw_metric_value_temp;"
  dbGetQuery(mydb,query)
  
  dbDisconnect(mydb)
  return(TRUE)
}

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
  return(TRUE)
}

JobIndividualMetric=function(CompanyId){
  
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
  if (is.na(previousdate)) {
    op=data.frame(metric_value=0,emp_id=vertexlist1$a.emp_id)
  }else{
    strengthall$date=as.Date(as.POSIXct(strengthall$calc_time))
    
    currstrength=strengthall[strengthall$date==currdate,c("emp_id","nw_metric_value")]
    
    prevstrength=strengthall[strengthall$date==previousdate,c("emp_id","nw_metric_value")]
    
    instrength=merge(currstrength,prevstrength,by="emp_id")
    names(instrength)[2:3]=c("current","previous")
    
    instrength$perc=(instrength$current-instrength$previous)/instrength$previous  
    instrength$perc=instrength$perc+1
    instrength$perc[is.na(instrength$perc)]=0
    
    instrength$perc[instrength$perc>1]=1
    
    instrength$metric_value=round(instrength$perc*100,0)
    
    op=instrength[,c("metric_value","emp_id")]
    
  }
  
  
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
  return(TRUE)
}

JobTeamMetric=function(CompanyId){
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
  
  #nodes of all Team
  vertexlist1=cypher(graph, querynode) 
  
  queryedge1 = "match (a:Employee)-[r]->(b:Employee) 
  return a.emp_id as from ,b.emp_id as to ,type(r) as relation,r.weight as weight"
  
  edgelist1 = cypher(graph, queryedge1)
  
  query="SELECT * FROM variable;"
  res <- dbSendQuery(mydb,query)
  variable=fetch(res,-1)
  
  query="select * from cube_master"
  
  res <- dbSendQuery(mydb,query)
  
  cube_master<- fetch(res,-1)
  
  query="select * from employee"
  
  res <- dbSendQuery(mydb,query)
  
  employee<- fetch(res,-1)
  
  # individual sentiment
  query=paste("Select * from individual_metric_value where metric_id=5
              order by metric_val_id desc Limit ",
              nrow(employee),sep = "")
  
  res <- dbSendQuery(mydb,query)
  
  sentiment_ind <- fetch(res,-1)
  
  # individual retention
  query=paste("Select * from individual_metric_value where metric_id=3 
              order by metric_val_id desc Limit ",
              nrow(employee),sep = "")
  
  res <- dbSendQuery(mydb,query)
  
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
  
  
  op=data.frame(cube_id=as.numeric(),metric_id=as.numeric(),score=as.numeric(),display_flag=as.integer(),stringsAsFactors = FALSE)
  
  for (k in 1:nrow(cube_master)){
    print(k)
    cube_id=cube_master$cube_id[k]
    emp_id_cube=employee$emp_id[employee$cube_id==cube_id]
    
    if(length(emp_id_cube)<variable$value[variable$variable_name=="MinTeamSize"]){
      display_flag=0
    }else{
      display_flag=1
    }
    
    if(length(emp_id_cube)==0){
      op=rbind(op,data.frame(cube_id=as.numeric(cube_id),metric_id=c(6,7,8,9,10),score=0,display_flag))
      next
    }
    
    # Expertise
    edgelist=edgelist1[edgelist1$from %in% emp_id_cube &
                         edgelist1$to %in% emp_id_cube & 
                         edgelist1$relation=="learning",c("from","to","weight")]
    
    # create graph for current team and learning relation
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_cube)
    
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
    op=rbind(op,data.frame(cube_id=as.numeric(cube_id),metric_id=as.numeric(6),score=as.numeric(Performancescore),display_flag))
    
    
    # social Cohesion
    
    edgelist=edgelist1[edgelist1$from %in% emp_id_cube &
                         edgelist1$to %in% emp_id_cube & 
                         edgelist1$relation=="social",c("from","to","weight")]
    
    # create graph for current team and learning relation
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_cube)
    
    # calculate in strength
    instrength=strength(g,mode="in")
    
    #average of instrength
    avginstrength=mean(instrength)
    
    
    # normalize by dividing with max of in strength overall
    socialcohesionscore=avginstrength/maxinstrength_social
    
    # scale 0-100
    socialcohesionscore=round(socialcohesionscore*100,0)
    
    # copy social cohesion score to op
    op=rbind(op,data.frame(cube_id=as.numeric(cube_id),metric_id=as.numeric(7),score=as.numeric(socialcohesionscore),display_flag))
    
    
    #Retention
    
    # individual retention
    retention <-retention_ind[retention_ind$emp_id %in% emp_id_cube,]
    
    #to find people at risk based on Threshold
    PeopleAtRisk=retention[retention$metric_value<=variable$value[variable$variable_name=="Metric8RiskThreshold"],]
    #to find people not at risk 
    peopleNotRisk=retention[!(retention$emp_id %in% PeopleAtRisk$emp_id),]
    
    #  to filter edge for mentor and current team
    edgelist=edgelist1[edgelist1$from %in% emp_id_cube &
                         edgelist1$to %in% emp_id_cube & 
                         edgelist1$relation=="mentor",c("from","to","weight")]
    
    
    #initiate fraction to 0
    fraction=0
    if(nrow(PeopleAtRisk)>0){
      for (i in 1:nrow(PeopleAtRisk)){
        # emp who is inflencing
        riskemp=PeopleAtRisk$emp_id[i]
        # retention risk rate of emp who is inflencing
        riskrate=1-(PeopleAtRisk$metric_value[i]/100)
        if (nrow(peopleNotRisk)>0){
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
    }  
    #Retention of tean = count of people at rsik + fraction of people whom they influence
    RetentionTeam=(nrow(PeopleAtRisk)+fraction)/length(emp_id_cube)
    
    # reverse The Retention scale
    RetentionTeam=1-RetentionTeam
    
    #Round and scale 0-100
    RetentionTeam=round(RetentionTeam*100,0)
    
    #add to op table
    op=rbind(op,data.frame(cube_id=as.numeric(cube_id),metric_id=as.numeric(8),score=as.numeric(RetentionTeam),display_flag))  
    # retention end
    
    # innovation
    # filte edge for current team and innovation relation from Master
    edgelist=edgelist1[edgelist1$from %in% emp_id_cube &
                         edgelist1$to %in% emp_id_cube & 
                         edgelist1$relation=="innovation",c("from","to")]
    
    # create graph
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_cube)
    
    # calculate in strength
    instrength=strength(g,mode="in")
    
    #average of instrength
    avginstrength=mean(instrength)
    
    
    # normalize by dividing with max of in strength overall
    avginstrength=avginstrength/maxinstrength_innovation
    
    # percentage of find innovators in team
    innovatorsinteam=length(emp_id_cube[emp_id_cube %in% innovators])/length(emp_id_cube)
    
    # innovators score
    innovationscore=variable$value[variable$variable_name=="Metric9Wt1"]*avginstrength+variable$value[variable$variable_name=="Metric9Wt2"]*sqrt(innovatorsinteam)
    
    #round and scale 0-100
    innovationscore=round(innovationscore*100,0)
    
    # add to op
    op=rbind(op,data.frame(cube_id=as.numeric(cube_id),metric_id=as.numeric(9),score=as.numeric(innovationscore),display_flag))  
    #sentiment
    
    # individual sentiment
    
    sentiment <- sentiment_ind[sentiment_ind$emp_id %in% emp_id_cube,]
    
    # filter for edge for current team alll all relation
    edgelist=edgelist1[edgelist1$from %in% emp_id_cube &
                         edgelist1$to %in% emp_id_cube ,c("from","to")]
    
    #aggregate edges for dif relatin
    edgelist=unique(edgelist)
    
    # create graph
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_cube)
    
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
    op=rbind(op,data.frame(cube_id=as.numeric(cube_id),metric_id=as.numeric(10),score=as.numeric(sentimentScore),display_flag))
    
  }
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  query="CREATE TABLE `team_metric_value_temp` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `cube_id` int(11) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  display_flag tinyint(1),
  PRIMARY KEY (`metric_val_id`),
  KEY `cube_id` (`cube_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `team_metric_value_temp_ibfk_1` FOREIGN KEY (`cube_id`) REFERENCES `cube_master` (`cube_id`),
  CONSTRAINT `team_metric_value_temp_ibfk_2` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
  );"
  
  dbGetQuery(mydb,query)
  
  values <- paste("(",op$cube_id,",",op$metric_id,"," ,op$score,",'",currtime,"',",op$display_flag,")", sep="", collapse=",")
  
  queryinsert <- paste("insert into team_metric_value_temp (cube_id,metric_id,metric_value,calc_time,display_flag) values ", values)
  
  dbGetQuery(mydb,queryinsert)
  
  query="insert into team_metric_value (cube_id,metric_id,metric_value,calc_time,display_flag)
  select cube_id,metric_id,metric_value,calc_time,display_flag from team_metric_value_temp"
  
  dbGetQuery(mydb,query)
  
  query="drop table team_metric_value_temp;"
  dbGetQuery(mydb,query)
  
  dbDisconnect(mydb)
  return(TRUE)
}

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
    
    if(length(emp_id_dim)<variable$value[variable$variable_name=="MinTeamSize"]){
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
        if (nrow(peopleNotRisk)>0){
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
  return(TRUE)
  
}

JobInitiativeMetric=function(CompanyId){
  
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
  
  
  querynode = "MATCH (i:Init)<-[r:part_of]-(f:Function),i<-[:part_of]-(p:Position),
  i<-[:part_of]-(z:Zone) where i.Category='Team' 
  RETURN i.Id,f.Id as Function_id,f.Name as Function_Name,
  p.Id as Position_Id,p.Name as Position_Name,
  z.Id as Zone_Id,z.Name as Zone_Name"
  
  #nodes of current Team
  part_of_dim=cypher(graph, querynode) 
  
  
  queryedge1 = "match (a:Employee),(b:Employee) 
  with a,b
  match a-[r]->b
  return a.emp_id as from ,b.emp_id as to ,type(r) as relation,r.weight as weight"
  
  edgelist1 = cypher(graph, queryedge1)
  
  query="select * from cube_master"
  
  res <- dbSendQuery(mydb,query)
  
  cube_master<- fetch(res,-1)
  
  query="select * from employee"
  
  res <- dbSendQuery(mydb,query)
  
  employee<- fetch(res,-1)
  
  query=paste("Select * from individual_metric_value where metric_id=5
              order by metric_val_id desc Limit ",
              nrow(employee),sep = "")
  
  res <- dbSendQuery(mydb,query)
  
  sentiment_ind <- fetch(res,-1)
  
  query=paste("Select * from individual_metric_value where metric_id=3 
              order by metric_val_id desc Limit ",
              nrow(employee),sep = "")
  
  res <- dbSendQuery(mydb,query)
  
  retention_ind <- fetch(res,-1)
  
  query="SELECT * FROM variable;"
  res <- dbSendQuery(mydb,query)
  variable=fetch(res,-1)
  
  InitList=unique(part_of_dim$i.Id)
  InitList=sort(InitList)
  InitList=append(InitList,-1)
  
  
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
  
  
  op=data.frame(initiative_id=as.numeric(),metric_id=as.numeric(),score=as.numeric(),display_flag=as.integer(),stringsAsFactors = FALSE)
  
  for (k in 1:length(InitList)){
    print(k)
    initiative_id=InitList[k]
    if(initiative_id==-1){
      emp_id_init=employee$emp_id
    }else{
      Function=unique(part_of_dim$Function_Name[part_of_dim$i.Id==initiative_id])
      Position=unique(part_of_dim$Position_Name[part_of_dim$i.Id==initiative_id])
      Zone=unique(part_of_dim$Zone_Name[part_of_dim$i.Id==initiative_id])
      cube_id=cube_master$cube_id[cube_master$Function %in% Function & cube_master$Position %in% Position & cube_master$Zone %in% Zone]
      emp_id_init=employee$emp_id[employee$cube_id %in% cube_id]
      
    }
    if(length(emp_id_init)<variable$value[variable$variable_name=="MinTeamSize"]){
      display_flag=0
    }else{
      display_flag=1
    }
    
    if(length(emp_id_init)==0){
      op=rbind(op,data.frame(initiative_id=as.numeric(initiative_id),metric_id=c(6,7,8,9,10),score=0,display_flag))
      next
    }
    
    edgelist=edgelist1[edgelist1$from %in% emp_id_init &
                         edgelist1$to %in% emp_id_init & 
                         edgelist1$relation=="learning",c("from","to","weight")]
    
    # create graph for current team and learning relation
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_init)
    
    # calcualte degree of ind in team (incomming)
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
    op=rbind(op,data.frame(initiative_id=as.numeric(initiative_id),metric_id=as.numeric(6),score=as.numeric(Performancescore),display_flag))
    
    # social Cohesion
    
    edgelist=edgelist1[edgelist1$from %in% emp_id_init &
                         edgelist1$to %in% emp_id_init & 
                         edgelist1$relation=="social",c("from","to","weight")]
    
    # create graph for current team and learning relation
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_init)
    
    
    # calculate in strength
    instrength=strength(g,mode="in")
    
    #average of instrength
    avginstrength=mean(instrength)
    
    # normalize by dividing with max of in strength overall
    socialcohesionscore=avginstrength/maxinstrength_social
    # scale 0-100
    socialcohesionscore=round(socialcohesionscore*100,0)
    # copy social cohesion score to op
    op=rbind(op,data.frame(initiative_id=as.numeric(initiative_id),metric_id=as.numeric(7),score=as.numeric(socialcohesionscore),display_flag))
    
    
    #Retention
    
    
    # individual retention
    retention <-retention_ind[retention_ind$emp_id %in% emp_id_init,]
    
    
    #change threshold ?????50
    #to find people at risk based on Threshold
    PeopleAtRisk=retention[retention$metric_value<=variable$value[variable$variable_name=="Metric8RiskThreshold"],]
    #to find people not at risk 
    peopleNotRisk=retention[!(retention$emp_id %in% PeopleAtRisk$emp_id),]
    
    #  to filter edge for mentor and current team
    edgelist=edgelist1[edgelist1$from %in% emp_id_init &
                         edgelist1$to %in% emp_id_init & 
                         edgelist1$relation=="mentor",c("from","to","weight")]
    
    
    #initiate fraction to 0
    fraction=0
    if(nrow(PeopleAtRisk)>0){
      for (i in 1:nrow(PeopleAtRisk)){
        # emp who is inflencing
        riskemp=PeopleAtRisk$emp_id[i]
        # retention risk rate of emp who is inflencing
        riskrate=1-(PeopleAtRisk$metric_value[i]/100)
        if (nrow(peopleNotRisk)>0){
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
    }  
    #Retention of tean = count of people at rsik + fraction of people whom they influence
    RetentionTeam=(nrow(PeopleAtRisk)+fraction)/length(emp_id_init)
    
    # reverse The Retention scale
    RetentionTeam=1-RetentionTeam
    
    #Round and scale 0-100
    RetentionTeam=round(RetentionTeam*100,0)
    
    #add to op table
    op=rbind(op,data.frame(initiative_id=as.numeric(initiative_id),metric_id=as.numeric(8),score=as.numeric(RetentionTeam),display_flag))  
    # retention end
    
    # innovation
    # filte edge for current team and innovation relation from Master
    edgelist=edgelist1[edgelist1$from %in% emp_id_init &
                         edgelist1$to %in% emp_id_init & 
                         edgelist1$relation=="innovation",c("from","to")]
    
    # create graph
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_init)
    
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
    
#     # calcualte mu(mean) for betweeness
#     mu=mean(between$Betweenness)
#     # calculate threshold i.e mu+sigma
#     threshold=mu+sd(between$Betweenness)
    
    #list of innovators in organization i.e betweenness above threshold
    innovators=between$emp_id[between$Rank<(nrow(between)*variable$value[variable$variable_name=="Metric9InnovatorPercentile"])]
    
    # percentage of find innovators in team
    innovatorsinteam=length(emp_id_init[emp_id_init %in% innovators])/length(emp_id_init)
    
    # innovators score
    innovationscore=variable$value[variable$variable_name=="Metric9Wt1"]*avginstrength+variable$value[variable$variable_name=="Metric9Wt2"]*sqrt(innovatorsinteam)
    
    #round and scale 0-100
    innovationscore=round(innovationscore*100,0)
    
    # add to op
    op=rbind(op,data.frame(initiative_id=as.numeric(initiative_id),metric_id=as.numeric(9),score=as.numeric(innovationscore),display_flag))  
    #sentiment
    
    # individual sentiment
    
    sentiment <- sentiment_ind[sentiment_ind$emp_id %in% emp_id_init,]
    
    # filter for edge for current team alll all relation
    edgelist=edgelist1[edgelist1$from %in% emp_id_init &
                         edgelist1$to %in% emp_id_init ,c("from","to")]
    
    #aggregate edges for dif relatin
    edgelist=unique(edgelist)
    
    # create graph
    g <- graph.data.frame(edgelist, directed=TRUE, vertices=emp_id_init)
    
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
    op=rbind(op,data.frame(initiative_id=as.numeric(initiative_id),metric_id=as.numeric(10),score=as.numeric(sentimentScore),display_flag))
    
    
  }
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  query="CREATE TABLE `initiative_metric_value_temp` (
  `metric_val_id` int(11) NOT NULL AUTO_INCREMENT,
  `initiative_id` int(11) NOT NULL,
  `metric_id` int(11) NOT NULL,
  `metric_value` double DEFAULT NULL,
  `calc_time` datetime NOT NULL,
  display_flag tinyint(1),
  PRIMARY KEY (`metric_val_id`),
  KEY `metric_id` (`metric_id`),
  CONSTRAINT `initiative_metric_value_temp_ibfk_1` FOREIGN KEY (`metric_id`) REFERENCES `metric_master` (`metric_id`)
  );"
  
  dbGetQuery(mydb,query)
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  values <- paste("(",op$initiative_id,",",op$metric_id,"," ,op$score,",'",currtime,"',",op$display_flag,")", sep="", collapse=",")
  
  queryinsert <- paste("insert into initiative_metric_value_temp (initiative_id,metric_id,metric_value,calc_time,display_flag) values ", values)
  
  dbGetQuery(mydb,queryinsert)
  
  query="insert into initiative_metric_value (initiative_id,metric_id,metric_value,calc_time,display_flag)
  select initiative_id,metric_id,metric_value,calc_time,display_flag from initiative_metric_value_temp"
  
  dbGetQuery(mydb,query)
  
  query="drop table initiative_metric_value_temp;"
  dbGetQuery(mydb,query)
  
  
  dbDisconnect(mydb)
  return(TRUE)
}

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
  latest_date=max(team_metric_value$date)
  
  d1=dcast(team_metric_value, cube_id+metric_id~date, value.var="metric_value")
  
  if (ncol(d1)<=3){
    dbDisconnect(mydb)
    return()
  }
  
  # variable
  
  query="SELECT * FROM variable;"
  res <- dbSendQuery(mydb,query)
  variable=fetch(res,-1)
  
  Threshold=variable$value[variable$variable_id==10]
  
  d1$delta_n=d1[,ncol(d1)]-d1[,ncol(d1)-1]
  
  if (ncol(d1)>=7){
    d1$delta_n_1=d1[,ncol(d1)-2]-d1[,ncol(d1)-3]
    d1$delta_n_2=d1[,ncol(d1)-4]-d1[,ncol(d1)-5]
    
    d1$a1=ifelse(d1$delta_n<=(Threshold*-1),1,0)
    d1$a2=ifelse(d1$delta_n<0 & d1$delta_n_1<0 & d1$delta_n_2<0,1,0)
    d1$a=ifelse(d1$a1==1,1,ifelse(d1$a2==1,1,0))
    dalert=d1[d1$a==1,]
    dalert$delta=ifelse(dalert$a1==0 & dalert$a2==1,dalert$delta_n+dalert$delta_n_1+dalert$delta_n_2,dalert$delta_n)
  }else{
    d1$a1=ifelse(d1$delta_n<=(Threshold*-1),1,0)
    dalert=d1[d1$a1==1,]
    dalert$delta=dalert$delta_n
  }
  
  if (nrow(dalert)>0){
    for (i in 1:nrow(dalert)){
      # insert alert and get alert_id
      metric_id=dalert$metric_id[i]
      cube_id=dalert$cube_id[i]
      score=dalert[i,as.character(latest_date)]
      delta_score=dalert$delta[i]
      people_count=0
      currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
      status="Inactive"
      if(metric_id==8){#Retention
        
        emp_id_cube=employee$emp_id[employee$cube_id==cube_id]
        
        retention <-retention_ind[retention_ind$emp_id %in% emp_id_cube,]
        
        #to find people at risk based on Threshold
        PeopleAtAlert=retention$emp_id[retention$metric_value<=variable$value[variable$variable_name=="Metric8RiskThreshold"]]
        people_count=length(PeopleAtAlert)
        
      }
      
      if(metric_id==10){#Sentiment
        emp_id_cube=employee$emp_id[employee$cube_id==cube_id]
        
        sentiment <-sentiment_ind[sentiment_ind$emp_id %in% emp_id_cube,]
        
        #to find people at risk based on Threshold
        PeopleAtAlert=sentiment$emp_id[sentiment$metric_value<=variable$value[variable$variable_name=="AlertSentimentThreshold"]]
        
        people_count=length(PeopleAtAlert)
        #insert  people
        
      }
      
      dbBegin(mydb)
      query=paste("insert into alert (cube_id,metric_id,score,delta_score,people_count,alert_time,status) 
                  values (",cube_id,",",metric_id,",",score,",",delta_score,",",people_count,",'",currtime,"','",status,"');",sep="")
      
      dbGetQuery(mydb,query)
      
      if (people_count>0){
        query = "SELECT LAST_INSERT_ID();"
        
        res <- dbSendQuery(mydb,query)
        
        alert_id<- fetch(res,-1)
        alert_id=alert_id[1,1]
        #insert  people
        values <- paste("(",alert_id,",",PeopleAtAlert,")", sep="", collapse=",")
        
        queryinsert <- paste("insert into alert_people (alert_id,emp_id) values ", values)
        dbGetQuery(mydb,queryinsert)
        
        
      }
      
      dbCommit(mydb)
      
      
    }
  }  
  dbDisconnect(mydb)
  return(TRUE)
}

JobInitStatus=function(CompanyId){
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  query=paste("call getCompanyConfig(",CompanyId,");",sep = "")
  res <- dbSendQuery(mydb,query)
  CompanyConfig=fetch(res,-1)
  
  dbDisconnect(mydb)
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  query=paste("match (i:Init) where i.Status='Pending' and i.StartDate<='",currtime,"' 
              set i.Status='Active'",sep="")
  
  cypher(graph, query)
  return(TRUE)
}
