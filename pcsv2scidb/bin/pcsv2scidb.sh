#USAGE: pcsv2scidb <scidbFolder> <numInstances> <chunkSize> <linesToSkip> <inputFile> <outputFile>
#   <scidbFolder>    - Path to the SciDB data folder (parent of the instance folders).
#   <numInstances>   - Number of SciDB instances to stripe across.
#   <chunkSize>      - Chunk size of the 1-D load array.
#   <linesToSkip>    - Number of lines to skip in the inputFile.
#   <input file>     - Path to the input CSV file.
#   <output file>    - Base name of the ouput file that should be created in the folder under each instance.

# sudo update-alternatives --config java


#java -Xms512m -Xmx4096m -server -jar pcsv2scidb.jar /home/griessbaum/scidb_test/ 2 5000 1 /home/griessbaum/scidb_test/Band_20_Measurements.csv modistest.dlf
java -Xms512m -Xmx4096m -server -jar pcsv2scidb.jar "$@" 
