library(RMySQL)
library(dplyr)
library(RNeo4j)

args = commandArgs(trailingOnly=TRUE)

setwd("C:\\Users\\Hitendra\\Desktop\\R metric Function")
source('config.R')

update_neo=function(CompanyId){
  
  mydb = dbConnect(MySQL(), user=mysqlusername, password=mysqlpasswod, dbname=mysqldbname, host=mysqlhost, port=mysqlport)
  
  query=paste("call getCompanyConfig(",CompanyId,");",sep = "")
  res <- dbSendQuery(mydb,query)
  CompanyConfig=fetch(res,-1)
  
  dbDisconnect(mydb)
  
  comp_sql_dbname=CompanyConfig$comp_sql_dbname[1]
  comp_sql_server=CompanyConfig$sql_server[1]
  comp_sql_user_id=CompanyConfig$sql_user_id[1]
  comp_sql_password=CompanyConfig$sql_password[1]
  
  mydb = dbConnect(MySQL(), user=comp_sql_user_id, password=comp_sql_password, dbname=comp_sql_dbname, host=comp_sql_server, port=mysqlport)
  
  
  TableName=paste("calculated_edge_",format(Sys.Date(), "%Y_%m_%d"),sep = "")
  
  query=paste("SELECT * FROM ",TableName," ;",sep = "")
  res <- dbSendQuery(mydb,query)
  edgelist=fetch(res,-1)
  
  query="SELECT * FROM relationship_master"
  res <- dbSendQuery(mydb,query)
  relationship_master=fetch(res,-1)
  
  edgelist=merge(edgelist,relationship_master,by="rel_id")
  
  dbDisconnect(mydb)
  
  com_neopath=CompanyConfig$neo_db_url[1]
  com_neopath=paste(com_neopath,"/db/data/",sep = "")
  
  com_neousername=CompanyConfig$neo_user_name[1]
  com_neopassword=CompanyConfig$neo_passsword[1]
  
  graph = startGraph(com_neopath, username=com_neousername, password=com_neopassword)
  
  
  querynode = "match (a:Employee)-[r:innovation|learning|mentor|social]->(b:Employee) delete r"
  
  cypher(graph, querynode) 
  
  for (k in 1:4){
    edgelistrel=edgelist[edgelist$rel_id==k,]
    
    relname=relationship_master$rel_name[relationship_master$rel_id==k]
    
    query = paste("MATCH (a:Employee {emp_id:{from}}),(b:Employee {emp_id:{to}})
                  CREATE (a)-[r:",relname," {weight:toFloat({wt})}]->(b)",sep="")
    
    tx = newTransaction(graph)
    
    for (i in 1:nrow(edgelistrel)) {
      # Upload in blocks of 1000.
      if(i %% 1000 == 0) {
        # Commit current transaction.
        commit(tx)
        print(paste("Batch", i / 1000, "committed."))
        # Open new transaction.
        tx = newTransaction(graph)
      }
      
      # Append paramaterized Cypher query to transaction.
      appendCypher(tx,
                   query,
                   from = edgelistrel$emp_from[i],
                   to = edgelistrel$emp_to[i],
                   wt=edgelistrel$weight[i]
      )
      
    }
    
    # Commit last transaction.
    commit(tx)
    print("Last batch committed.")
    print("All done! for ")
    print(relname)
    return("TRUE")
  }
  
}

update_neo(args[1])

