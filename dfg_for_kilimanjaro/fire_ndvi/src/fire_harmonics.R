lib <- c("raster", "zoo", "ggplot2", "doParallel", "RColorBrewer", "rgeos", 
         "Rsenal")
sapply(lib, function(x) library(x, character.only = TRUE))

source("src/multiVectorHarmonics.R")

fls_agg1m <- list.files("data/md14a1/low/aggregated", pattern = "^aggsum_md14a1", 
                        full.names = TRUE)
rst_agg1m <- stack(fls_agg1m)

# Overall observed active fire pixels per month between 2001 and 2013
val_sum <- sapply(1:nlayers(rst_agg1m), function(i) {
  sum(rst_agg1m[[i]][], na.rm = TRUE)
})

months <- sapply(strsplit(basename(fls_agg1m), "_"), "[[", 3)
months <- substr(months, 5, 6)

val_sum_agg <- aggregate(val_sum, by = list(months), FUN = sum)
names(val_sum_agg) <- c("month", "value")
val_sum_agg$month <- factor(month.abb, levels = month.abb)

ggplot(aes(x = month, y = value), data = val_sum_agg) + 
  geom_histogram(stat = "identity") + 
  labs(x = "\nMonth", y = "No. of active fires\n") + 
  theme_bw()

# Observed active fire pixels per month 2001-05 vs. 2009-13
yrmn <- sapply(strsplit(basename(fls_agg1m), "_"), "[[", 3)
yrmn <- substr(yrmn, 1, 6)

harm_0105_0913 <- multiVectorHarmonics(rst_agg1m, time_info = yrmn, 
                                       intervals = c(2001, 2009), width = 5)
ggplot(aes(x = month, y = value, group = interval, colour = interval, 
           fill = interval), data = harm_0105_0913) + 
  #   geom_line() + 
  geom_histogram(stat = "identity", position = "dodge") + 
  labs(x = "\nMonth", y = "No. of active fires\n") +
  scale_colour_manual("", values = c("black", "grey65")) + 
  scale_fill_manual("", values = c("black", "grey65")) + 
  theme_bw()

# Same issue, but for 2001-04 vs. 2004-07 vs. 2007-10 vs. 2010-13
harm_0104_0407_0710_1013 <- multiVectorHarmonics(rst_agg1m, time_info = yrmn, 
                                                 intervals = seq(2001, 2009, 4), 
                                                 width = 5)

cols_qul <- brewer.pal(3, "Dark2")

p_agg1m_harm <- ggplot(aes(x = month, y = value, group = interval, 
                           colour = interval, fill = interval), 
                       data = harm_0104_0407_0710_1013) + 
  geom_histogram(stat = "identity", lwd = 2, position = "dodge") + 
  labs(x = "\nMonth", y = "No. of active fires\n") +
  scale_colour_manual("", values = cols_qul) + 
  scale_fill_manual("", values = cols_qul) + 
  theme_bw() + 
  theme(text = element_text(size = 15), 
        axis.title = element_text(size = 18))

png("out/fire/harmonics_0105_0509_0913.png", width = 25, height = 20, 
    units = "cm", pointsize = 15, res = 600)
print(p_agg1m_harm)
dev.off()

# # Separate fires inside the NP from fires outside the NP
# np_old <- readOGR(dsn = "data/protected_areas/", 
#                   layer = "fdetsch-kilimanjaro-national-park-1420535670531", 
#                   p4s = "+init=epsg:4326")
# np_old_utm <- spTransform(np_old, CRS("+init=epsg:21037"))
# 
# np_new <- readOGR(dsn = "data/protected_areas/", 
#                   layer = "fdetsch-kilimanjaro-1420532792846", 
#                   p4s = "+init=epsg:4326")
# np_new_utm <- spTransform(np_new, CRS("+init=epsg:21037"))
# np_new_utm_lines <- as(np_new_utm, "SpatialLines")
# 
# rst_agg1m_inside <- mask(rst_agg1m, np_new_utm)
# rst_agg1m_outside <- mask(rst_agg1m, np_new_utm, inverse = TRUE)
# 
# harm_0104_0613_inside <- multiVectorHarmonics(rst_agg1m_inside, time_info = yrmn, 
#                                               intervals = c(2001, 2006, 2010), width = 4)
# harm_0104_0613_inside$location <- "inside"
# harm_0104_0613_outside <- multiVectorHarmonics(rst_agg1m_outside, time_info = yrmn, 
#                                               intervals = c(2001, 2006, 2010), width = 4)
# harm_0104_0613_outside$location <- "outside"
# 
# harm_0104_0613_inside_outside <- 
#   rbind(harm_0104_0613_inside, harm_0104_0613_outside)
# 
# ggplot(aes(x = month, y = value, group = interval, colour = interval, 
#            fill = interval), data = harm_0104_0613_inside_outside) + 
#   geom_histogram(stat = "identity", position = "dodge") + 
#   facet_wrap(~location, ncol = 1) + 
#   labs(x = "\nMonth", y = "No. of active fires\n") +
#   scale_colour_manual("", values = c("grey75", "grey45", "black")) +   
#   scale_fill_manual("", values = c("grey75", "grey45", "black")) + 
#   theme_bw()

