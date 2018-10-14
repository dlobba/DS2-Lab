csv_converter <- function(source_f, target_f) {
  pattern <- "^[[:digit:]]+ [pP][-]?[[:alnum:]] [pP][-]?[[:alnum:]][*]*"
  fh_read <- file(source_f, "r")
  fh_write<- file(target_f, "w")
  while (TRUE) {
    line = readLines(fh_read, n = 1)
    if (length(line) == 0) {
      break
    }
    fields <- unlist(strsplit(line, "\\s+"))
    if (length(fields) < 3)
      next
    if (!grepl(pattern, line))
      next
    header <- paste(paste(fields[1:3], collapse = ";"), ";", sep = "")
    if (is.na(fields[4]))
      line <- header
    else
      line <- paste(header, paste(fields[4: length(fields)], collapse = " "))
    writeLines(line, fh_write)
  }
  close(fh_read)
  close(fh_write)
}

seq_message_converter <- function(logs, msc_file) {
  fh_write <- file(msc_file, "w")
  writeLines("msc {", fh_write)
  actors <- unique(sort(c (logs$V2, logs$V3)))
  actors <- sapply(actors, function(x) {
			paste(c("\"", x, "\""), collapse = "")})
  writeLines(paste(actors, collapse = ","),
             fh_write, sep = ";\n")
  for (i in 2:nrow(logs)) {
    log <- logs[i,]
    writeLines(sprintf("\"%s\"<=\"%s\" [label=\"%s\"];",
                       log[2], log[3], log[4]),
               fh_write)
  }
  writeLines("}", fh_write)
  close(fh_write)
}

args <- commandArgs(trailingOnly = T)
if (length(args) > 1) {
  temp_csv_file <- "temp_.csv"
  log_file <- args[1]
  msc_file <- args[2]
  csv_converter(log_file, temp_csv_file)
  data <- read.csv(temp_csv_file, sep=";",
                  header = FALSE,
                  stringsAsFactors = F)
  index = with(data, order(V1))
  data <- data[index,]
  seq_message_converter(data, msc_file)
} else
  print("Insufficient number of arguments")
