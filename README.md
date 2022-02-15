Small java programs created in the scope of my bachelor thesis (**Rainbow cycles in flip graphs**).

The TriangulationLab should be started using command: **java main [inputFile] r [outputFile]**

The **input file** is a text file encoding the point set and all its triangulations. The file **7gon.txt** provides an example for the encoding.

The following code generates a similar **8gon.txt** when executed on **SageMath**:
```
vertices = [[1,0],[0.707,0.707],[0.707,-0.707],[0,-1],[-0.707,-0.707],[0,1],[-1,0],[-0.707,0.707]]
ptconfig = PointConfiguration(vertices, fine=True))
		   with open("D:/8gon.txt", 'a+') as f:
			   f.write("V=" + str(vertices)+"\n")
			   t_list = ptconfig.triangulations_list()
			   for t in t_list: f.write("T:B" + str(list(t.boundary())) + ", D=" + str(list(t.interior_facets()))+"\n")
```

The generated **output file** contains **r-rainbow cycles** encoded as **Geogebra** code. Just execute the content of the output file in Geogebra to see the found r-rainbow cycles.

