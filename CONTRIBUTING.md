# Contributing to sbt-github-actions

## Windows

Currently, the project uses symbolic links which requires special handling when working on Windows.
Firstly you need to make sure that you have [mklink](https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/mklink)
permissions as a user, the easiest way to do this is if you happen to be running Windows 10/Windows 11 
is to enable [Developer Mode](https://learn.microsoft.com/en-us/windows/apps/get-started/developer-mode-features-and-debugging?source=recommendations).
After this is done then you can enable `symlinks` globally by doing

```shell
git config --global core.symlinks true
```

Alternately if you don't want to enable `symlinks` globally you can just selectively enable it when checking
out this repository, i.e.

```shell
git clone -c core.symlinks=true git@github.com:sbt/sbt-github-actions.git
```
