# Contributing to the Notification Service.

This document describes how to contribute to the Notification Service project.

## How to submit a feature request
Contact the PO mentioned in the [README.md]  .
Please provide a feature description and why this should be added to this service. Also, please describe the definition of done (DoD) criteria.

## Gerrit Project Details  
Notification Service codebase is stored in the Gerrit repos mentioned in the [README.md]
  
### Documents

The documentation for the Notification Service is located in the [doc folder](https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.oss.common.service/eric-oss-notification-service/+/master/doc).

To update documents that are not in the referenced folder, contact the service guardians mentioned in the [README.md](https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.oss.common.service/eric-oss-notification-service/+/master/README.md).

## Contribution Workflow
The Notification Service source code repositories are described in the
[README.md](https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.oss.common.service/eric-oss-notification-service/+/master/README.md).

1. If the **contributor** changes the Notification Services interfaces, he/she
   is responsible for:
   - describing any change in the REST API and/or the messaging payload in the API spec project.
   - updating the pre-integration and integration tests accordingly
   - preserving compatibility with the existing behavior: this means that any new
     contribution is optional and Notification Service can work equally well when
     the new contribution is disabled
2. If the **contributor** introduces any new 3PP, he/she is responsible for:
   - choosing from Bazaar a version having ESW2 and performs generic FOSS if not available in Bazaar.
   - performing specific FOSS review for the software vendor lists where the Notification Service is integrated.
3. The **contributor** updates the application code in its local repository.
   He/she is responsible for:
   - implementing new unit tests e.g. when new classes are introduced or existing ones are extended
   - keeping the [SonarQube](https://sonarqube.lmera.ericsson.se/dashboard?id=eric-eo-notification-service) score at the agreed level
   - implementing any needed code for e.g. database schemas and migration, messaging system configuration etc
4. The **contributor** pushes the update to Gerrit for review, including a reference to the JIRA.
5. The **contributor** invites the **service guardians** (mandatory) and **other relevant parties** (optional) to the Gerrit review, and makes no further changes to the patchset until it is reviewed.
6. The **service guardians** review the patchset and give a code-review score.
The code-review scores and corresponding workflow activities are as follows:
    - Score is +1
        A **reviewer** is happy with the changes but approval is required from another reviewer.
    - Score is +2
        The **service guardian** accepts the change and merges the code to master branch.
	The Publish job is initiated to make the change available to consumers.
    - Score is -1 or -2
        The **contributor** follows-up on reviewer comments, until changes approved by service guardian.
    - The **service guardian** and the **contributor** align to determine when and how the change is published.
7. Upon successful completion of the Publish job, the **contributor** follows that
   the Integration job completes successfully and the Notification Service enters the integration charts.

   [README.md]: <https://gerrit.ericsson.se/#/admin/projects/OSS/com.ericsson.oss.common.service/eric-oss-notification-service/+/master/README.md>

The **contributor** does **not** need to manually update the Common Base OS (CBOS) version.
This is done automatically every time a new CBOS is released.
