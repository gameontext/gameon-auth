# Spring Security GameOn Auth.

Implementation of GameOn Auth project, using Spring Security.

*NOTE:* Still a work in progress, but successful replacement of Gameon Auth confirmed using Docker Compose.
*NOTE:* *IMPORTANT* No Twitter support yet! (Twitter is OAuth 1.0a, not OAuth2, so needs it's own handler from scratch.. yay!!)

## Overview

Initial Urls:
- `/auth/oauth2/authorization/dummy/facebook`
- `/auth/oauth2/authorization/dummy/github`
- `/auth/oauth2/authorization/dummy/google`
- `/auth/oauth2/authorization/dummy/dummy` (only active during local development)

Redirect Urls: (for configuring within social apps, prefix the host/port of this app)
- `/auth/oauth2/code/facebook`
- `/auth/oauth2/code/github`
- `/auth/oauth2/code/google`

Addtional Urls:
- `/auth/PublicCertificate`  serves pub cert for frontend use.

Old-Auth Compat urls:
- `/auth/FacebookAuth`
- `/auth/GoogleAuth`
- `/auth/TwitterAuth`
- `/auth/GithubAuth`
- `/auth/DummyAuth`

## Flow:

Browser goes to appropriate initial url, gets bounced to remote service to sign in, then back to redirect url, which reads tokens etc, and forwards browser to `/auth/token` endpoint.

`/auth/token` endpoint is a RestController, thats protected with Spring Security, requiring a successful authentication to have occurred before it can be invoked. 

The appropriate information is then retrieved from the Spring Security authentication, and is used to build the JWT to return to the user.

*Note:* All urls need to start /auth to emulate the Old-Auth context root approach, otherwise GameOn Proxy would need updating to know how to route traffic to this service. As this is currently intended to be a drop in replacement, to enable A/B testing, canary deployment etc, it was better to keep urls compatible with Old-Auth. This also includes acutators which are moved to `/auth` in this project, eg `/auth/health` )

*Note:* Had to add a secondary domain name used to access the `/auth/dummy` endpoint because that's emulating an entirely different oauth2 server, we may yet revisit this 
idea, but that's how it sits for now. 




