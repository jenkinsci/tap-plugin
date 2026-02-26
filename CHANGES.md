# TAP plug-in changelog

## Version 2.4.5 (2025/??/??)

- [GH-44](https://github.com/jenkinsci/tap-plugin/pull/44): Add @Symbol("publishTap") to TapPublisher Descriptor and pipeline test (thanks @0xShubhamSolanki)

## Version 2.4.4 (2025/03/13)

- [JENKINS-75332](https://issues.jenkins.io/browse/JENKINS-75332): Fix showOnlyFailures for merged results (thanks @dweber)
- [JENKINS-74247](https://issues.jenkins.io/browse/JENKINS-74247): Extra inline JS script in TapStreamResult/body.jelly (thanks @shlomomdahan)

## Version 2.4.3 (2024/08/15)

1. Enable [Jenkins Security Scan](https://www.jenkins.io/doc/developer/security/scan/) (pr #39), thanks @strangelookingnerd
2. [JENKINS-72561](https://issues.jenkins.io/browse/JENKINS-72561): Fixed error where the TAP Extended Test results page could produce an error panel (related to sidepanel.jelly)

## Version 2.4.2 (2024/04/06)

1. SECURITY-3190: Fix XSS security bug (already published)

## Version 2.4.1 (2024/01/18)

1. Migrated old infra docs to GitHub `README.md`, will serve plug-in docs from GH Pages
2. [JENKINS-72558](https://issues.jenkins.io/browse/JENKINS-72558): NullPointerException with TAP plugin version 2.4.0 (restored mistakenly removed `readObject`, #36)

## Version 2.4 (2023/12/24)

1. [#31](https://github.com/jenkinsci/tap-plugin/pull/31): Updated the code using IDE linter
2. Added `CHANGES.md`
3. Updated `README.md`
4. [#32](https://github.com/jenkinsci/tap-plugin/pull/32): Update pom.xml (plugins, dependencies, Jenkins version)
5. Bump io.jenkins.tools.bom.bom-2.414.x to bom-2.426.x 2598.v49e2b_e68d413 #33

## Version 2.3 (2019-06-05)

1. [tap4j dependency upgraded to 4.4.2 version](https://github.com/jenkinsci/tap-plugin/pull/24)
2. [enabled parsing of TAP documents with corrupted YAML content](https://github.com/jenkinsci/tap-plugin/pull/25) 1.
   New option in advanced, to remove corrupted YAML elements   (should work after upgrading the plug-in, but remember to
   back up first anyway!)

## Version 2.2.2 (2019-02-14)

1. [JENKINS-55787](https://issues.jenkins.io/browse/JENKINS-55787) - Getting issue details... STATUS

## Version 2.2.1 (2018-02-07)

1. [JENKINS-48925](https://issues.jenkins.io/browse/JENKINS-48925) - Getting issue details... STATUS

## Version 2.2 (2018-01-26)

1. [JENKINS-48925](https://issues.jenkins.io/browse/JENKINS-48925) - Getting issue details... STATUS

## Version 2.1 (2017-02-28)

1. [JENKINS-22250](https://issues.jenkins-ci.org/browse/JENKINS-22250): "fail the build if no test results are present"
   option doesn't fail the job
2. [JENKINS-21456](https://issues.jenkins-ci.org/browse/JENKINS-21456): Plugin runs despite earlier errors
3. Upgraded tap4j dependency from 4.2.0 to 4.2.1 (latest)

## Version 2.0 (2016-08-20)

1. [JENKINS-29649: don't overwrite existing junit-results](https://issues.jenkins-ci.org/browse/JENKINS-29649)
2. [JENKINS-34000: Workflow/Pipeline Support for TAP Plugin](https://issues.jenkins-ci.org/browse/JENKINS-34000)
3. [JENKINS-33779: TAP plugin: enable detailed output for TAP with subtests](https://issues.jenkins-ci.org/browse/JENKINS-33779)

## Version 1.25 (2016-06-21)

1. [JENKINS-23851: Expand env vars in the ant pattern field](https://issues.jenkins-ci.org/browse/JENKINS-23851)
2. [JENKINS-17804: Add option to show only failures](https://issues.jenkins-ci.org/browse/JENKINS-17804)
3. [JENKINS-29650: Don't use a build step synchronization, defaulting to NONE.](https://issues.jenkins-ci.org/browse/JENKINS-29650)
4. [JENKINS-30751: Use same logic everywhere to say whether a test is a failure or not](https://issues.jenkins-ci.org/browse/JENKINS-30751)
5. [JENKINS-24843: Copy duration from Tabulated Tests](https://issues.jenkins-ci.org/browse/JENKINS-24843)
6. [pull request \#6: fix duration\_ms (thanks to @forrest79)](https://github.com/jenkinsci/tap-plugin/pull/6)
7. [JENKINS-22016 via pull request \#10: Expand env vars in the ant pattern field (thanks to @evandy0)](https://issues.jenkins-ci.org/browse/JENKINS-22016)
8. [SECURITY-85: \[tap\] Can read all files on file system](https://issues.jenkins-ci.org/browse/SECURITY-85)

## Version 1.24 (2015-10-01)

1. [JENKINS-19471: TAP parser does not conform to InstantTAP website](https://issues.jenkins-ci.org/browse/JENKINS-19471)
2. [JENKINS-24505: Tests are not marked as skipped with TAP plugin](https://issues.jenkins-ci.org/browse/JENKINS-24505)
3. [JENKINS-19676: TAP test description does not get escaped](https://issues.jenkins-ci.org/browse/JENKINS-19676)
4. [JENKINS-27227: TAP plugin fails to load with java.lang.NoClassDefFoundError: hudson/tasks/test/AbstractTestResultAction](https://issues.jenkins-ci.org/browse/JENKINS-27227)
5. [JENKINS-29153: tap plugin fails job when not have to](https://issues.jenkins-ci.org/browse/JENKINS-29153)

## Version 1.23 (2015-05-23)

1. [JENKINS-28508: Broken link on Tap Results](https://issues.jenkins-ci.org/browse/JENKINS-28508)

## Version 1.22 (2015-03-07)

1. [Pull request \#8 to add LICENSE.txt](https://github.com/jenkinsci/tap-plugin/pull/8)

## Version 1.21

1. Dropped

## Version 1.20

1. [JENKINS-22047: Add option to reduce noise in logs](https://issues.jenkins-ci.org/browse/JENKINS-22047)

## Version 1.18

1. Updated to tap4j-4.0.8
2. [JENKINS-22047: Add option to reduce noise in logs](https://issues.jenkins-ci.org/browse/JENKINS-22047)
3. [JENKINS-22036: NullPointer when there is no Test Plan](https://issues.jenkins-ci.org/browse/JENKINS-22036)
4. [JENKINS-17960: Indicate if tests don't go to plan](https://issues.jenkins-ci.org/browse/JENKINS-17960)
5. [JENKINS-21917: TAP results graph causes null pointer exception](https://issues.jenkins-ci.org/browse/JENKINS-21917)
6. <https://github.com/jenkinsci/tap-plugin/pull/4>

## Version 1.17

1. [JENKINS-22047: Add option to reduce noise in logs](https://issues.jenkins-ci.org/browse/JENKINS-22047)
2. [JENKINS-22036: NullPointer when there is no Test Plan](https://issues.jenkins-ci.org/browse/JENKINS-22036)
3. [JENKINS-17960: Indicate if tests don't go to plan](https://issues.jenkins-ci.org/browse/JENKINS-17960)
4. [JENKINS-21917: TAP results graph causes null pointer exception](https://issues.jenkins-ci.org/browse/JENKINS-21917)
5. [Pull request \#4](https://github.com/jenkinsci/tap-plugin/pull/4)

## Version 1.17

1. [JENKINS-20924: Make plans optional in TAP via a configuration](https://issues.jenkins-ci.org/browse/JENKINS-20924)

## Version 1.16

1. Updated to tap4j-4.0.5 (better subtests handling)

## Version 1.15

1. [JENKINS-16325: TAP Parser can't handle the output from prove](https://issues.jenkins-ci.org/browse/JENKINS-16325)

## Version 1.14

1. Security bug reported by Kees J. via e-mail. This issue is related to exposing files that the user running Jenkins
   has access via the plug-in.

## Version 1.13

1. [JENKINS-17960: Indicate if tests don't go to plan](https://issues.jenkins-ci.org/browse/JENKINS-17960)

## Version 1.12

1. [JENKINS-18885: Parse errors with Git's TAP test suite, part 2](https://issues.jenkins-ci.org/browse/JENKINS-18885)
2. [JENKINS-17878: HTML test output in tapResults not escaped](https://issues.jenkins-ci.org/browse/JENKINS-17878)
3. [JENKINS-17855: TAP Stream results summary page contains links that fail](https://issues.jenkins-ci.org/browse/JENKINS-17855)
4. [JENKINS-17504: TAP Plugin generates bad detail links on "tapTestReport" page](https://issues.jenkins-ci.org/browse/JENKINS-17504)

## Version 1.11

1. [JENKINS-17859: TAP report table show failed test message on ALL tests after the failed one.](https://issues.jenkins-ci.org/browse/JENKINS-17859)
2. [JENKINS-17855: TAP Stream results summary page contains links that fail](https://issues.jenkins-ci.org/browse/JENKINS-17855)
3. [JENKINS-17941: Parse errors with Git's TAP test suite](https://issues.jenkins-ci.org/browse/JENKINS-17941)
4. [JENKINS-17947: Nested TAP not parsed correctly](https://issues.jenkins-ci.org/browse/JENKINS-17947)
5. [JENKINS-17504: TAP Plugin generates bad detail links on "tapTestReport" page](https://issues.jenkins-ci.org/browse/JENKINS-17504)

## Version 1.10

1. [JENKINS-17245: Tap plug-in can't find TAP attachments](https://issues.jenkins-ci.org/browse/JENKINS-17245)

## Version 1.9

1. [JENKINS-16262: Tap plug-in can't find TAP attachments](https://issues.jenkins-ci.org/browse/JENKINS-16262)

## Version 1.8

1. [JENKINS-15914: TAP results table misses first comment line](https://issues.jenkins-ci.org/browse/JENKINS-15914)
2. [JENKINS-15322: NOTESTS in TAP response gives parse error and stack trace from plugin](https://issues.jenkins-ci.org/browse/JENKINS-15322)
3. [JENKINS-15401: support TODO directive to not fail such tests](https://issues.jenkins-ci.org/browse/JENKINS-15401)
4. [JENKINS-15907: When multiple TAP files with same basename match pattern, only one is processed](https://issues.jenkins-ci.org/browse/JENKINS-15907)

## Version 1.7

1. [JENKINS-15586: TAP plug-in is ignoring given file extension and looking for .tap files](https://issues.jenkins-ci.org/browse/JENKINS-15586)

## Version 1.6

1. [JENKINS-15419: TAP published results hide JUnit published results](https://issues.jenkins-ci.org/browse/JENKINS-15419)
2. [JENKINS-15497: Display link to download TAP attachment](https://issues.jenkins-ci.org/browse/JENKINS-15497)

## Version 1.2.7

1. Removed requirement to have the TAP Plan at start or at the end of the TAP Stream. This way, TAP Streams generated
   using Perl done\_testing() now works well with the plug-in

## Version 1.2.6

1. Support JSON within YAMLish data

## Version 1.1

1. Support to Bail out!'s
2. JENKINS-10562 TAP Plugin fails on slave

## Version 1.0

1. Initial design of the plug-in
2. Custom UI for TAP test results
3. JUnit-like graph that displays the test results per build (actually the graph was modeled using TestNG Plug-in as
   basis)
