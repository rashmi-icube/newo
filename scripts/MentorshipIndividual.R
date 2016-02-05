MentorshipInd=function(emp_id){
  library(RNeo4j)
  library(igraph)
  library(moments)
  
  graph = startGraph("http://localhost:7474/db/data/", username="neo4j", password="hitesh16")
  
  queryedge = paste("match (a:Employee {emp_id:",emp_id,"})-[r:mentor]->(b:Employee)
                    return sum(r.weight)",sep="")
  
  score= cypher(graph, queryedge)
  
  return(score[1,1])
}
