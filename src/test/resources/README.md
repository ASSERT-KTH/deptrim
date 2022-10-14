### Projects used as test resources:

- [kstyrc/embedded-redis](https://github.com/kstyrc/embedded-redis):
  - SHA `c78c74578273fef309d96e76f2b1868b5edc19f2`
  - We specialize guava
- [apache/tika](https://github.com/apache/tika):
  - SHA `1f4169b128fe5ebdc3ce191b78343ad7d3579da1`
  - We test with the `tika-core` module
  - Note: `tika-core` relies on `tika-parent`, which defines extra dependencies for the modules we have discarded
  - We specialize commons-io
