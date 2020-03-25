# sbt-github-actions

A trivial plugin for assisting in building sbt projects using [GitHub Actions](https://github.com/features/actions), in the style of [sbt-travisci](https://github.com/dwijnand/sbt-travisci).

## Usage

Add the following to your `plugins.sbt`:

```sbt
addSbtPlugin("com.codecommit" % "sbt-github-actions" % <latest>)
```

## Keys

- `githubIsWorkflowBuild` : `Boolean` – Indicates whether or not the build is currently running within a GitHub Actions Workflow
- `githubWorkflowName` : `String` – The name of the currently-running workflow. Will be undefined if not running in GitHub Actions.
- `githubWorkflowDefinition` : `Map[String, Any]` – The raw (parsed) contents of the workflow YAML definition. Will be undefined if not running in GitHub Actions, or if (for some reason) the workflow could not be identified. Workflows are located by taking the `githubWorkflowName` and finding the YAML definition which has the corresponding `name:` key/value pair.
