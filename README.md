# DepTrim 

[![build](https://github.com/castor-software/deptrim/actions/workflows/build.yml/badge.svg)](https://github.com/castor-software/deptrim/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=castor-software_deptrim&metric=alert_status)](https://sonarcloud.io/dashboard?id=castor-software_deptrim)
[![codecov](https://codecov.io/gh/castor-software/deptrim/branch/main/graph/badge.svg?token=L70YMFGJ4D)](https://codecov.io/gh/castor-software/deptrim)

DepTrim automatically diversifies dependencies in Maven projects through software debloating.

# How to use it?
 
## Input

DepTrim takes as list of compiled Java class files and an entry point as input. 
The entry point is the fully qualified name of the class that contains the main method. 

## Output

DepTrim outputs a list of reachable class files that are accessed statically from the entry point.

# How it works?

## Main plan

We can use DepClean:

1. Add to the `src/test/resources` folder:
    - Two Maven projects that builds successfully and have one or more common dependency ().
2. Run DepClean on the two projects:
    - Get the list of class files from the common dependency that are necessary for each of the two projects.
      - https://github.com/pagehelper/Mybatis-PageHelper ("5.1.10": "9dade15d30f7a62bc6a8ce62fc288a5c6158ee79",)
      - https://github.com/ronmamo/reflections ("0.9.12": "7741e8f5d13f739d644ef5e91dde055fbe88ca57")
      - https://github.com/dinix2008/quasar-groovy ("2.4.1": "6afdb591137d5570d4aa26a08f464ef009e1d005")
      - https://github.com/magro/kryo-serializers ("0.45": "5073412311cd08efa0eadb3058c20349f3f889ab")
      - https://github.com/JodaOrg/joda-convert ("2.2.1": "0a6fd3435d504d37796b770a0743d13487810b36")
      - https://github.com/kstyrc/embedded-redis ("0.6": "c78c74578273fef309d96e76f2b1868b5edc19f2")
3. Remove the unused class files from the common dependency.
    - (Future work) We reuse the JDBL tool to get more granular debloat and remove methods.
4. Rebuild the two projects with the variant of the dependency that we have produced.

## Alternative plan

We can to reuse the GraalVM points-to static analysis to determine which class files are actually reachable.  

Related PRs:
- https://github.com/oracle/graal/pull/4375









