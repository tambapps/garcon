# Garçon - lightweight HTTP Server library for Groovy

Garçon (pronounced `gar·son`, as in [garcon de café](https://en.wiktionary.org/wiki/gar%C3%A7on_de_caf%C3%A9)) is a lightweight HTTP Server library with **no dependencies** (except Groovy) and
compatible with Android (you can use it in the [Android Groovy Shell](https://play.google.com/store/apps/details?id=com.tambapps.android.grooidshell)).

## Start a server with just a few lines of code

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
## Implement Simple CRUD API
You can implement a simple todos API (based on [JSONPlaceholder API](https://jsonplaceholder.typicode.com/)) with a Groovy script

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
postTodo(@ParsedRequestBody Todo post)  {
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
getTodo(@PathVariable("id") Integer id)  {
  def todo = todosById[id]
  if (todo) return todo
  throw new NotFoundException("Todo was not found")
}

@Patch('/todo/{id}')
patchTodo(@PathVariable("id") Integer id, @ParsedRequestBody Todo patch)  {
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

Garcon.fromInstance(accept: ContentType.JSON, contentType: ContentType.JSON, this)
    .start(address: "localhost", port: 8081)
```