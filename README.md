# Project Stark

Site: https://sinri.github.io/stark/

## Naming

Named After: シュタルク
 
> フリーレンとフェルンと共に旅をすることになった戦士で、アイゼンの弟子。子供っぽい性格かつ不器用で臆病ながら、優しい心の持ち主で、旅先の人々によく好かれる。高い戦闘力を持ち、パーティーの前衛を務める。

## Maven Repository

```xml
<dependency>
    <groupId>io.github.sinri</groupId>
    <artifactId>stark</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Background

Facing the AI Coding tide, the previous work, Framework Keel is not so friendly for AI,
while I do not like Spring Eco, which is too heavy and everything is defined by others.

So I decided to build a new framework, Stark, which should be lightweight and easy to use,
both for human developers and AI Coders.

## Principle

- Use Vert.x 5.0.x with JDK 25+; 
- Mark Nullability with org.jspecify:jspecify standard;
- Logging 
  - Use Logger with SLF4J, and the log format could be text or JSON object;
  - Support send logs to Aliyun Log Service, use API;
- Database
  - Directly write MySQL SQL to handle the database, not use model;
- Data Entity
  - Use JsonObject to represent the data entity;
  - Not use lombok, write getters and setters in code directly;
- Web Framework
  - Use Vert.x Web;
  - Commonly, map one class for one API as the final handler of the request;