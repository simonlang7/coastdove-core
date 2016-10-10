# coastdove-core
Coast Dove Core Framework

Coast Dove is a framework for modules to enhance third-party apps. This works without modifying apks and doesn't require root, because it's built on Android's accessibility features.
Coast Dove modules can:
- react to certain events sent by the core
- request a view tree of all elements displayed on the screen
- interact with elements on the screen
- display overlays (simplified by a helper class)

Coast Dove is an acronym for [co]llect [a]pp usage [st]atistics, [d]isplay [ove]rlays, which were its original purposes. Then it developed into a more general framework, but the name stuck. It was tested on Android devices with KitKat (4.4) and higher, but interacting with elements on the screen currently only works with Lollipop and above.

To build a Coast Dove module, download Coast Dove Lib and include it in your project. The module can be registered using CoastDoveModules.registerModule, and should inherit CoastDoveListenerService (be sure to define the service in your AndroidManifest.xml).

(More info to follow)
