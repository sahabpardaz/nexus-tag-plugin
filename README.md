![Build and Test](https://github.com/sahabpardaz/nexus-tag-plugin/workflows/Build%20and%20Test/badge.svg)
# Tag Plugin for Nexus Repository OSS

This plugin provides tagging support to Nexus Repository OSS 3.x. Note that this feature is available on
Nexus Repository Pro and it has some advantage over this plugin (better support, better integration, ...). So, it's
intended to be used by whom can not buy Pro version.

# Installation
Download the latest jar bundle from [Github packages](https://github.com/sahabpardaz/nexus-tag-plugin/packages) and copy
it to 'deploy' directory of target nexus repository manager. For more information about installing plugins, see
"[Installing a custom Nexus 3 plugin](https://sonatype-nexus-community.github.io/nexus-development-guides/plugin-install.html)".

# Usage
This plugin provides tagging functionality by exposing RESTFul API. Each tag has a unique name and, a set of attributes.
It can be associated with one or more existing components (artifacts) in nexus. Stored tags can be searched via RESTFul
API. Search API supports searching tags by attributes or associated components. APIs can be called
manually in 'System->API' section of Administration panel.

Here are few examples of using RESTFul APIs:

Adding a tag:
```
POST http://127.0.0.1:8081/service/rest/v1/tags
Content-Type: application/json

{
    "name": "project1-142",
    "attributes": {
        "version": "1",
        "status": "failed"
    },
    "components" : [
        {
          "repository": "repo1",
          "group": "gr1",
          "name": "comp1",
          "version": "1"
        }
    ]
}
```

Searching tags:
```
GET http://127.0.0.1:8081/service/rest/v1/tags?attribute=status:successful&associatedComponent=repo1:gr1:comp1 >= 1

Accept: application/json
```
Tags can be searched by attributes or associated components. Attribute filter can be added using 'attribute' query
parameter. Parameter format is: key:value. Multiple filters can be added by defining multiple parameters.

Components can be searched via one or more 'associatedComponent' query parameter. Parameter format is
'repository:group:name op version'. e.g. repo1:gr1:n1 > 1.0.0 adds a filter to search in order to match tags that has an
associated component named 'n1', in 'g1' group of 'repo1' repository that its version is higher than '1.0.0'.

Updating a tag:
```
PUT http://127.0.0.1:8081/service/rest/v1/tags/project1-142
Content-Type: application/json

{
    "name": "project1-142",
    "attributes": {
        "version": "1",
        "status": "successful"
    },
    "components" : [
        {
          "repository": "repo1",
          "group": "gr1",
          "name": "comp1",
          "version": "1"
        },
        {
          "repository": "repo1",
          "group": "gr1",
          "name": "comp2",
          "version": "3"
        }
    ]
}
```

Deleting a tag:
```
DELETE http://127.0.0.1:8081/service/rest/v1/tags/project1-142
Accept: application/json
```


Importing tags:
```
POST http://127.0.0.1:8081/service/rest/v1/import-tags
Content-Type: application/json
[
    {
        "name": "project1-142",
        "attributes": {
            "version": "1",
            "status": "failed"
        },
        "components" : [
            {
              "repository": "repo1",
              "group": "gr1",
              "name": "comp1",
              "version": "1"
            }
        ]
        "firstCreated": "2020-10-21T06:26:27.236+0000",
        "lastUpdated": "2020-10-21T06:26:27.236+0000"
    },
    {
        "name": "project1-143",
        "attributes": {
            "version": "2",
            "status": "successful"
        },
        "components" : [
            {
              "repository": "repo1",
              "group": "gr1",
              "name": "comp1",
              "version": "2"
            }
        ]
        "firstCreated": "2020-10-22T06:26:27.236+0000",
        "lastUpdated": "2020-10-22T06:26:27.236+0000"
    }
]
```

# Compatibility
Minimum supported version of nexus is 3.15.1-01 at the moment. These versions have been tested:
* 3.15.1
* 3.27.0

Plugin may work with other versions, but it hasn't been tested yet.

# License
This project is licensed under the Apache License - see the LICENSE.md file for details

# Future Works
* Secure API by adding authentication and authorization.