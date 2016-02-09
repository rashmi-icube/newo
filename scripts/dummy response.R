setwd("C:\\Users\\Hitendra\\Desktop\\sqldb\\survey response")
question=read.csv("question.csv",stringsAsFactors = FALSE)

question$startdate=as.Date(question$startdate,"%Y-%m-%d")
question$enddate=as.Date(question$enddate,"%Y-%m-%d")

today <- Sys.Date()
question=question[question$startdate<=today,]

employee=read.csv("employee.csv")
me_response=data.frame(emp_id=as.numeric(),que_id=as.numeric(),response_time=as.character(),semtiment_weight=as.character(),rel_id=as.numeric())
we_response=data.frame(emp_id=as.numeric(),que_id=as.numeric(),response_time=as.character(),target_emp_id=as.character(),rel_id=as.numeric(),weight=as.character())

for (i in 1:nrow(question)){
  que_id=question$que_id[i]
  respondent=sample(1:300,sample(210:280,1),replace=FALSE)
  que_type=question$que_type[i]
  startdate=question$startdate[i]
  enddate=question$enddate[i]
  rel_id=question$rel_id[i]
  print(i)
  for (j in 1:length(respondent)){
    emp_id=respondent[j]
    days=as.numeric(difftime(enddate,startdate,units = "days"))+1
    secs=days*24*60*60-2
    startdate=as.POSIXct(strptime(paste(startdate,' 00:00:01'),"%Y-%m-%d %H:%M:%S"))
    response_time=startdate+sample(1:secs,1)
   if (que_type==0){
      response=sample(1:5,1)
      sentiment_weight=sample(1:5,1)
      me_response=rbind(me_response,data.frame(emp_id,que_id,response_time,sentiment_weight,rel_id))
    }else{
      wesub=we_response[we_response$emp_id==emp_id & we_response$rel_id==rel_id,]
      currcube=employee$cube_id[employee$emp_id==emp_id]  
      currcubeemp=employee$emp_id[employee$cube_id==currcube]
      currcubeempnot=employee$emp_id[!(employee$emp_id %in% currcubeemp)]
      if(nrow(wesub)==0){
        target_emp_id_cube=currcubeemp[sample(1:length(currcubeemp),min(sample(4:7,1),length(currcubeemp)),replace = FALSE)]
        target_emp_id_cubenot=currcubeempnot[sample(1:length(currcubeempnot),min(sample(1:3,1),length(currcubeempnot)),replace = FALSE)]
        target_emp_id=append(target_emp_id_cube,target_emp_id_cubenot)
      }else{
        target_emp_id_current=wesub$target_emp_id
        target_emp_id_same=target_emp_id_current[sample(1:length(target_emp_id_current),round(length(target_emp_id_current)*0.8,0),replace = FALSE)]
        target_emp_id_cube=currcubeemp[sample(1:length(currcubeemp),min(sample(1:2,1),length(currcubeemp)),replace = FALSE)]
        target_emp_id_cubenot=currcubeempnot[sample(1:length(currcubeempnot),min(sample(1:2,1),length(currcubeempnot)),replace = FALSE)]
        target_emp_id=append(target_emp_id_same,target_emp_id_cube,target_emp_id_cubenot)
        target_emp_id=unique(target_emp_id)
        #print(paste("done for question ",i))
      }
      weight=sample(1:5,length(target_emp_id),replace = TRUE)
      we_response=rbind(we_response,data.frame(emp_id,que_id,response_time,target_emp_id,rel_id,weight))
    }
  }
}

write.csv(me_response,"me_response.csv")
write.csv(we_response,"we_response.csv")
