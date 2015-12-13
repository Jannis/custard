# Custard

An improved version of the requirements, architecture and work planning
tool [MUSTARD](http://github.com/CodethinkLabs/mustard) implemented
in Clojure.

## Introduction

TODO

### Benefits

* Developer-friendly - all it takes is a Git repository and a text editor
* Standard code review workflows can be used for plans made with Custard

## Usage

### Get started locally

**Create a Custard repository**

```
mkdir ~/your-project
cd ~/your-project
git init
```

**Clone and start Custard**

For this, you will need to install https://github.com/boot-clj/boot
first.

```
git clone https://github.com/jannis/custard
cd custard
Custard_PATH=~/your-project boot run-development
```

**Open Custard in the browser**

Then point your browser to `http://localhost:3000` and you should
be good to go!

## License

Copyright (C) 2015 Jannis Pohlmann.

Licensed under GNU Affero General Public License 3.0.
See the LICENSE file for details.
