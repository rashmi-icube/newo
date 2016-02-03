AveragePathLength=function(){
  library(RMySQL)
  library(RNeo4j)
  library(igraph)
  library(moments)
  
  mydb = dbConnect(MySQL(), user='hpatel', password='hitesh16', dbname='owen')
  
  empquery="Select * from employee"
  
  res <- dbSendQuery(mydb,empquery)
  employee <- fetch(res)
  
  cubequery="Select * from cube_master"
  
  res <- dbSendQuery(mydb,cubequery)
  cube_master <- fetch(res)
  
  relquery="Select * from relationship_master"
  
  res <- dbSendQuery(mydb,relquery)
  relationship_master <- fetch(res)
  
  graph = startGraph("http://localhost:7474/db/data/", username="neo4j", password="hitesh16")
  
  querynode = "match (a:Employee) return a.emp_id"
  vertexlist=cypher(graph, querynode)
  
  queryedge = "match (a:Employee),(b:Employee)
  with a,b
  match a-[r]->b
  return a.emp_id as from ,b.emp_id as to,type(r) as Relation_Type,r.weight as weight"
  
  edgelist = cypher(graph, queryedge)
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  op=data.frame(nw_metric_value=as.numeric(),cube_id=as.numeric(),rel_id=as.numeric())
  for (i in 1:nrow(cube_master)){
    cube_id=cube_master$cube_id[i]
    emplist=employee$emp_id[employee$cube_id==cube_id]
    for (j in 1:nrow(relationship_master)){
      rel_id=relationship_master$rel_id[j]
      if (relationship_master$rel_name[j]!="All"){
        rel=relationship_master$rel_name[j]
        g1<- induced_subgraph(g, which(V(g)$name %in% emplist))
        g2=subgraph.edges(g1, which(E(g1)$Relation_Type==rel), delete.vertices = FALSE)
        nw_metric_value=average.path.length(g2)
        op=rbind(op,data.frame(nw_metric_value,cube_id,rel_id))
      }else{
        g1<- induced_subgraph(g, which(V(g)$name %in% emplist))
        vert=get.vertex.attribute(g1)
        edg=data.frame(get.edgelist(g1))
        edg=unique(edg)
        g2 <- graph.data.frame(edg, directed=TRUE,vertices = vert)
        nw_metric_value==average.path.length(g2)
        op=rbind(op,data.frame(nw_metric_value,cube_id,rel_id))
      }
      
    }
  }
  
  op[is.na(op)] <- 0
  
  currtime=format(Sys.time(), "%Y-%m-%d %H:%M:%S")
  
  values <- paste("(",op$cube_id,",7,", op$nw_metric_value,",'",currtime,"',",op$rel_id,")", sep="", collapse=",")
  
  queryinsert <- paste("insert into team_nw_metric_value (cube_id,nw_metric_id,nw_metric_value,calc_time,rel_id) values ", values)
  
  dbGetQuery(mydb,queryinsert)
  
}
