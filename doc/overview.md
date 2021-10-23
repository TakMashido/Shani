SHANI

Internally it's modular interpreter of Intend objects.
Intends(or actually IntendBase interface it's wrapper to) is raw descriptor of action user is willing to take.
For now it's only ShaniString object, but others(e.g. gesture descriptor fetched with work in progress
module from camera).

Every part shani except framework core is modular, everything is just dynamically loaded object doing it's job.
This includes:


