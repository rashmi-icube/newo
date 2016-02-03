sentimentTeam=function(Function,Position,Zone){
  library(RMySQL)
  library(reshape2)

# Function=c(1,2)
# Position=c(4)
# Zone=c(8)

  mydb = dbConnect(MySQL(), user='hpatel', password='hitesh16', dbname='owen')

  query="Select * from me_response"

  res <- dbSendQuery(mydb,query)
  me_response <- fetch(res,n = -1)
  sentiment=aggregate(me_response$que_id,by=list(emp_id=me_response$emp_id),max)
  
  sentiment$score=me_response$sentiment_weight[me_response$emp_id==sentiment$emp_id & me_response$que_id==sentiment$x]
  
  abc=merge(sentiment,me_response[,c("emp_id","que_id","sentiment_weight")],by.x = c("emp_id","x"),by.y = c("emp_id","que_id"),all.x = TRUE)
  
  query1="Select * from cube_master"
  res <- dbSendQuery(mydb,query1)
  cubemaster=fetch(res,n = -1)

  query2="Select * from dimension_value"
  res <- dbSendQuery(mydb,query2)
  dimensionvalue=fetch(res,n = -1)
  
  functionlist=dimensionvalue$dimension_val_name[dimensionvalue$dimension_val_id %in% Function]
  positionlist=dimensionvalue$dimension_val_name[dimensionvalue$dimension_val_id %in% Position]
  zonelist=dimensionvalue$dimension_val_name[dimensionvalue$dimension_val_id %in% Zone]
  
  
  cubelist=cubemaster$cube_id[cubemaster$Function %in% functionlist & cubemaster$Position %in% positionlist & cubemaster$Zone %in% zonelist]
  
  query3="Select * from employee"
  res <- dbSendQuery(mydb,query3)
  employee=fetch(res,n = -1)
  
  emplist=employee$emp_id[employee$cube_id %in% cubelist]
  
  abc=abc[abc$emp_id %in% emplist,]
  
  sentimentscore=mean(abc$sentiment_weight)
  return(sentimentscore)
}  

