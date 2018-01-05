## Ubunut victoria
#java -Xms512m -Xmx4096m -server -jar mod2csv.jar /home/griessbaum/modtools/ModisData/hdf/* -o /home/griessbaum/modtools/ModisData/csv/

## Ubuntu 
#java -Xms512m -Xmx4096m -server -jar mod2csv.jar /home/scidb/loaddata/hdf/* -o /home/scidb/loaddata/csv/

## Tong
java -Xms512m -Xmx4096m -server -jar -Djava.library.path="/usr/lib/java/" mod2csv.jar /raid/scratch/griessbaum/loaddata/hdf/* -o /raid/scratch/griessbaum/loaddata/csv/

## CentOS
#java  -Xms512m -Xmx4096m -server -jar -Djava.library.path="/usr/lib/java/" mod2csv.jar /home/scidb/loaddata/hdf/* -o /home/scidb/loaddata/csv/   


