sentimentInd=function(emp_id){
  library(RMySQL)
  library(reshape2)
  
  mydb = dbConnect(MySQL(), user='hpatel', password='hitesh16', dbname='owen')
  
  query=paste("Select * from me_response where emp_id=",emp_id,sep = "")
  
  res <- dbSendQuery(mydb,query)
  me_response <- fetch(res,n = -1)
  
  # FILTER FOR LATEST RESPONSE
  
  score=mean(me_response$sentiment_weight)
  return(score)
}  

