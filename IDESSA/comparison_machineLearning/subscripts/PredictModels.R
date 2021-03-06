

load(paste(resultpath,"/testing.RData",sep=""))

load(paste(resultpath,"/predictorVariables.RData",sep=""))


####################################### PREDICT #######################################
testing_predictors <-testing[,names(testing) %in%  predictorVariables]
testing_observed<-eval(parse(text=paste("testing$",response,sep="")))


if (any(model=="rf")){
  load(paste(resultpath,"/fit_rf.RData",sep=""))
  prediction_rf=data.frame("prediction"=predict (fit_rf,testing_predictors))
  if (type=="classification") prediction_rf$predicted_prob <- predict (fit_rf,testing_predictors,type="prob")
  prediction_rf$observed=testing_observed
  
  ##transform rain to normal distribution??
  if (response=="Rain" & transformResponse){
    prediction_rf$prediction=exp(prediction_rf$prediction)
    prediction_rf$observed=exp(prediction_rf$observed)
  }
  
  prediction_rf$chDate=testing$chDate
  prediction_rf$x=testing$x
  prediction_rf$y=testing$y
  save(prediction_rf,file=paste(resultpath,"/prediction_rf.RData",sep=""))
  rm(prediction_rf)
  gc()
}
if (any(model=="nnet")){
  load(paste(resultpath,"/fit_nnet.RData",sep=""))
  prediction_nnet=data.frame("prediction"=predict (fit_nnet,testing_predictors))
  if (type=="classification") prediction_nnet$predicted_prob <- predict (fit_nnet,testing_predictors,type="prob")
  prediction_nnet$observed=testing_observed
  
  ##transform rain to normal distribution??
  if (response=="Rain" & transformResponse){
    prediction_nnet$prediction=exp(prediction_nnet$prediction)
    prediction_nnet$observed=exp(prediction_nnet$observed)
  }
  
  prediction_nnet$chDate=testing$chDate
  prediction_nnet$x=testing$x
  prediction_nnet$y=testing$y
  save(prediction_nnet,file=paste(resultpath,"/prediction_nnet.RData",sep=""))
  rm(prediction_nnet)
  gc()
}
if (any(model=="svm")){
  load(paste(resultpath,"/fit_svm.RData",sep=""))
  prediction_svm=data.frame("prediction"=predict (fit_svm,testing_predictors))
  if (type=="classification") prediction_svm$predicted_prob <- predict (fit_svm,testing_predictors,type="prob")
  prediction_svm$observed=testing_observed
  
  ##transform rain to normal distribution??
  if (response=="Rain" & transformResponse){
    prediction_svm$prediction=exp(prediction_svm$prediction)
    prediction_svm$observed=exp(prediction_svm$observed)
  }
  
  prediction_svm$chDate=testing$chDate
  prediction_svm$x=testing$x
  prediction_svm$y=testing$y
  save(prediction_svm,file=paste(resultpath,"/prediction_svm.RData",sep=""))
  rm(prediction_svm)
  gc()
}


if (any(model=="avNNet")){
  load(paste(resultpath,"/fit_avNNet.RData",sep=""))
  prediction_avNNet=data.frame("prediction"=predict (fit_avNNet,testing_predictors))
  if (type=="classification") prediction_avNNet$predicted_prob <- predict (fit_avNNet,testing_predictors,type="prob")
  prediction_avNNet$observed=testing_observed
  
  ##transform rain to normal distribution??
  if (response=="Rain" & transformResponse){
    prediction_avNNet$prediction=exp(prediction_avNNet$prediction)
    prediction_avNNet$observed=exp(prediction_avNNet$observed)
  }
  
  prediction_avNNet$chDate=testing$chDate
  prediction_avNNet$x=testing$x
  prediction_avNNet$y=testing$y
  save(prediction_avNNet,file=paste(resultpath,"/prediction_avNNet.RData",sep=""))
  rm(prediction_avNNet)
  gc()
}


##############################################################################
rm(testing)
gc()
