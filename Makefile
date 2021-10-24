ifeq ($(origin JAVA_HOME), undefined)
  JAVA_HOME=/usr
endif

#ifeq ($(origin NETLOGO), undefined)
NETLOGO="/home/forrest/apps/NetLogo\ 6.1.0"
#endif

#ifeq ($(origin SCALA_JAR), undefined)
#  SCALA_JAR=$(NETLOGO)/lib/scala-library.jar
#endif

SRCS=$(wildcard src/*.java)

raytracing.jar: $(SRCS) manifest.txt
	echo "$(NETLOGO)/app/netlogo-6.1.0.jar"
	mkdir -p classes
	$(JAVA_HOME)/bin/javac -source 1.8 -target 1.8 -classpath "$(NETLOGO)/app/netlogo-6.1.0.jar" -d classes $(SRCS)
	jar cvfm raytracing.jar manifest.txt -C classes .

raytracing.zip: raytracing.jar README.md raytracing.html raytracing.config.txt shapes.txt RayTracing\ Example.nlogo3d 
	rm -rf raytracing
	mkdir raytracing
	cp -rp raytracing.jar README.md Makefile src manifest.txt raytracing.html raytracing.config.txt shapes.txt raytracing
	zip -rv raytracing.zip raytracing RayTracing\ Example.nlogo3d gabby.jpg samplesky.jpg
	#rm -rf raytracing

clean:
	rm -rf classes
	rm -rf raytracing
	rm raytracing.jar
	rm raytracing.zip

