% Generated by roxygen2 (4.1.1): do not edit by hand
% Please edit documentation in R/containerwidget.R
\name{containerwidget}
\alias{containerwidget}
\title{containerwidget}
\usage{
containerwidget(html = NULL, css = NULL, javascript = NULL,
  width = NULL, height = NULL)
}
\arguments{
\item{html}{html as text (optional)}

\item{css}{css as text (optional)}

\item{javascript}{javascript as text (optional)}

\item{width}{width of element (optional) if NULL then use full window width}

\item{height}{height of element (optional) if NULL then use full window height}
}
\description{
containerwidget is a simple wrapper of \code{\link[htmlwidgets]{createWidget}}.
}
\examples{
css <- ".containerwidget {background-color:grey;}"
html <- "<b style=\\"background-color:yellow;\\">Text</b>"
javascript <- "alert('running');"
containerwidget(html = html, css = css, javascript = javascript)
}

