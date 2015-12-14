# srclib-basic [![Build Status](https://travis-ci.org/alexsaveliev/srclib-basic.png?branch=master)](https://travis-ci.org/alexsaveliev/srclib-basic)

## Requirements

srclib-basic requires:

* Oracle JDK 8 or OpenJDK 8
* Gradle 2.8

## Language support

srclib-basic supports the following languages

* PHP
* Objective-C


## Building

srclib-basic can be build and registered with the following two commands:

    make
    src toolchain add sourcegraph.com/sourcegraph/srclib-basic

## Testing

Run `git submodule update --init` the first time to fetch the submodule test
cases in `testdata/case`.

`make test` - Test in program mode

`make test-gen` - Generate new test data in program mode
