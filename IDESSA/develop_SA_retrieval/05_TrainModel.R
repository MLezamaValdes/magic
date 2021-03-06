library(Rainfall)
setwd("/media/memory01/data/IDESSA/Results/Model/")
outpath <- "/media/memory01/data/IDESSA/Results/Model/"

for (daytime in c("day","night")){
  
  trainData <- get(load(paste0(outpath,"dataset_",daytime,".RData")))
  if(daytime=="day"){
    
    predictornames <- c("VIS0.6","VIS0.8","NIR1.6","IR3.9","WV6.2","WV7.3",
                        "IR8.7","IR9.7","IR10.8","IR12.0","IR13.4","sunzenith",
                        "T0.6_1.6","T6.2_10.8","T7.3_12.0","T8.7_10.8",
                        "T10.8_12.0","T3.9_7.3","T3.9_10.8")
  }else{
    predictornames <- c("IR3.9","WV6.2","WV7.3",
                        "IR8.7","IR9.7","IR10.8","IR12.0","IR13.4",
                        "T6.2_10.8","T7.3_12.0","T8.7_10.8",
                        "T10.8_12.0","T3.9_7.3","T3.9_10.8")
    
  }
  responseRA <- trainData$RainArea
  predictorsRA <- trainData[,predictornames]
  
  responseRR <-trainData$P_RT_NRT[trainData$P_RT_NRT>0]
  predictorsRR <- trainData[trainData$P_RT_NRT>0,predictornames]
  
  model_RA <- train4rainfall(predictorsRA,responseRA, out = "RInfo",
                             scaleVars = TRUE,sampsize = 1,
                             thresholdTune = c(seq(0, 0.1, 0.01), 
                                               seq(0.12, 0.2, 0.02), 
                                               seq(0.2,0.3,0.05), 
                                               seq(0.4, 1,0.1)))
  
  save(model_RA,file=paste0(outpath,daytime,"_model_RA.RData"))
  
  model_RR <- train4rainfall(predictorsRR,responseRR,scaleVars = TRUE,
                             sampsize = 1)
  save(model_RR,file=paste0(outpath,daytime,"_model_RR.RData"))
}