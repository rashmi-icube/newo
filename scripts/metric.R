TeamMetric=function(Function,Position,Zone){
  #library(RNeo4j)
  #library(igraph)
  #library(moments)
  
  #graph = startGraph("http://localhost:7474/db/data/", username="neo4j", password="hitesh16")
  
  op=data.frame(init_id=c(6:10),score=sample(1:5,5,replace = TRUE))
  return(op)
}

IndividualMetric=function(Function,Position,Zone){
#   library(RNeo4j)
#   library(igraph)
#   library(moments)
#   
#   graph = startGraph("http://localhost:7474/db/data/", username="neo4j", password="hitesh16")
#   
  op=data.frame(init_id=c(1:5),score=sample(1:5,5,replace = TRUE))
  return(op)
}