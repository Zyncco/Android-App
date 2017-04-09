# Zync for Android Documentation

This directory serves as a utility to anyone who is interested
in exploring the inner workings of the project, for contributing
reasons or what have you. This documentation does not repeat any
expected client behaviour which is set out in the `Zync/Specs`
directory; it only explains the behaviours and methods which are
unique to this project/Android client. If you are setting up your
development environment for the first time, please refer to the root
README to know how to do so.

Files in this directory usually do not explain classes individually
but moreover cover characteristics across the project. When classes
are referenced, they will be in a JavaDoc format such as 
`ZyncApplication#onStart()`

## `api` package

The API package is built to be able to be extracted from the project
and still be usable for Java-based projects which want to use the Zync
API. This package will later become a library when the API reaches it's
first major release (1.0).