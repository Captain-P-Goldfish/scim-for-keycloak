# Executing the integration tests

The integration tests are separated into two variations

1. A local setup that runs against locally started keycloak
2. A docker setup that uses the configured keycloak image to deploy the scim-for-keycloak extension

you can find the specific configuration files in `scim-for-keycloak-frontend-integration-tests/config/`

* docker-setup.properties
* local-setup.properties

**NOTE:** \
The current selenium-testcontainers image does have problems with docker-desktop for windows and thus the tests cannot
be run with docker anymore. Need to wait for a fix here. (Or its just my machine cannot say for sure...)

## Running the local setup

This part will shortly explain how to run the integration tests with a local setup

### Configure local setup

Open the file `local-setup.properties` and configure the connection to your locally running keycloak and to the database
that is used by keycloak. The integration tests will also setup a direct connection to the database in order to check
that the database entries have been added correctly after actions on the view.

* **CAUTION**: \
  The tests will do delete operations on the configured database so do not run them with a productive environment!
* **Recommendation** \
  setup a new instance that has scim-for-keycloak deployed. If you connect your debugger to the running keycloak you may
  also benefit from code hot swap that makes changes to the API - that must be tested again - less disturbing.

### Execute local setup

Open the class `de.captaingoldfish.scim.sdk.keycloak.tests.LocalFrontendTests` and remove the `@Disabled` annotation. If
the file `local-setup.properties` was properly configured the tests should run without problems.

## Reducing the number of tests to be run

The integration tests are consuming quite a lot of time and are implemented as `JUnit @TestFactory`. So the pretty big
disadvantage here is that is not easily possible to execute only the desired test-method. This was done because the
tests require a login to the keycloak webadmin-console and the after-each methods should clear the changes made on the
database.

So if you want to reduce the number of tests simply uncomment all tests that you do not want to be exeucted.

## Running the docker setup

There is no need to configure anything on the docker setup. You may change the configurations on the file
`docker-setup.properties` to use another keycloak image for example but it should run on the fly if the
selenium-testcontainers library is working (it may run under linux I have not tested it yet but it does not run under
windows anymore - at least on my machine - )