# Same issue, but with rejection of cells intersected by NP border
id_intersect <- foreach(i =1:ncell(rst_agg1m), .packages = lib, 
                        .combine = "c") %dopar% {
  rst <- rst_agg1m[[1]]
  rst[][-i] <- NA
  shp <- rasterToPolygons(rst)
  return(gIntersects(np_new_utm_lines, shp))
}

rst_agg1m_rmb <- rst_agg1m
rst_agg1m_rmb[id_intersect] <- NA

rst_agg1m_rmb_inside <- mask(rst_agg1m_rmb, np_new_utm)
rst_agg1m_rmb_outside <- mask(rst_agg1m_rmb, np_new_utm, inverse = TRUE)

harm_before_after_rmb_inside <- multiVectorHarmonics(rst_agg1m_rmb_inside, time_info = yrmn, 
                                              intervals = c(2001, 2006, 2010), width = 4)
harm_before_after_rmb_inside$location <- "inside"
harm_before_after_rmb_outside <- multiVectorHarmonics(rst_agg1m_rmb_outside, time_info = yrmn, 
                                               intervals = c(2001, 2006, 2010), width = 4)
harm_before_after_rmb_outside$location <- "outside"

harm_before_after_rmb_inside_outside <- 
  rbind(harm_before_after_rmb_inside, harm_before_after_rmb_outside)

ggplot(aes(x = month, y = value, group = interval, colour = interval, 
           fill = interval), data = harm_before_after_rmb_inside_outside) + 
  geom_histogram(stat = "identity", position = "dodge") + 
  facet_wrap(~location, ncol = 1) + 
  labs(x = "\nMonth", y = "No. of active fires\n") +
  scale_colour_manual("", values = c("grey75", "grey45", "black")) +   
  scale_fill_manual("", values = c("grey75", "grey45", "black")) + 
  theme_bw()

# seasonality, split into 4 tiles
fls_agg1m <- list.files("data/md14a1/low/aggregated", pattern = "^aggsum_md14a1", 
                        full.names = TRUE)
rst_agg1m <- stack(fls_agg1m)
rst_agg1m <- writeRaster(rst_agg1m, "data/md14a1/low/aggregated/split/aggsum_md14a1.tif", 
                         format = "GTiff", overwrite = TRUE, bylayer = FALSE)
fls_agg1m <- "data/md14a1/low/aggregated/split/aggsum_md14a1.tif"
rst_agg1m_split <- splitRaster(fls_agg1m)

registerDoParallel(cl <- makeCluster(4))
harm_agg1m_split <- foreach(i = rst_agg1m_split, j = c("ul", "ll", "ur", "lr"), 
                            .packages = lib, .combine = "rbind") %dopar% {
  harm_0104_0407_0710_1013 <- multiVectorHarmonics(i, time_info = yrmn, 
                                                   intervals = c(2001, 2005, 2009), 
                                                   width = 5)
  
  return(data.frame(tile = j, harm_0104_0407_0710_1013))
}

harm_agg1m_split$tile <- factor(harm_agg1m_split$tile, 
                                levels = c("ul", "ur", "ll", "lr"))

cols_gry <- c("grey25", "grey50", "grey75")
p_agg1m_harm_split <- ggplot(aes(x = month, y = value, group = interval, colour = interval, 
           fill = interval), data = harm_agg1m_split) + 
  geom_histogram(stat = "identity", position = "dodge") + 
  facet_wrap(~ tile, ncol = 2) + 
  labs(x = "\nMonth", y = "No. of active fires\n") +
  scale_colour_manual("", values = cols_qul) + 
  scale_fill_manual("", values = cols_qul) + 
  theme_bw() + 
  theme(text = element_text(size = 15), 
        axis.title = element_text(size = 18))

png("out/fire/harmonics_0105_0509_0913_split.png", width = 25, height = 20, 
    units = "cm", pointsize = 15, res = 600)
print(p_agg1m_harm_split)
dev.off()

stopCluster(cl)
