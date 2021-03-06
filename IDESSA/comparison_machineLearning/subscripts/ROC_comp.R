
predictionFiles=paste0(resultpath,"/",Filter(function(x) grepl("RData", x), list.files(resultpath,pattern="prediction")))
for (i in predictionFiles){
  load(i)
}

load(paste(resultpath,"/predictorVariables.RData",sep=""))
load(paste(resultpath,"/testing.RData",sep=""))

##################################################################################################################
#                                         ROC
##################################################################################################################

if (response=="RInfo"){
  if (any(model=="rf")){
    obs=as.numeric(prediction_rf$observed)
    obs[as.character(prediction_rf$observed)=="norain"]=0
    obs[as.character(prediction_rf$observed)=="rain"]=1
    pred <- prediction(prediction_rf$predicted_prob$rain,obs)
    perf_rf <- performance(pred, "tpr", "fpr") 
    auc_rf=unlist(performance(pred, measure="auc")@y.values)
  }
  if (any(model=="nnet")){
    obs=as.numeric(prediction_nnet$observed)
    obs[as.character(prediction_nnet$observed)=="norain"]=0
    obs[as.character(prediction_nnet$observed)=="rain"]=1
    pred <- prediction(prediction_nnet$predicted_prob$rain,obs)
    perf_nnet <- performance(pred, "tpr", "fpr") 
    auc_nnet=unlist(performance(pred, measure="auc")@y.values)
  }
  if (any(model=="svm")){
   obs=as.numeric(prediction_svm$observed)
    obs[as.character(prediction_svm$observed)=="norain"]=0
   obs[as.character(prediction_svm$observed)=="rain"]=1
   pred <- prediction(prediction_svm$predicted_prob$rain,obs)
    perf_svm <- performance(pred, "tpr", "fpr") 
   auc_svm=unlist(performance(pred, measure="auc")@y.values)
  }
  if (any(model=="avNNet")){
    obs=as.numeric(prediction_avNNet$observed)
    obs[as.character(prediction_avNNet$observed)=="norain"]=0
    obs[as.character(prediction_avNNet$observed)=="rain"]=1
    pred <- prediction(prediction_avNNet$predicted_prob$rain,obs)
    perf_avNNet <- performance(pred, "tpr", "fpr") 
    auc_avNNet=unlist(performance(pred, measure="auc")@y.values)
  }

  if (length(model)==4) color=c("black","red","blue","green","grey")
  if (length(model)==3) color=c("black","red","blue","grey")
  if (length(model)==2) color=c("black","red","grey")
  if (length(model)==1) color=c("black","grey")

  legendnames=c()
  for (i in 1:length(model)){
    legendnames=c(legendnames,paste(toupper(model[i]),round(eval(parse(text=paste("auc_",model[i],sep=""))),2)))
  }
  pdf(paste(resultpath,"/prediction_roc.pdf",sep=""))
  plot(eval(parse(text=paste("perf_",model[1],sep=""))),colorize=TRUE)
  plot(eval(parse(text=paste("perf_",model[1],sep=""))),col=color[1])
   for (i in 2:length(model)){
      plot(eval(parse(text=paste("perf_",model[i],sep=""))),col=color[i],add=TRUE)
    }
    lines(c(0,1),c(0,1),col="grey50")
    legend("bottomright",col=color,lty=c(1),legend=c(legendnames, "Random 0.5"),bty="n")
  dev.off()
}

##################################################################################################################
#                                                 ROC per scene With Conficence
##################################################################################################################
pdf(paste(resultpath,"/ROC_confidence.pdf",sep=""))
  col=c("black","red","blue","darkgreen","red")
  auc=c()
  aucvals=list()
  perf=list()
  for (i in 1:length(model)){
    modeldata=eval(parse(text=paste("prediction_",model[i],sep="")))
    predframe=list()
    obsframe=list()
    for (scene in 1:length(unique(eval(parse(text=paste("modeldata$",dateField,sep="")))))){
      predframe[[scene]]=modeldata$predicted_prob$rain[eval(parse(text=paste("modeldata$",dateField,sep="")))==
                                                 unique(eval(parse(text=paste("modeldata$",dateField,sep=""))))[scene]]
      obsframe[[scene]]=modeldata$observed[eval(parse(text=paste("modeldata$",dateField,sep="")))==
                                     unique(eval(parse(text=paste("modeldata$",dateField,sep=""))))[scene]]
    }

    pred <- prediction(predframe,obsframe)

    perf[[i]] <- performance(pred, "tpr", "fpr")  
    aucvals[[i]]=unlist(performance(pred, measure="auc")@y.values)
    auc[i]=mean(unlist(performance(pred, measure="auc")@y.values))
    if (i==1) plot(perf[[i]],avg="vertical",spread.estimate="stderror",col=col[i],spread.scale=2)
    if (i>1) plot(perf[[i]],avg="vertical",spread.estimate="stderror",add=TRUE,col=col[i],spread.scale=2)
    }
  lines(c(0,1),c(0,1),col="grey50")
  legend("bottomright",legend=paste(model,round(auc,3)),col=col[1:length(model)],lwd=1,bty="n")
