InfluenceInd=function(emp_id){
  library(RNeo4j)
  library(igraph)
  library(moments)
  
  graph = startGraph("http://localhost:7474/db/data/", username="neo4j", password="hitesh16")
  
  querynode = "match (a:Employee) return a.emp_id"
  
  vertexlist1=cypher(graph, querynode)
  
  queryedge = "match (a:Employee),(b:Employee) 
  with a,b
  match a-[r:innovation]->b
  return a.emp_id as from ,b.emp_id as to "
  
  edgelist1 = cypher(graph, queryedge)
  
  g1 <- graph.data.frame(edgelist1, directed=TRUE, vertices=vertexlist1)
  
  between=betweenness(g1,v=c(emp_id))
  
  return(between)
}
