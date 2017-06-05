# Persistence [![](https://jitpack.io/v/austinv11/Persistence.svg)](https://jitpack.io/#austinv11/Persistence)
Persisting data across multiple clients made easy.

## The protocol 
This API implements the peer-to-peer-persistence-protocol v1 (available 
[here](https://gist.github.com/austinv11/b91ada1d9f85e9ef3fdeb08952916c47)). This means the nodes this communicates with 
aren't bound to this particular implementation! 

### TL;DR 
This works by allowing for the creation of peer-to-peer networks via tcp. Once the networks are established, clients can 
persist objects into the network and each mutation to the object will be shared to all (transitive) peers. The main 
advantage of this is that no central database/server is needed to manage objects. An important consequence of this is 
that ALL objects on the network are copied and stored on every peer; so without redundancy systems in place, it is 
possible to lose all data if one is not careful. 

## Shut up and show me some code! 
This implementation attempts to be as seamless as possible in its abstractions. 
```java 
PersistenceManager manager = new PersistenceManager().setServerPort(8080); //Set up your node
manager.connectTo("somehost", 8080); //Connect to the network
IMyObject obj = manager.persist(new MyObjectImpl()); //That's it!
obj.setName("Hello world"); //This change will now be reflected accross all nodes!
((Persisted) obj).unpersist(); //This unpersists the object (and yeah, it now magically implements Persisted).
```

## Adding this as a dependency
Given that `@VERSION@` = the version of Persistence (this can either be a release version, the short commit hash or `master-SNAPSHOT`).
### With Maven
In your `pom.xml` add:
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.github.austinv11</groupId>
    <artifactId>Persistence</artifactId>
    <version>@VERSION@</version>
  </dependency>
</dependencies>
```
### With Gradle
In your `build.gradle` add: 
```groovy
repositories {
  maven {
    url  "https://jitpack.io"
  }
}
dependencies {
  compile "com.github.austinv11:Persistence:@VERSION@"
}
```
### With SBT
In your `build.sbt`:
```sbt
libraryDependencies ++= Seq(
  "com.github.austinv11" % "Persistence" % "@VERSION@"
)

resolvers += "jitpack.io" at "https://jitpack.io"
```

## Important caveats in the Java API
**Note:** These can be circumvented by using an object's Store object directly, retrieved via 
`PersistenceManager#storeFor`.
* When persisting an object, it must have both getters and setters for the persisted properties.
* Persisted objects should be split into an interface and implementation; this is due to a limitation in Java Proxies
whereby they can only intercept method calls to *interfaces*.
* When modifying properties, you *must* use the object returned by persist() rather than your original object.
* It is expected that object implementations properly implement hashCode()

## The future
* Provide an annotation processing api in addition to proxies.
* Implement authentication and input validation.
* Migrate from TCP to a more robust protocol. 

