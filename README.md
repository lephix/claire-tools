# claire-tools
Tools for claire's work.

Status: ![status](https://api.travis-ci.org/lephix/claire-tools.svg)

# HOW TO USE
+ Download jar file
+ Create a command properties file with the content accordingly.
+ Put the jar file and the properties file together.
+ Run command `java -jar claire-tools-${version}.jar --spring.config.name=${command.name}`

# Command List

## collectPODTracing
Command for collecting all languages' POD progress and save the progress to tracing file.

collectPODTracing.properties sample.
```
command.name=collectPODTracing

source.folder.path=/Users/longxiang/Desktop
source.path.pattern=POD_checklist_Office.*xlsx
target.path=/Users/longxiang/Desktop/POD_Tracking_Sheet_2016.xlsx
target.week.col=E
target.lang.code.col=B
target.lang.code.row.start=2
```
