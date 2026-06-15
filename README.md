# sbt-git-publish

Adds support for publishing SBT projects to a local directory and then pushing them to an S3 bucket.

## Install

```scala
resolvers += "bondlink-maven-repo" at "https://maven.bondlink-cdn.com"
addSbtPlugin("bondlink" % "sbt-s3-publish" % "0.0.1")
```

## Settings

### `s3PublishBucket`

The `s3PublishBucket` setting is required to specify the S3 bucket where releases will be published.

### `s3PublishDir`

The `s3PublishDir` setting is required to configure the path to the local git repository directory for publishing.

```scala
s3PublishDir := file(sys.env("HOME")) / "my-maven-repo"
```

### `s3PublishBucket` (optional)

The `gitPublishRemote` setting controls which configured git remote the plugin pushes releases to. If your git repository only has one configured remote (e.g. `origin`), then the plugin will automatically use it, otherwise you must specify this value.

### `gitPublishBranch` (optional)

The `gitPublishBranch` setting controls which configured git branch the plugin publishes to. By default the plugin tries to guess this value based on the main branch of the configured git remote.

## Usage

To publish your artifacts to the local git repository, simply run

```scala
sbt:sbt-s3-publish> publish
```

Then you can release (i.e. push) the changes to the remote git repository via the `gitRelease` task:

```scala
sbt:sbt-s3-publish> gitRelease
```
