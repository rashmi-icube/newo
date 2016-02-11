TeamMetric=function(Function,Position,Zone){
  library(RNeo4j)
  library(igraph)
  library(moments)
  library(RMySQL)
  library(reshape2)
  
  
  op=data.frame(metric_id=as.numeric(),score=as.numeric())
  
  graph = startGraph("http://localhost:7474/db/data/")  
  
  mydb = dbConnect(MySQL(), user='icube', password='icube123', dbname='owen', host='192.168.1.6', port=3306)
  
  #mydb = dbConnect(MySQL(), user='root', password='', dbname='owen', host='127.0.0.1', port=3306)
  
  querynode = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
              a-[:is_positioned]->(p:Position) 
                    where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                    return a.emp_id",sep="")
  
  
  vertexlist=cypher(graph, querynode)
  
  queryedge = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                    (z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]->(f:Function),
                    a-[:is_positioned]->(p:Position)<-[:is_positioned]-b
                    where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                    with a,b
                    match a-[r:learning]->b
                    return a.emp_id as from ,b.emp_id as to ,r.weight as weight",sep="")
  
  edgelist = cypher(graph, queryedge)
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  
  instrength=strength(g,mode="in")
  
  skew=skewness(instrength)
  
  den=graph.density(g)
  
  Performancescore=0.7*den+0.3*skew
  
  op=rbind(op,data.frame(metric_id=6,score=Performancescore))
  
  queryedge = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                    (z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]->(f:Function),
                    a-[:is_positioned]->(p:Position)<-[:is_positioned]-b
                    where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                    with a,b
                    match a-[r:social]->b
                    return a.emp_id as from ,b.emp_id as to ,r.weight as weight",sep="")
  
  edgelist = cypher(graph, queryedge)
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  
  socialcohesionscore=graph.density(g)
  
  op=rbind(op,data.frame(metric_id=7,score=socialcohesionscore))
  
  # retention
  
  
  # retention end
  op=rbind(op,data.frame(metric_id=8,score=5))
  
  
  # innovation
  queryedge = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                    (z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]->(f:Function),
                    a-[:is_positioned]->(p:Position)<-[:is_positioned]-b
                    where f.Id in [",paste(Function,collapse=","),"] and p.Id in [",paste(Position,collapse=","),"] and z.Id in [",paste(Zone,collapse=","),"]
                    with a,b
                    match a-[r:innovation]->b
                    return a.emp_id as from ,b.emp_id as to ,r.weight as weight",sep="")
  
  edgelist = cypher(graph, queryedge)
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  
  den=graph.density(g)
  
  querynode = "match (a:Employee) return a.emp_id"
  
  vertexlist1=cypher(graph, querynode)
  
  queryedge = "match (a:Employee),(b:Employee) 
  with a,b
  match a-[r:innovation]->b
  return a.emp_id as from ,b.emp_id as to "
  
  edgelist1 = cypher(graph, queryedge)
  
  g1 <- graph.data.frame(edgelist1, directed=TRUE, vertices=vertexlist1)
  
  between=data.frame(betweenness(g1))
  names(between)="Betweenness"
  between$emp_id=row.names(between)
  
  mu=mean(between$Betweenness)
  threshold=mu+sd(between$Betweenness)
  
  innovators=between$emp_id[between$Betweenness>threshold]
  
  innovatorsinteam=length(vertexlist$a.emp_id[vertexlist$a.emp_id %in% innovators])/nrow(vertexlist)
  
  innovationscore=0.3*den+0.7*innovatorsinteam
  
  op=rbind(op,data.frame(metric_id=9,score=innovationscore))
  
  #sentiment
  query="Select * from me_response"
  
  res <- dbSendQuery(mydb,query)
  me_response <- fetch(res,n = -1)
  sentiment=aggregate(me_response$que_id,by=list(emp_id=me_response$emp_id),max)
  
  #sentiment$score=me_response$sentiment_weight[me_response$emp_id==sentiment$emp_id & me_response$que_id==sentiment$x]
  
  abc=merge(sentiment,me_response[,c("emp_id","que_id","sentiment_weight")],by.x = c("emp_id","x"),by.y = c("emp_id","que_id"),all.x = TRUE)
  
  query1="Select * from cube_master"
  res <- dbSendQuery(mydb,query1)
  cubemaster=fetch(res,n = -1)
  
  query2="Select * from dimension_value"
  res <- dbSendQuery(mydb,query2)
  dimensionvalue=fetch(res,n = -1)
  
  functionlist=dimensionvalue$dimension_val_name[dimensionvalue$dimension_val_id %in% Function]
  positionlist=dimensionvalue$dimension_val_name[dimensionvalue$dimension_val_id %in% Position]
  zonelist=dimensionvalue$dimension_val_name[dimensionvalue$dimension_val_id %in% Zone]
  
  
  cubelist=cubemaster$cube_id[cubemaster$Function %in% functionlist & cubemaster$Position %in% positionlist & cubemaster$Zone %in% zonelist]
  
  query3="Select * from employee"
  res <- dbSendQuery(mydb,query3)
  employee=fetch(res,n = -1)
  
  emplist=employee$emp_id[employee$cube_id %in% cubelist]
  
  abc=abc[abc$emp_id %in% emplist,]
  
  sentimentscore=mean(abc$sentiment_weight)
  
  op=rbind(op,data.frame(metric_id=10,score=sentimentscore))
  
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
  
  mydb = dbConnect(MySQL(), user='icube', password='icube123', dbname='owen', host='192.168.1.6', port=3306)
    
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
  
  dbDisconnect(mydb)
  
  return(op)
}