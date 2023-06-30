# DepTrim <img src=".img/logo.svg" align="left" height="135px" alt="DepTrim logo"/>

[![Maven Central](https://img.shields.io/maven-central/v/se.kth.castor/deptrim-maven-plugin.svg)](https://search.maven.org/search?q=g:se.kth.castor%20AND%20a:deptrim*)
[![build](https://github.com/castor-software/deptrim/actions/workflows/build.yml/badge.svg)](https://github.com/castor-software/deptrim/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=alert_status)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![codecov](https://codecov.io/gh/castor-software/deptrim/branch/main/graph/badge.svg?token=L70YMFGJ4D)](https://codecov.io/gh/ASSERT-KTH/deptrim)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=security_rating)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=bugs)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=code_smells)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=ncloc)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=sqale_index)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)

## What is DepTrim?

DepTrim is a Maven plugin that automatically specializes the dependencies of a project.
The objective is hardening the [software supply chain](https://www.cesarsotovalero.net/blog/the-software-supply-chain.html) of third-party dependencies of a project by using dependencies that only contain the classes and interfaces that are **actually necessary** to build the project.
Relying on specialized variants of dependencies is **good for security**, as it reduces the attack surface of the project, and **good for performance**, as it reduces the size of the final artifact.

After running DepTrim, a directory named `libs-specialized` is created in the root of the project.
This directory contains the specialized variants of all the dependencies necessary to build the project (inc. direct and transitive dependencies).
DepTrim can also create a specialized POM file, named `pom-specialized.xml`.
This specialized POM uses the specialized variants of the dependencies instead of the original dependencies.
DepTrim deploys the specialized variants of the dependencies in the local Maven repository.

**NOTE:** DepTrim does not modify the original source code of the project nor its original `pom.xml`.

## Usage

Run DepTrim directly from the command line as follows:

```bash
cd {PATH_TO_MAVEN_PROJECT}
# First, compile source and test files of the project.
mvn compile   
mvn compiler:testCompile
# Then, run the latest version of DepTrim.
mvn se.kth.castor:deptrim-maven-plugin:0.1.1:deptrim -DcreateSinglePomSpecialized=true
```

Alternatively, configure the original `pom.xml` file of the project to run DepTrim as part of the build as follows:

```xml
<plugin>
  <groupId>se.kth.castor</groupId>
  <artifactId>deptrim-maven-plugin</artifactId>
  <version>0.1.1</version>
  <executions>
    <execution>
      <goals>
        <goal>deptrim</goal>
      </goals>
      <configurations>
        <createSinglePomSpecialized>true</createSinglePomSpecialized>
      </configurations>
    </execution>
  </executions>
</plugin>
```

In both cases, a directory name `libs-specialized` will be created in the root of the project, together with a file named `pom-specialized.xml`, which uses the specialized variants of the dependencies.

## Optional parameters

The `deptrim-maven-plugin` accepts the following additional parameters.

| Name                        |     Type      | Description                                                                                                                                                                                                                                                                                                                                                                                                        | 
|:----------------------------|:-------------:|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| 
| `<specializeDependencies>`  | `Set<String>` | Add a list of dependencies, identified by their coordinates, to be specialized by DepTrim. **Dependency format is:** `groupId:artifactId:version:scope`. An empty string indicates that all the dependencies in the dependency tree of the project will be specialized (`default`).                                                                                                                                |
| `<ignoreDependencies>`      | `Set<String>` | Add a list of dependencies, identified by their coordinates, to be ignored by DepTrim during the analysis. This is useful to override incomplete result caused by bytecode-level static analysis. **Dependency format is:** `groupId:artifactId:version:scope`.                                                                                                                                                    |
| `<ignoreScopes>`            | `Set<String>` | Add a list of scopes, to be ignored by DepTrim during the analysis. Useful to not analyze dependencies with scopes that are not needed at runtime. **Valid scopes are:** `compile`, `provided`, `test`, `runtime`, `system`, `import`. An empty string indicates no scopes (`default`).                                                                                                                            |
| `<createSinglePomSpecialized>`    |   `boolean`   | If this is `true`, DepTrim creates a specialized version of the POM file in the root of the project, called `pom-specialized.xml`, which points to the variant of the specialized the dependencies. **Default value is:** `false`.                                                                                                                                                                                 |
| `<createDependencySpecializedPerPom>`    |   `boolean`   | If this is `true`, DepTrim creates one specialized version of the POM file per specialized dependency, called `pom-specialized-x-y.xml`, where `x` is an integer identifying a specialized dependency, and `y` is the total number of specialized dependencies. **Default value is:** `false`.                                                                                                                    |
| `<createAllPomSpecialized>` |   `boolean`   | If this is `true`, DepTrim creates all the combinations of specialized version of the original POM in the root of the project (i.e., $2^y$ POM files will be created). Name format is `pom-specialized-n-x-y.xml`, where `n` is the combination number, `x` is the number of specialized dependencies in this combination, and `y` is the total number of specialized dependencies. **Default value is:** `false`. |
| `<verboseMode>`             |   `boolean`   | Run DepTrim in verbose mode. **Default value is:** `false`.                                                                                                                                                                                                                                                                                                                                                        |
| `<skipDepTrim>`             |   `boolean`   | Skip plugin execution completely. **Default value is:** `false`.                                                                                                                                                                                                                                                                                                                                                   |

## How does DepTrim works?

DepTrim runs before executing during the [`pre-package`](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html#some-phases-are-not-usually-called-from-the-command-line) phase of the Maven build lifecycle. 
DepTrim relies on [depclean-core](https://github.com/castor-software/depclean) to statically collects all the types used by the project under analysis, as well as in its dependencies. 
With this information, DepTrim removes all the types in the dependencies that are not used by the project.
DepTrim also creates a directory named `libs-specialized` in the root of the project, which contains the specialized versions of the dependencies.
DepTrim creates a new `pom-specialized.xml` file that contains only the specialized versions of the dependencies.

The `pom-specialized.xml` is created following these steps:

1. Identify all used dependencies and add them as direct dependencies.
2. For the used dependencies, remove the types (i.e., compiled classes and interfaces) that are not used by the project.
3. Deploy the modified dependencies in the local Maven repository.
4. Create a `pom-specialized.xml` so that it uses the specialized variants of the dependencies located in the local Maven repository.

### Known limitations

DepTrim needs to know all the types used by the project under analysis, as well as in its dependencies.
This is a challenging task, as it requires "seeing" all the project's codebase.
In particular, it is not possible to detect the usage of [dynamic Java features](https://www.graalvm.org/latest/reference-manual/native-image/dynamic-features/), such as reflection, dynamic proxies, or custom class loaders, in Java.
This necessitates both a thorough understanding of Java's dynamic features and a careful examination of the project's codebase.
To detect the utilization of dynamic features within a Java application, we recommend the use of the [GraalVM Tracing Agent](https://www.graalvm.org/22.0/reference-manual/native-image/Agent/).

```bash
java -agentlib:native-image-agent=config-output-dir=/path/to/config-dir/ -jar yourApp.jar
```
By running your application with the agent, it will generate a configuration directory (`/path/to/config-dir/`) containing the files that describe the observed dynamic behavior.
This useful for specialization tasks, e.g., when specializing dependencies that could be accessed dynamically and lack complete a priori knowledge about all possible dynamic behaviors.

While DepTrim aims to streamline the dependency-trimming process, understanding its limitations and employing additional tools like the GraalVM Tracing Agent can help enhance the process.
However, note that certain dynamic behaviors, such as the implications of multi-threading or just-in-time (JIT) compilation, may be too subtle or intricate to be detected readily.

## Installing and building from source

Prerequisites:

- [Java OpenJDK 17](https://openjdk.java.net) or above
- [Apache Maven](https://maven.apache.org/)

In a terminal, clone the repository and switch to the cloned folder:

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
