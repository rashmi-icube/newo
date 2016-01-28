palindrome <- function(p) {
    for(i in 1:floor(nchar(p)/2) ) {
        r <- nchar(p) - i + 1
        if ( substr(p, i, i) != substr(p, r, r) ) return(FALSE)
    }
    TRUE
}

Performance=function(Function,Position,Zone){
  library(RNeo4j)
  library(igraph)
  library(moments)
  
  graph = startGraph("http://localhost:7474/db/data/", username="neo4j", password="hitesh16")
    
  querynode = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
              a-[:is_positioned]->(p:Position) 
                    where f.Id in ['",paste(Function,collapse="','"),"'] and p.Id in ['",paste(Position,collapse="','"),"'] and z.Id in ['",paste(Zone,collapse="','"),"']
                    return a.emp_id",sep="")
  
  
  vertexlist=cypher(graph, querynode)
  
  queryedge = paste("match (z:Zone)<-[:from_zone]-(a:Employee)-[:has_functionality]->(f:Function),
                    (z:Zone)<-[:from_zone]-(b:Employee)-[:has_functionality]->(f:Function),
                    a-[:is_positioned]->(p:Position)<-[:is_positioned]-b
                    where f.Id in ['",paste(Function,collapse="','"),"'] and p.Id in ['",paste(Position,collapse="','"),"'] and z.Id in ['",paste(Zone,collapse="','"),"']
                    with a,b
                    match a-[r:learning]->b
                    return a.emp_id as from ,b.emp_id as to ,r.weight as weight",sep="")
  
  edgelist = cypher(graph, queryedge)
  
  g <- graph.data.frame(edgelist, directed=TRUE, vertices=vertexlist)
  
  instrength=strength(g,mode="in")
  
  skew=skewness(instrength)
  
  den=graph.density(g)
  
  Performancescore=0.7*den+0.3*skew
  return(Performancescore)


}
