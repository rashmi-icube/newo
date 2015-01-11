library(RNeo4j)
library(igraph)
library(moments)
library(RMySQL)
library(reshape2)

#setwd("C:\\Users\\Hitendra\\Desktop\\R metric Function")
#Function=c(1)
#Position=c(4)
#Zone=c(8)

source('config.R')

TeamMetric=function(Function,Position,Zone){
  
  # sql DB connection
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
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
  
  graph = startGraph(neopath, username=neousername, password=neopassword)
  
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
  
  # sql query to get max instrength for relation learning
  query=paste("SELECT max(t1.nw_metric_value) as Score FROM individual_nw_metric_value as t1
              where t1.nw_metric_id=2 and t1.rel_id=4 and t1.calc_time=
              (select max(t2.calc_time) from individual_nw_metric_value as t2
              where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
              ,sep="")
  
  #run query
  res <- dbSendQuery(mydb,query)
  
  maxinstrength <- fetch(res,-1)
  maxinstrength=maxinstrength[1,1]
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
  
  # sql query to get max instrength for relation social
  query=paste("SELECT max(t1.nw_metric_value) as Score FROM individual_nw_metric_value as t1
              where t1.nw_metric_id=2 and t1.rel_id=3 and t1.calc_time=
              (select max(t2.calc_time) from individual_nw_metric_value as t2
              where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
              ,sep="")
  
  #run query
  res <- dbSendQuery(mydb,query)
  
  maxinstrength <- fetch(res,-1)
  maxinstrength=maxinstrength[1,1]
  # normalize by dividing with max of in strength overall
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
  query=paste("SELECT max(t1.nw_metric_value) as Score FROM individual_nw_metric_value as t1
              where t1.nw_metric_id=2 and t1.rel_id=1 and t1.calc_time=
              (select max(t2.calc_time) from individual_nw_metric_value as t2
              where t2.nw_metric_id=t1.nw_metric_id and t2.rel_id=t1.rel_id and t1.emp_id=t2.emp_id);"
              ,sep="")
  
  #run query
  res <- dbSendQuery(mydb,query)
  
  maxinstrength <- fetch(res,-1)
  maxinstrength=maxinstrength[1,1]
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
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  query=paste("select rel_name from relationship_master where rel_id=",rel_id,";",sep="")
  
  res <- dbSendQuery(mydb,query)
  
  relname<- fetch(res,-1)
  
  dbDisconnect(mydb)  
  
  relname=relname[1,1]
  
  graph = startGraph(neopath, username=neousername, password=neopassword)
  
  querynode = paste("match (a:Employee {emp_id:",emp_id,"})-[r:",relname,"]->(b:Employee) 
                    return b.emp_id as emp_id,r.weight as weight"
                    ,sep="")
  
  FirstConn=cypher(graph, querynode)
  
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

TeamSmartList=function(Function,Position,Zone,init_type_id){
  # condition to replace all(0) with der dimension_id
  cat("\nData Received Function=",Function,"Position=",Position,"Zone=",Zone,"init_type=",init_type_id,file="Rlog.txt",sep=" ",append=TRUE)
  if(Function==0 || Position==0 || Zone==0){
    mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
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
    dbDisconnect(mydb)
  }
  
  # graph connection
  graph = startGraph(neopath, username=neousername, password=neopassword)
  
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
    mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
    
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
    dbDisconnect(mydb)
    cat("\n done calculating for type 10",file="Rlog.txt",sep=" ",append=TRUE)  
  }
  
  # Rank Score in descending order
  op$Rank=rank(-op$Score,ties.method= "random")
  # flag high medium low
  op$flag=ifelse(op$Rank<=nrow(op)/3,"High",ifelse(op$Rank<=nrow(op)*2/3,"Medium","Low"))
  # return op
  op$emp_id=as.integer(op$emp_id)
  op$Rank=as.integer(op$Rank)
  cat("\n Returning op for Team SmartList",file="Rlog.txt",sep=" ",append=TRUE)  
  return(op)
}

IndividualSmartList=function(emp_id,init_type_id){
  # connect neo
  graph = startGraph(neopath, username=neousername, password=neopassword)
  #sql db connection
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
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
  op$flag=ifelse(op$Rank<=nrow(op)/3,"High",ifelse(op$Rank<=nrow(op)*2/3,"Medium","Low"))
  
  op$emp_id=as.integer(op$emp_id)
  op$Rank=as.integer(op$Rank)
  
  # return op
  return(op)
}
