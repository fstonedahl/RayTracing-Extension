ifeq ($(origin JAVA_HOME), undefined)
  JAVA_HOME=/usr
endif

ifeq ($(origin NETLOGO), undefined)
  NETLOGO=../..
endif

ifeq ($(origin SCALA_JAR), undefined)
  SCALA_JAR=$(NETLOGO)/lib/scala-library.jar
endif

SRCS=$(wildcard src/*.java)

raytracing.jar: $(SRCS) manifest.txt
	mkdir -p classes
	$(JAVA_HOME)/bin/javac -g -encoding us-ascii -source 1.5 -target 1.5 -classpath $(NETLOGO)/NetLogo.jar:$(SCALA_JAR) -d classes $(SRCS)
	jar cmf manifest.txt raytracing.jar -C classes .

raytracing.zip: raytracing.jar README.md raytracing.html raytracing.config.txt shapes.txt RayTracing\ Example.nlogo3d 
	rm -rf raytracing
	mkdir raytracing
	cp -rp raytracing.jar README.md Makefile src manifest.txt raytracing.html raytracing.config.txt shapes.txt RayTracing\ Example.nlogo3d raytracing
	zip -rv raytracing.zip raytracing
	#rm -rf raytracing

clean:
	rm -rf classes
	rm -rf raytracing
	rm raytracing.jar
	rm raytracing.zip

