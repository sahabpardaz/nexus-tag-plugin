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
This plugin provides tagging functionality by exposing RESTFul API. Each tag is a document with a unique name and a
set of optional attributes. Each tags can be associated with one or more components (artifacts).  Stored tags can be
searched via RESTFul API. Search API supports searching tags by attributes or associated components. APIs can be called
manually in 'System->API' section of Administration panel.

Here are few examples of using RESTFul APIs:

Adding a tag:
```
curl -X POST --header 'Content-Type: application/json' http://127.0.0.1:8081/service/rest/v1/tags \
  -d '{
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
}' 
```

Updating a tag:
```
curl -X PUT --header 'Content-Type: application/json' http://127.0.0.1:8081/service/rest/v1/tags/project1-142 \
  -d '{
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
}' 
```

Searching tags:
```
curl -X GET --header "accept: application/json" \
     "http://127.0.0.1:8081/service/rest/v1/tags?attribute=status%3Dsuccessful&associatedComponent=repo1%3Agr1%3Acomp1%20%3E%3D%201"
```

Deleting a tag:
```
curl -X DELETE --header "accept: application/json" "http://127.0.0.1:8081/service/rest/v1/tags/project1-142"
```

TODO: Add API Documentation

# License
This project is licensed under the Apache License - see the LICENSE.md file for details

# Future Works
* Add support for searching tags by associated components.
* Secure API by adding authentication and authorization.
* Add release workflow to publish artifact to github packages.