ifeq ($(OS),Windows_NT)
	GRADLEW = .\gradlew.bat
else
	GRADLEW = ./gradlew
endif

.PHONY: default install test test-gen clean dist docker-image release

default: install

install: 
	${GRADLEW} install

test: install
	srclib -v test -m program

test-gen: install
	srclib -v test -m program --gen

clean:
	rm -f .bin/*.jar
	rm -rf build


dist: install

docker-image: install
	docker build -t srclib/srclib-basic .

release: docker-image
	docker push srclib/srclib-basic
