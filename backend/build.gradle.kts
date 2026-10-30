// Loads the convention plugin once here, so that every module shares one
// copy of Spotless. A module whose plugins differ, as app's do with Spring
// Boot, would otherwise load a copy of its own, and Spotless's shared
// build service would refuse it.
plugins {
    id("ampolatech.java-conventions") apply false
}
