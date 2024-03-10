# Garçon - lightweight HTTP Server library for Groovy and Marcel

Garçon (pronounced `gar·son`, as in [garçon de café](https://en.wiktionary.org/wiki/gar%C3%A7on_de_caf%C3%A9)) is a lightweight HTTP Server library for Groovy and [Marcel](https://tambapps.github.io/marcel/), my own programming language.

Its goal is to implement HTTP server the quickest and clearest way possible.

This library is hosted on Maven Central.
- [garcon-groovy](https://central.sonatype.com/artifact/com.tambapps.http/garcon-groovy/2.0).
- [garcon-marcel](https://central.sonatype.com/artifact/com.tambapps.http/garcon-marcel/2.0).

## Implement Simple CRUD API

You can implement a simple todos API (based on [JSONPlaceholder API](https://jsonplaceholder.typicode.com/)) with a script

### In Groovy

```groovy
@Grab('com.tambapps.http:garcon:1.1-SNAPSHOT')
import com.tambapps.http.garcon.*
import com.tambapps.http.garcon.annotation.*
import com.tambapps.http.garcon.exception.*
import groovy.transform.Field

class Todo {
  Integer userId
  Integer id
  String title
  Boolean completed
}

@Field
final Map todosById = [
    new Todo(id: 1, userId: 1, title: "delectus aut autem", completed: false),
    new Todo(id: 2, userId: 1, title: "quis ut nam facilis et officia qui", completed: false),
    new Todo(id: 3, userId: 1, title: "fugiat veniam minus", completed: false),
    new Todo(id: 4, userId: 1, title: "et porro tempora", completed: true)
].collectEntries { [it.id, it] }

@ResponseStatus(HttpStatus.CREATED)
@Post('/todos')
postTodo(@ParsedRequestBody Todo post) {
  if (!post.userId || !post.title || post.completed == null) {
    throw new BadRequestException("Some fields are missing/malformed")
  }
  post.id = todosById.size() + 1
  todosById[post.id] = post
  return post
}

@Get('/todos')
getTodos() {
  return todosById.values().sort { it.id }
}

@Get('/todo/{id}')
getTodo(@PathVariable("id") Integer id) {
  def todo = todosById[id]
  if (todo) return todo
  throw new NotFoundException("Todo was not found")
}

@Patch('/todo/{id}')
patchTodo(@PathVariable("id") Integer id, @ParsedRequestBody Todo patch) {
  def todo = getTodo(id)
  if (patch.userId) todo.userId = patch.userId
  if (patch.title) todo.title = patch.title
  if (patch.completed != null) todo.completed = patch.completed
  return todo
}

@Delete('/todo/{id}')
deleteTodo(@PathVariable("id") Integer id) {
  def todo = getTodo(id)
  todosById.remove(id)
  return todo
}

void onStart(InetAddress address, int port) {
  println "Started on $address:$port"
}

Garcon.fromInstance(this, accept: ContentType.JSON, contentType: ContentType.JSON)
    .start(address: "localhost", port: 8081)
```

## Define your endpoints dynamically

You can also define endpoints dynamically using `Garcon.serve(Closure)` method

````groovy
def garcon = new Garcon(InetAddress.getByName("localhost"), 8081)
garcon.serve {
  get 'hello/{someone}', {
    return "Hello $someone"
  }
  get '/hello', contentType: ContentType.JSON, {
    return [hello: 'world']
  }
  post '/path', accept: ContentType.JSON, {
    return "Hello ${parsedRequestBody.who}"
  }
}

garcon.join()
````

### In Marcel

I created [Marcel](https://tambapps.github.io/marcel/), my own programming language.

```kotlin
dumbbell 'com.tambapps.http:garcon-marcel:2.0-SNAPSHOT'

/**
 * imports
 **/
import com.tambapps.http.garcon.*
import com.tambapps.http.garcon.annotation.*
import com.tambapps.http.garcon.exception.*
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

/**
 * Data
 **/
static final AtomicInteger ID_INCREMENT  = new AtomicInteger()
static final List TODOS = [
  new Todo(id: ID_INCREMENT.incrementAndGet(), userId: 1, title: "Go to the grocery store", completed: false),
  new Todo(id: ID_INCREMENT.incrementAndGet(), userId: 1, title: "Finish homeworks", completed: false),
  new Todo(id: ID_INCREMENT.incrementAndGet(), userId: 2, title: "Do the dishes", completed: false),
  new Todo(id: ID_INCREMENT.incrementAndGet(), userId: 3, title: "Charge computer", completed: true)
]

class Todo {
  Integer id
  Integer userId
  String title
  Boolean completed

  constructor(this.id, this.userId, this.title, this.completed)
}

/**
 * API
 **/
@Get('/todos')
fun Collection getTodos() {
  return TODOS
}

@Get('/todos/{id}')
fun Object getTodo(@PathVariable("id") Integer id) {
  Todo todo = TODOS.find { Todo it -> it.id == id }
  if (todo) return todo
  throw new NotFoundException("Todo with id $id not found")
}

@ResponseStatus(CREATED)
@Post('/todos')
fun Object postTodo(@ParsedRequestBody dynobj post) {
  if (!post.userId || !post.title) {
    throw new BadRequestException("Some fields are missing/malformed")
  }
  TODOS.add(
    new Todo(id: ID_INCREMENT.incrementAndGet(), userId: post.userId.asInt(), title: post.title.asString(), completed: false)
  )
  return post
}

@Patch('/todos/{id}')
fun Object patchTodo(@PathVariable("id") Integer id, @ParsedRequestBody dynobj patch) {
  Todo todo = getTodo(id)
  if (patch.userId != null) todo.userId = patch.userId.asInt()
  if (patch.title != null) todo.title = patch.title.asString()
  if (patch.completed != null) todo.completed = patch.completed.asBool()
  return todo
}

@Delete('/todos/{id}')
fun Object deleteTodo(@PathVariable("id") Integer id) {
  Todo todo = TODOS.find { Todo it -> it.id == id }
  TODOS.remove(todo)
  return todo
}


fun void onStart(InetAddress address, int port) {
  println("Started on ${address.hostName}:$port")
}

Garcon.fromInstance(this, accept: ContentType.JSON, contentType: ContentType.JSON)
    .start(address: "localhost", port: 8081)
```
