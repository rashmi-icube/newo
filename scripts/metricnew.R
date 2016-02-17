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
  #mydb = dbConnect(MySQL(), user='hpatel', password='hitesh16', dbname='owen')
  
  #mydb = dbConnect(MySQL(), user='hpatel', password='hitesh16', dbname='owen', host='192.168.1.13', port=3306)
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  
  if(length(Function)==1 & length(Position)==1 & length(Zone==1)){
    
    # to get cube_id
    query=paste("call getCubeIdFromDimension(",Function,",",Position,",",Zone,");",sep = "")
    
    res <- dbSendQuery(mydb,query)
    # cube_id
    cube_id <- fetch(res,-1)
    cube_id=cube_id[1,1]
    
    # don't know y need to disconnect and connect again
    dbDisconnect(mydb)
    #mydb = dbConnect(MySQL(), user='hpatel', password='hitesh16', dbname='owen')
    mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
    
    
    query=paste("SELECT * FROM team_metric_value where cube_id=",cube_id," order by metric_val_id desc limit 5;",sep = "")
    
    res <- dbSendQuery(mydb,query)
    # cube_id
    op <- fetch(res,-1)
    op=op[,c("metric_id","metric_value")]
    names(op)=c("metric_id","op")
    
    dbDisconnect(mydb)
    return(op)
  }
  #op data frame
  op=data.frame(metric_id=as.numeric(),score=as.numeric())
  
  # graph DB connection
  #graph = startGraph("http://localhost:7474/db/data/", username="neo4j", password="hitesh16")
  
  graph = startGraph(neopath, username=neousername, password=neopassword)
  
  
  # Query to get nodes of current Team
  querynode = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                    a-[:is_positioned]->(p:Position) 
                    where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                    return a.emp_id",sep="")
  
  #nodes of current Team
  vertexlist=cypher(graph, querynode) 
  
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
  
  # calculate density of current team
  DensityTeam=graph.density(g)
  
  # calcualte strength of ind in team (incomming)
  indegree=degree(g,mode="in")
  
  #claculate Skewness
  skew=skewness(indegree)
  
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
  Performancescore=0.7*sqrt(DensityTeam)+0.3*sqrt(skew)
  
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
  
  # calcualte density of team
  socialcohesionscore=graph.density(g)
  
  # square root to scale up
  socialcohesionscore=sqrt(socialcohesionscore)
  
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
  PeopleAtRisk=indretention[indretention$metric_value<=50,]
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
  
  # calculate denity of team
  DensityTeam=graph.density(g)
  
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
  
  # calcualte mu(mean) for betweeness
  mu=mean(between$Betweenness)
  # calculate threshold i.e mu+sigma
  threshold=mu+sd(between$Betweenness)
  
  #list of innovators in organization i.e betweenness above threshold
  innovators=between$emp_id[between$Betweenness>threshold]
  
  # percentage of find innovators in team
  innovatorsinteam=length(vertexlist$a.emp_id[vertexlist$a.emp_id %in% innovators])/nrow(vertexlist)
  
  # innovators score
  innovationscore=0.3*sqrt(DensityTeam)+0.7*sqrt(innovatorsinteam)
  
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
  
  # add to op
  op=rbind(op,data.frame(metric_id=10,score=sentimentScore))
  
  dbDisconnect(mydb)
  
  return(op)
  
}

IndividualMetric=function(emp_id){
  library(RNeo4j)
  library(igraph)
  library(moments)
  library(RMySQL)
  library(reshape2)
  
  
  op=data.frame(metric_id=as.numeric(),score=as.numeric())
  
  graph = startGraph("http://localhost:7474/db/data/", username="", password="")
  
  mydb = dbConnect(MySQL(), user='icube', password='icube123', dbname='owen')
  
  # expertise
  queryedge = paste("match (a:Employee {emp_id:",emp_id,"})-[r:learning]->(b:Employee)
                    return sum(r.weight)",sep="")
  
  score= cypher(graph, queryedge)
  
  expertisescore=score[1,1]
  
  op=rbind(op,data.frame(metric_id=1,score=expertisescore))
  
  queryedge = paste("match (a:Employee {emp_id:",emp_id,"})-[r:mentor]->(b:Employee)
                    return sum(r.weight)",sep="")
  
  score= cypher(graph, queryedge)
  
  mentorshipscore=score[1,1]
  
  op=rbind(op,data.frame(metric_id=2,score=mentorshipscore))
  
  # Retention
  op=rbind(op,data.frame(metric_id=3,score=4))
  
  #influence
  querynode = "match (a:Employee) return a.emp_id"
  
  vertexlist1=cypher(graph, querynode)
  
  queryedge = "match (a:Employee),(b:Employee) 
  with a,b
  match a-[r:innovation]->b
  return a.emp_id as from ,b.emp_id as to "
  
  edgelist1 = cypher(graph, queryedge)
  
  g1 <- graph.data.frame(edgelist1, directed=TRUE, vertices=vertexlist1)
  
  influencescore=betweenness(g1,v=c(emp_id))
  
  op=rbind(op,data.frame(metric_id=4,score=influencescore))
  
  #sentimnet
  query=paste("Select * from me_response where emp_id=",emp_id,sep = "")
  
  res <- dbSendQuery(mydb,query)
  me_response <- fetch(res,n = -1)
  
  # FILTER FOR LATEST RESPONSE
  
  sentimentscore=mean(me_response$sentiment_weight)
  
  op=rbind(op,data.frame(metric_id=5,score=sentimentscore))
  
  
  return(op)
}