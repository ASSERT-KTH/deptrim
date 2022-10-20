# DepTrim <img src="https://github.com/castor-software/depclean/blob/master/.img/logo.svg" align="left" height="135px" alt="DepClean logo"/>

[![build](https://github.com/castor-software/deptrim/actions/workflows/build.yml/badge.svg)](https://github.com/castor-software/deptrim/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=alert_status)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![codecov](https://codecov.io/gh/castor-software/deptrim/branch/main/graph/badge.svg?token=L70YMFGJ4D)](https://codecov.io/gh/castor-software/deptrim)

# What is DepTrim?

DepTrim automatically diversifies dependencies in Maven projects for hardening its [software supply chain](https://www.cesarsotovalero.net/blog/the-software-supply-chain.html).
To do so, it creates different variants of the dependencies in the dependency tree of a project.
DepTrim works as a Maven plugin.
It can be executed as a Maven goal through the command line or integrated directly into the Maven build lifecycle (CI/CD).
DepTrim does not modify the original source code of the project nor its original `pom.xml`.

# Usage

Configure the pom.xml file of your Maven project to use DepTrim as part of the build:

```xml
<plugin>
  <groupId>se.kth.castor</groupId>
  <artifactId>deptrim-maven-plugin</artifactId>
  <version>{DEPTRIM_LATEST_VERSION}</version>
  <executions>
    <execution>
      <goals>
        <goal>deptrim</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```
Or you can run DepTrim directly from the command line.

```bash
cd {PATH_TO_MAVEN_PROJECT}
mvn compile   
mvn compiler:testCompile
mvn se.kth.castor:deptrim-maven-plugin:{DEPTRIM_LATEST_VERSION}:deptrim
```

# Optional Parameters

The `deptrim-maven-plugin` can be configured with the following additional parameters.

| Name                   |     Type      | Description                                                                                                                                                                                                                                                                                                   | 
|:-----------------------|:-------------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| 
| `<trimDependencies>`   | `Set<String>` | Add a list of dependencies, identified by their coordinates, to be trimmed by DepTrim. **Dependency format is:** `groupId:artifactId:version:scope`. An Empty string indicates that all the dependencies in the dependency tree of the project will be trimmed (default).                                     |
| `<ignoreDependencies>` | `Set<String>` | Add a list of dependencies, identified by their coordinates, to be ignored by DepTrim during the analysis. Useful to override incomplete result caused by bytecode-level analysis. **Dependency format is:** `groupId:artifactId:version:scope`.                                                              |
| `<ignoreScopes>`       | `Set<String>` | Add a list of scopes, to be ignored by DepTrim during the analysis. Useful to not analyze dependencies with scopes that are not needed at runtime. **Valid scopes are:** `compile`, `provided`, `test`, `runtime`, `system`, `import`. An Empty string indicates no scopes (default).                         |
| `<createPomTrimmed>` |   `boolean`   | If this is true, DepTrim creates a trimmed version of the `pom.xml` in the root of the project, called `trimmed-pom.xml`, that uses the variant of the trimmed the dependencies. **Default value is:** `false`.                                                                                               |
| `<ignoreTests>`        |   `boolean`   | If this is true, DepTrim will not analyze the test classes in the project, and, therefore, the dependencies that are only used for testing will be considered unused. This parameter is useful to detect dependencies that have `compile` scope but are only used for testing. **Default value is:** `false`. |
| `<createResultJson>`   |   `boolean`   | If this is true, DepTrim creates a JSON file of the dependency tree along with metadata of each dependency. The file is called `deptrim-results.json`, and is located in the `target` directory of the project. **Default value is:** `false`.                                                                |
| `<createCallGraphCsv>` |   `boolean`   | If this is true, DepTrim creates a CSV file with the static call graph of the API members used in the project. The file is called `deptrim-callgraph.csv`, and is located in the `target` directory of the project. **Default value is:** `false`.                                                            |
| `<skipDepTrim>`        |   `boolean`   | Skip plugin execution completely. **Default value is:** `false`.                                                                                                                                                                                                                                              |

[//]: # (TODO: Explain here how to integrate DepTrim in the CI/CD pipeline so that a different variant of the dependencies is used for each build.)

# How does DepTrim works?

DepTrim runs before executing the `package` phase of the Maven build lifecycle. 
It relies on [DepClean](https://github.com/castor-software/depclean) to statically collects all the types referenced in the project under analysis as well as in its dependencies. 
Then, it removes the parts of the dependencies that are not used by the project.

With this usage information, DepTrim constructs a new `trimmed-pom.xml` based on the following three steps:

1. add all used dependencies as direct dependencies
2. from the used dependencies, trim the parts that are not used by the project
3. modify the pom so that it uses the trimmed variants of the used dependencies

If all the tests pass, and the project builds correctly after these changes, then it means that the dependencies identified as bloated can be removed. DepClean produces a file named `pom-debloated.xml`, located in the root of the project, which is a clean version of the original `pom.xml` without bloated dependencies.

## Installing and building from source

Prerequisites:

- [Java OpenJDK 11](https://openjdk.java.net) or above
- [Apache Maven](https://maven.apache.org/)

In a terminal clone the repository and switch to the cloned folder:

```bash
git clone https://github.com/castor-software/deptrim.git
cd deptrim
```

Then run the following Maven command to build the application and install the plugin locally:

```bash
mvn clean install
```

## License

Distributed under the MIT License. See [LICENSE](https://github.com/castor-software/depclean/blob/master/LICENSE.md) for more information.

## Funding

DepTrim is partially funded by the [Wallenberg Autonomous Systems and Software Program (WASP)](https://wasp-sweden.org).

<img src="https://github.com/castor-software/depclean/blob/master/.img/wasp.svg" height="50px" alt="Wallenberg Autonomous Systems and Software Program (WASP)"/>
