![Build and Test](https://github.com/sahabpardaz/nexus-tag-plugin/workflows/Build%20and%20Test/badge.svg)
# Tag Plugin for Nexus Repository OSS

This plugin provides tagging support to Nexus Repository OSS 3.x. Note that this feature is available on
Nexus Repository Pro and it has some advantage over this plugin (better support, better integration, ...). So, it's
intended to be used by whom can not buy Pro version.

# Installation
Download the latest release and copy the jar bundle to deploy directory of target nexus repository manager.
For more information about installing plugins, see 
"[Installing a custom Nexus 3 plugin](https://sonatype-nexus-community.github.io/nexus-development-guides/plugin-install.html)".

# Usage
This plugin provides tagging functionality by exposing RESTFul API. See swagger documentation in System->API section of 
Administration panel to see how this can be used.

TODO: Add API Documentation

# License
This project is licensed under the Apache License - see the LICENSE.md file for details

# Future Works
* Add support for searching tags by associated components.
* Secure API by adding authentication and authorization.
* Add release workflow to publish artifact to github packages.