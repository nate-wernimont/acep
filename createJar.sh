classesCased=( "MolDyn" "Crypt" "LUFact" "SOR" "SparseMatmult" "RayTracer" "Series" "MonteCarlo" "Euler" )
rm -r bin/jgfutil
mkdir bin/jgfutil
mv sootOutput/jgfutil/*.class bin/jgfutil/
for i in "${classesCased[@]}"
do
  rm -r bin/"${i,,}"
  mkdir bin/"${i,,}"
  mv sootOutput/"${i,,}"/*.class bin/"${i,,}"/
  rm bin/JGF"${i}"BenchSizeA.class
  mv sootOutput/JGF"${i}"BenchSizeA.class bin/
done
cd bin/
for i in "${classesCased[@]}"
do
  sed -i "s/^Main-Class:.*/Main-Class: JGF${i}BenchSizeA/" ../Manifest.txt
  jar cfm "${i,,}".jar ../Manifest.txt jgfutil/*.class "${i,,}"/*.class edu/iastate/cs/design/asymptotic/machinelearning/calculation/PrintInfo.class JGF"${i}"BenchSizeA.class
done
