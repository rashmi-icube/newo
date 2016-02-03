InnovationTeam=function(Function,Position,Zone){
  library(RNeo4j)
  library(igraph)
  library(moments)
  
  graph = startGraph("http://localhost:7474/db/data/", username="neo4j", password="hitesh16")
  
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
  return(innovationscore)
}
