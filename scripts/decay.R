decayfun=function(D,cbatch,currdecay){
  if (cbatch>0 & cbatch >=lastbatch){
    batchscore=if(length(D$weight[D$batch_id==cbatch])==1){D$weight[D$batch_id==cbatch]}else{0}
    score=batchscore*currdecay+decayfun(D,cbatch-1,currdecay*decay)
    return(score)
  }else{
    return(0)
  }
}
decayweigthfun=function(cbatch){
  if (cbatch>0 & cbatch >=lastbatch){
    dweight=1+decay*decayweigthfun(cbatch-1)
    return(dweight)
  }else{
    return(0)
  }
}


decay=0.5
curbatch=max(op$batch_id)
lastbatch=max(min(op$batch_id),curbatch-3)
opcal=data.frame(emp_id=as.numeric(),target_emp_id=as.numeric(),
                 rel_id=as.numeric(),weight=as.numeric())
for ( i in 1:300){
  curremp=i
  print(paste("i=",i))
  for ( j in 1:4){
    currrel=j
    sub=op[op$emp_id==curremp & op$rel_id==currrel,]
    target=unique(sub$target_emp_id)
    print(paste("j=",j))
    if(length(target)>0){
      for (k  in 1:length(target)){
        print(paste("k=",k))
        curtarget=target[k]
        sub1=sub[sub$target_emp_id==curtarget,]
        score=decayfun(sub1,curbatch,1)/decayweigthfun(curbatch)
        temp=data.frame(emp_id=curremp,target_emp_id=curtarget,rel_id=currrel,weight=score)
        opcal=rbind(opcal,temp)
      }
    }
  }
}
View(opcal)
write.csv(opcal, file = "calresult.csv")