RetentionTeam=function(Function,Position,Zone){

# library(RMySQL)
# library(reshape2)
# 
# Function=c(1,2)
# Position=c(4)
# Zone=c(8)
# 
# mydb = dbConnect(MySQL(), user='hpatel', password='hitesh16', dbname='owen')
# 
# query="Select * from individual_nw_metric_value where rel_id=1"
# 
# res <- dbSendQuery(mydb,query)
# ind_nw_metric <- fetch(res,n = -1)
# 
# query1="Select * from cube_master"
# res <- dbSendQuery(mydb,query1)
# cubemaster=fetch(res,n = -1)
# 
# query2="Select * from dimension_value"
# res <- dbSendQuery(mydb,query2)
# dimensionvalue=fetch(res,n = -1)
# 
# functionlist=dimensionvalue$dimension_val_name[dimensionvalue$dimension_val_id %in% Function]
# positionlist=dimensionvalue$dimension_val_name[dimensionvalue$dimension_val_id %in% Position]
# zonelist=dimensionvalue$dimension_val_name[dimensionvalue$dimension_val_id %in% Zone]
# 
# 
# cubelist=cubemaster$cube_id[cubemaster$Function %in% functionlist & cubemaster$Position %in% positionlist & cubemaster$Zone %in% zonelist]
# 
# query3="Select * from employee"
# res <- dbSendQuery(mydb,query3)
# employee=fetch(res,n = -1)
# 
# emplist=employee$emp_id[employee$cube_id %in% cubelist]
# 
# ind_nw_metric=ind_nw_metric[ind_nw_metric$emp_id %in% emplist,]
# 
# ind_nw_metric$calc_time=strptime(ind_nw_metric$calc_time,format="%Y-%m-%d %H:%M" )
# dates=sort.POSIXlt(unique(ind_nw_metric$calc_time),decreasing = TRUE)[1:2]
# 
# 
# ind_nw_metric=ind_nw_metric[ind_nw_metric$calc_time %in% dates,]
# 
#
  return(2)
}