# sbt-s3-publish

Adds support for publishing SBT projects to a local directory and then pushing them to an S3 bucket.

## Install

```scala
resolvers += "bondlink-maven-repo" at "https://maven.bondlink-cdn.com"
addSbtPlugin("bondlink" % "sbt-s3-publish" % "0.0.1")
```

## Settings

### `s3PublishBucket`

The `s3PublishBucket` setting is required to specify the S3 bucket where releases will be published.

### `s3PublishDir` (optional)

The `s3PublishDir` settings specifies where files will be published before being released to S3. It's optional and by default uses a directory named `sbt-s3-publish` in the `XDG_CACHE_HOME` directory, or in `$HOME/.cache` if `XDG_CACHE_HOME` is not defined.

## Usage

To publish your artifacts to the local `s3PublishDir` directory, simply run

```scala
sbt:sbt-s3-publish> publish
```

Then you can release the changes to the S3 bucket via the `s3Release` task:

```scala
sbt:sbt-s3-publish> s3Release
```
