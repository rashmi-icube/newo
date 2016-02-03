betweennessind=function(){
  library(RMySQL)
  library(RNeo4j)
  library(igraph)
  library(moments)
  
  mydb = dbConnect(MySQL(), user='hpatel', password='hitesh16', dbname='owen')
  
  relquery="Select * from relationship_master"
  
  res <- dbSendQuery(mydb,relquery)
  relationship_master <- fetch(res)
  
  graph = startGraph("http://localhost:7474/db/data/", username="neo4j", password="hitesh16")
  
  querynode = "match (a:Employee) return a.emp_id"
  vertexlist=cypher(graph, querynode)
  
  op=data.frame(nw_metric_value=as.numeric(),emp_id=as.numeric(),rel_id=as.numeric())
  for (i in 1:nrow(relationship_master)){
    rel_id=relationship_master$rel_id[i]
    if (relationship_master$rel_name[i]!="All"){
      rel=relationship_master$rel_name[i]
      
      queryedge = paste("match (a:Employee),(b:Employee)
                        with a,b
                        match a-[r:",rel,"]->b
                        return a.emp_id as from ,b.emp_id as to",sep="")
      
      edgelist = cypher(graph, queryedge)
      
      g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
      
      between=data.frame(betweenness(g,normalized = TRUE))
      # normalized
      names(between)="nw_metric_value"
      between$emp_id=row.names(between)
      between$rel_id=rel_id
      op=rbind(op,between)
    }else{
      queryedge = "match (a:Employee),(b:Employee)
      with a,b
      match a-[r:innovation|mentor|social|learning]->b
      return a.emp_id as from ,b.emp_id as to"
      
      edgelist = cypher(graph, queryedge)
      edgelist=unique(edgelist)
      
      g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
      
      between=data.frame(betweenness(g,normalized = TRUE))
      names(between)="nw_metric_value"
      between$emp_id=row.names(between)
      between$rel_id=rel_id
      op=rbind(op,between)
    }
  }
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  values <- paste("(",op$emp_id,",3,", op$nw_metric_value,",'",currtime,"',",op$rel_id,")", sep="", collapse=",")
  
  queryinsert <- paste("insert into individual_nw_metric_value (emp_id,nw_metric_id,nw_metric_value,calc_time,rel_id) values ", values)
  
  dbGetQuery(mydb,queryinsert)
  
}