# Custard

An improved version of the requirements, architecture and work planning
tool [MUSTARD](http://github.com/CodethinkLabs/mustard) implemented
in Clojure.

## Introduction

TODO

### Benefits

* Developer-friendly - all it takes is a Git repository and a text editor
* Standard code review workflows can be used for plans made with Custard
* Bring external documents (whitepapers, specs, emails...) into version
  control

### Running Custard

#### Running Custard from source

**Create a Custard repository**

```
mkdir ~/your-project
cd ~/your-project
git init
```

**Clone and run Custard**

You will need to install https://github.com/boot-clj/boot first for this.

```
git clone https://github.com/jannis/custard
cd custard
```

Followed by either:
```
CUSTARD_PATH=~/your-project boot run-development
```
or:
```
CUSTARD_PATH=~/you-project boot run-production
```

**Open Custard in the browser**

Then point your browser to http://localhost:3000 and you should
be good to go!

#### Serious deployment

To obtain an uberjar for deployment, simply run
```
boot uberjar
```
at the root of the Custard source repository.

This will result in a `target/custard-<version>.jar` to be built that
can then be pushed to a deployment target (e.g. a server, Docker
container, etc.) of your choice. It can be executed using
```
CUSTARD_PATH=/path/to/a/repository java -jar custard-<version>.jar
```
and will spin up two web servers, one for the backend (serving at port
3001) and one for the web frontend (serving at port 3000). By default,
the frontend will assume the backend is served from the same host. This
can be configured at build time:

```
BACKEND_URL=http://some-server:8080/path/to/backend \
APP_PORT=8000 \
BACKEND_PORT=8080 \
boot uberjar
```

Since it is written into the generated client JavaScript, the
`BACKEND_URL` can only be set at build-time. `APP_PORT` and
`BACKEND_PORT` can be set at runtime, just like `CUSTARD_PATH`.

## License

Copyright (C) 2015 Jannis Pohlmann.

Licensed under GNU Affero General Public License 3.0.
See the LICENSE file for details.
