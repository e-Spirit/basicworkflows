# BasicWorkflows

## Overview

This project contains the source files of the [BasicWorkflows](https://docs.e-spirit.com/module/basicworkflows/BasicWorkflows_Documentation_EN.html) module.

The module provides new workflows for deleting or releasing elements in the SiteArchitect and the ContentCreator.

It takes release and delete dependencies into account so that potential generation errors are minimized and the accessibility of elements is ensured.

The workflows can be individually extended by setting up own implementations to fulfill project-specific needs.

## Building the module

**Requirements**
* [Maven](https://maven.apache.org/)

To build the module, simply run

```
mvn clean install
```

in the root directory.

The built fsm file will be located at

```
./target/basicworkflows.fsm
```

## Resources

[Documentation](https://docs.e-spirit.com/module/basicworkflows/BasicWorkflows_Documentation_EN.html)

[Release notes](https://docs.e-spirit.com/module/basicworkflows/releasenotes/BasicWorkflows_Releasenotes_EN.html)

## Legal Notices
The BasicWorkflows module is a product of [Crownpeak Technology GmbH](https://www.crownpeak.com), Dortmund, Germany. The BasicWorkflows module is subject to the Apache-2.0 license.

## Disclaimer
This document is provided for information purposes only. Crownpeak may change the contents hereof without notice. This document is not warranted to be error-free, nor subject to any other warranties or conditions, whether expressed orally or implied in law, including implied warranties and conditions of merchantability or fitness for a particular purpose. Crownpeak specifically disclaims any liability with respect to this document and no contractual obligations are formed either directly or indirectly by this document. The technologies, functionality, services, and processes described herein are subject to change without notice.