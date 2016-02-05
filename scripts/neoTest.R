TestFunction <- function() {
  library('bitops')
  library('RCurl')
  library('RJSONIO')
  querystring="match (a:Employee) return a.emp_id,a.FirstName"
  h = basicTextGatherer()
  curlPerform(url="localhost:7474/db/data/cypher",
              postfields=paste('query',curlEscape(querystring), sep='='),
              writefunction = h$update,
              verbose = FALSE
  )           
  result <- fromJSON(h$value())
  #print(result)
  data <- data.frame(t(sapply(result$data, unlist)))
  #print(data)
  names(data) <- result$columns
  if(nrow(data)>1){
    return("Rashmi succeeded")
  }else{
    return("Rashmi Filed")
  }
  
}
