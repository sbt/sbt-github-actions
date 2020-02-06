# sbt-github-actions

A trivial plugin for assisting in building sbt projects using [GitHub Actions](https://github.com/features/actions), in the style of [sbt-travisci](https://github.com/dwijnand/sbt-travisci).

## Usage

Add the following to your `plugins.sbt`:

```sbt
addSbtPlugin("com.codecommit" % "sbt-github-actions" % <latest>)
```

## Keys

- `githubIsWorkflowBuild` : `Boolean` – Indicates whether or not the build is currently running within a GitHub Actions Workflow
