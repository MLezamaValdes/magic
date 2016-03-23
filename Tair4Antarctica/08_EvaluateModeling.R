rm(list=ls())
library(caret)
library(Rsenal)
library(hexbin)
library(grid)



load("/media/hanna/data/Antarctica/results/ML/final/testData.RData")
modeldats <- testData[,substr(names(testData),nchar(names(testData))-2,
                              nchar(names(testData)))=="Mod"]


for (i in 1:ncol(modeldats)){
  modeldat <-modeldats[,i]
  modelname<- substr(names(modeldats)[i],1,3)
  regstat <- regressionStats(modeldat,testData$statdat)
  
  ################################################################################
  ################################################################################
  pdf(paste0("/media/hanna/data/Antarctica/visualizations/evaluation_",
             modelname,"_hexbin.pdf"))
  ptxt<-paste0("R^2= ",sprintf("%.2f", round(regstat$Rsq,2)),
               "\nME = ",sprintf("%.2f", round(regstat$ME,2)),
               "\nMAE = ",sprintf("%.2f", round(regstat$MAE,2)),
               "\nRMSE = ",sprintf("%.2f", round(regstat$RMSE,2)))
  hbp <- hexbinplot(testData$statdat~modeldat,
                    xlim=c(min(testData$statdat,modeldat,na.rm=T),
                           max(testData$statdat,modeldat,na.rm=T)),
                    ylim=c(min(testData$statdat,modeldat,na.rm=T),
                           max(testData$statdat,modeldat,na.rm=T)),
                    ylab="Measured Air temperature (°C)", 
                    xlab="predicted Air temperature(°C)",
                    colramp=colorRampPalette(rev(terrain.colors(10))),
                    panel = function(...) {
                      panel.hexbinplot(...)
                      panel.abline(a=0,b=1,lwd=2)
                      panel.abline(lm(testData$statdat~modeldat),lwd=2,lty=2)
                      #grid.text(ptxt, unit(0.10, 'npc'), unit(0.88, 'npc'))
                      grid.text(ptxt, 0.1, 0.82)
                      
                    })
  print(hbp)
  dev.off()
}
#dazu noch die leave one station out cross validation ergebnisse

################################################################################
################################################################################

pdf(paste0("/media/hanna/data/Antarctica/visualizations/RF_timeseries.pdf"),
    width=8,height=7)

for (i in unique(testData$station)){
  dat_sort <- testData[testData$station==i,]
  dat_sort <- dat_sort[order(dat_sort$doy),]
  dat_sort <- dat_sort[complete.cases(dat_sort),]
  if (nrow(dat_sort)<10){next}
  obs <- smooth.spline(dat_sort$doy,dat_sort$statdat, spar=0.25)
  pred <- smooth.spline(dat_sort$doy,dat_sort$LST, spar=0.25)
  pred_ML <- smooth.spline(dat_sort$doy,dat_sort$rfMod, spar=0.25)
  pred_lin <- smooth.spline(dat_sort$doy,dat_sort$lin, spar=0.25)
  lim <- c(min(dat_sort$statdat,dat_sort$LST,na.rm=T),max(dat_sort$statdat,dat_sort$LST,na.rm=T))
  plot(obs,type="l",xlab="doy",ylab="Air Temperature (°C)",
       main=i,ylim=lim)
  lines(pred,col="black",lty=2)
  lines(pred_lin,col="red",lty=2)
  lines(pred_ML,col="red",lty=1)
  legend("bottomleft",legend=c("Stations","MODIS LST","Random Forests","Linear Model"),
         col=c("black","black","red","red"),lty=c(1,2,1,2),lwd=1,bty="n")
}

dat_sort <- testData[order(testData$doy),]
dat_sort <- dat_sort[complete.cases(dat_sort),]
dat_sort <- aggregate(x = data.frame(dat_sort$statdat,dat_sort$LST,dat_sort$rfMod,
                                     dat_sort$linMod),
                      by = list(dat_sort$doy), FUN = "mean")
obs <- smooth.spline(1:365,dat_sort$dat_sort.statdat, spar=0.25)
pred <- smooth.spline(1:365,dat_sort$dat_sort.LST, spar=0.25)
pred_ML <- smooth.spline(1:365,dat_sort$dat_sort.rfMod, spar=0.25)
pred_lin <- smooth.spline(1:365,dat_sort$dat_sort.linMod, spar=0.25)
lim <- c(min(dat_sort$dat_sort.statdat,dat_sort$dat_sort.LST,dat_sort$dat_sort.RfMod,na.rm=T),
         max(dat_sort$dat_sort.statdat,dat_sort$dat_sort.LST,dat_sort$dat_sort.RfMod,na.rm=T))
plot(obs,type="l",xlab="doy",ylab="Air Temperature (°C)",
     main="all stations",ylim=lim)
lines(pred,col="black",lty=2)
lines(pred_lin,col="red",lty=2)
lines(pred_ML,col="red",lty=1)
legend("bottomleft",legend=c("Stations","MODIS LST","Random Forests","Linear Model"),
       col=c("black","black","red","red"),lty=c(1,2,1,2),lwd=1,bty="n")
dev.off()
#######################################
load("/media/hanna/data/Antarctica/results/ML/final/model_RF.RData")
pdf("/media/hanna/data/Antarctica/visualizations/rf_varimp.pdf",width=5,height=5)
plot(varImp(model_RF),col="black")
dev.off()


####################################
# Correlation LST ~ AirT
load("/media/hanna/data/Antarctica/results/ML/final/trainData.RData")
pdf(paste0("/media/hanna/data/Antarctica/visualizations/CorLSTAirT_hexbin.pdf"))

hbp <- hexbinplot(trainData$statdat~trainData$LST,
                  xlim=c(min(trainData$statdat,trainData$LST,na.rm=T),
                         max(trainData$statdat,trainData$LST,na.rm=T)),
                  ylim=c(min(trainData$statdat,trainData$LST,na.rm=T),
                         max(trainData$statdat,trainData$LST,na.rm=T)),
                  ylab="Measured Air temperature (°C)", 
                  xlab="MODIS LST (°C)",
                  colramp=colorRampPalette(rev(terrain.colors(10))),
                  panel = function(...) {
                    panel.hexbinplot(...)
                    panel.abline(a=0,b=1,lwd=2)
                    panel.abline(lm(trainData$statdat~trainData$LST),lwd=2,lty=2)
                    
                  })
print(hbp)
dev.off()