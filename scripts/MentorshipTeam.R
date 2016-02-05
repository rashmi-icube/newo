MentorshipTeam=function(Function,Position,Zone){
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
                    match a-[r:mentor]->b
                    return a.emp_id as from ,b.emp_id as to ,r.weight as weight",sep="")
  
  edgelist = cypher(graph, queryedge)
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  
  instrength=strength(g,mode="in")
  mn=min(instrength)
  mx=max(instrength)
  threshold=(mx-mn)/4*3+mn
  leadershipscore=length(instrength[instrength>threshold])/nrow(vertexlist)*100
  return(leadershipscore)
}
