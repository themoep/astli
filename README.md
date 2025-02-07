# Abstract-Syntax-Tree-based Library Identification (ASTLI)

## What It Is

ASTLI finds libraries (`.jar`) in obfuscated Android applications (`.apk`). First, ASTLI learns libraries by extracting fingerprints that are derived from its abstract syntax tree and from its method signatures. Then, ASTLI matches fingerprints against an application and gives an estimate on how likely the library is present in the application. See [1] for a detailed explanation of the process and the evaluation. Cite [2] if you use this work. 

## Requirements

- `dx.jar` from Android SDK Build Tools
- Tested with OpenJDK 8

## Build

1. `git clone https://github.com/kstudent/astli`
2. `cd astli`
3. Copy `dx.jar` or create symlink to `dx.jar` in `astli/lib/`;
   NOTE: you can find `dx.jar` in `<Android-Sdk>/build-tools/<version>/lib/dx.jar` but version 30.0.3 is the latest that still ships dx
   e.g.: `mkdir -p astli/lib && ln -s <Android-Sdk>/build-tools/30.0.3/lib/dx.jar astli/lib/dx.jar` inside the `astli` directory
5. Build with `./gradlew astli:build` (run `./gradlew tasks --all` to see all tasks)
   This throws some errors but creates the `astli.jar`, which other commands fail to create.

## Usage

Run `java -jar ./astli/build/libs/astli.jar` to list all options and parameters.

## Notice

ASTLI is a research prototype, and, although ASTLI has been extensively evaluated with FOSS applications, one may encounter edge cases that ASTLI cannot handle. 

## Sources

[1] Rabensteiner, Christof: Android Library Identification (Master's Thesis); 2017;  http://diglib.tugraz.at/download.php?id=5988e795a35ec&location=search

[2] Feichtner, Johannes, and Rabensteiner, Christof: Obfuscation-Resilient Code Recognition in Android Apps; ARES 2019;  