dev.off()

 ###write all auc values per scene
 dir.create (paste(resultpath,"/Confusion_comp",sep=""))
 aucvalsOut=data.frame((sapply(aucvals,c)))
 colnames(aucvalsOut)=model
 write.csv(aucvalsOut,file=paste(resultpath,"/Confusion_comp/aucdata.csv",sep=""),row.names=FALSE)

##################################################################################################################
#                                                 Compare ROC by t-test
##################################################################################################################
ttestresults=matrix(ncol=length(aucvals),nrow=length(aucvals))

for (i in 1:(length(aucvals)-1)){
#  for (i in 1:(length(aucvals))){
  for(k in (i+1):length(aucvals)){
#    for(k in 1:length(aucvals)){
    ttestresults[i,k]=t.test(aucvals[[i]],aucvals[[k]])$p.value
  }
}

ttestresultsPaired=matrix(ncol=length(aucvals),nrow=length(aucvals))

for (i in 1:(length(aucvals)-1)){
  #  for (i in 1:(length(aucvals))){
  for(k in (i+1):length(aucvals)){
    #    for(k in 1:length(aucvals)){
    ttestresultsPaired[i,k]=t.test(aucvals[[i]],aucvals[[k]],paired=T)$p.value
  }
}

colnames(ttestresults)=model
rownames(ttestresults)=model
colnames(ttestresultsPaired)=model
rownames(ttestresultsPaired)=model

write.csv(ttestresults,file=paste(resultpath,"/ttest_ROC.csv",sep=""))
write.csv(ttestresultsPaired,file=paste(resultpath,"/ttest_Paired_ROC.csv",sep=""))


##################################################################################################################
#                                                 Compare ROC by wilcoxon test
##################################################################################################################
wilcoxresults=matrix(ncol=length(aucvals),nrow=length(aucvals))

for (i in 1:(length(aucvals)-1)){
  #  for (i in 1:(length(aucvals))){
  for(k in (i+1):length(aucvals)){
    #    for(k in 1:length(aucvals)){
    wilcoxresults[i,k]=wilcox.test(aucvals[[i]],aucvals[[k]])$p.value
  }
}

wilcoxresultsPaired=matrix(ncol=length(aucvals),nrow=length(aucvals))

for (i in 1:(length(aucvals)-1)){
  #  for (i in 1:(length(aucvals))){
  for(k in (i+1):length(aucvals)){
    #    for(k in 1:length(aucvals)){
    wilcoxresultsPaired[i,k]=wilcox.test(aucvals[[i]],aucvals[[k]],paired=T)$p.value
  }
}

colnames(wilcoxresults)=model
rownames(wilcoxresults)=model
colnames(wilcoxresultsPaired)=model
rownames(wilcoxresultsPaired)=model

write.csv(wilcoxresults,file=paste(resultpath,"/wilcox_ROC.csv",sep=""))
write.csv(wilcoxresultsPaired,file=paste(resultpath,"/wilcox_Paired_ROC.csv",sep=""))


##################################################################################################################
#                                                 CROSSTABS
##################################################################################################################
if (type=="classification"){
  if (any(model=="rf")){
    write.csv(table(prediction_rf$observed,prediction_rf$prediction),file=paste(resultpath,"/crosstabRF.csv",sep=""))
  }
  if (any(model=="nnet")){
    write.csv(table(prediction_nnet$observed,prediction_nnet$prediction),file=paste(resultpath,"/crosstabNNET.csv",sep=""))
  }
  if (any(model=="svm")){
    write.csv(table(prediction_svm$observed,prediction_svm$prediction),file=paste(resultpath,"/crosstabSVM.csv",sep=""))
  }
  if (any(model=="avNNet")){
    write.csv(table(prediction_avNNet$observed,prediction_avNNet$prediction),file=paste(resultpath,"/crosstabavNNet.csv",sep=""))
  }
###################
  pdf(paste(resultpath,"/prediction_crosstab.pdf",sep=""))
  if (length(model)>2) par(mfrow=c(2,2))
  if (any(model=="rf")){
    plot(prediction_rf$observed,prediction_rf$prediction,main="RF",xlab="observed",ylab="predicted")
  }
  if (any(model=="nnet")){
    plot(prediction_nnet$observed,prediction_nnet$prediction,main="NNET",xlab="observed",ylab="predicted")
  }
  if (any(model=="svm")){
    plot(prediction_svm$observed,prediction_svm$prediction,main="SVM",xlab="observed",ylab="predicted")
  }
  if (any(model=="avNNet")){
   plot(prediction_avNNet$observed,prediction_avNNet$prediction,main="avNNet",xlab="observed",ylab="predicted")
  }
  dev.off()
}

