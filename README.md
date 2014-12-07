# aws-codedeploy

A Clojure plugin to create an AWS CodeDeploy Package.

The plugin looks at the appspec.yml and creates a zip archive with the files & scripts defined in it.

Optionally, it can replace tokens with values.

## Usage

Add the following entry to your plugins vector in project.clj
~~~
     :plugins [[ystad/appspec "1.0.0"]]
~~~

Define an Appspec section, as shown in the example below.

Here:
   - appspec-file, represents the appspec file. Refer [AppSpec-Reference](http://docs.aws.amazon.com/codedeploy/latest/userguide/app-spec-ref.html).
   - package, Represents the name of the code-deploy package.
   - tokens, Represents a map of the form {:<from> "to"}, these tokens will be used to replace templated values in the appspec or the scripts. See below for further details.

~~~
    :appspec { :appspec-file "appspec.yml"
               :package "myservicepackage"
               :tokens { :env "staging" } })
~~~

To execute the plugin

~~~
     > lein aws-codedeploy
~~~

A sample appspec.yml is shown below. The tokens defined in {{}} are replaced with the values defined in the tokens map. (Here, {{version}} is a special token referring to this leiningen project's version).

You can checkout a working [example](https://github.com/ystad/lein-codedeploy-example).

~~~
      version: 0.0
      os: linux
      files:
        - source: /myservice-{{version}}.jar
          destination: /opt/packages/
        - source: config/config-{{env}}.jar
          destination: /opt/packages/
        - source: target/
          destination: /opt/packages/
      hooks:
        AfterInstall:
          - location: scripts/install_package
            timeout: 300
            runas: root
~~~

## License

Distributed under the Eclipse Public License.
