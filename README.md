# Garçon - lightweight HTTP Server library for Groovy

Garçon (pronounced `gar·son`, as in [garcon de café](https://en.wiktionary.org/wiki/gar%C3%A7on_de_caf%C3%A9)) is a lightweight HTTP Server library with **no dependencies** (except Groovy) and
compatible with Android (you can use it in the [Android Groovy Shell](https://play.google.com/store/apps/details?id=com.tambapps.android.grooidshell)).

## Start a server with just a few lines of code

````groovy
def garcon =  new Garcon(InetAddress.getByName("localhost"), 8081)
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
@Grab('com.tambapps.http:garcon:1.1')
import com.tambapps.http.garcon.*
import com.tambapps.http.garcon.annotation.*
import com.tambapps.http.garcon.exception.*
import groovy.transform.Field

@Field
final Map todosById = [[userId: 1, id: 1, title: "delectus aut autem", completed: false]].collectEntries { [it.id, it] } as LinkedHashMap
// TODO create class Todo and use it

@Post('/todos')
postTodo(@ParsedRequestBody Map post)  {
  if (!(post.userId instanceof Integer) || !(post.title instanceof String) || !(post.completed instanceof Boolean)) {
    throw new BadRequestException("Some fields are missing/malformed")
  }
  post.id = todosById.size() + 1
  todosById[post.id] = post
  return post
}

@Get('/todos')
getTodos() {
  return todosById.values()
}

@Get('/todo/{id}')
getTodo(@PathVariable("id") Integer id)  {
  def todo = todosById[id]
  if (todo) return todo
  throw new NotFoundException("Todo was not found")
}

@Patch('/todo/{id}')
patchTodo(@PathVariable("id") Integer id, @ParsedRequestBody Map patch)  {
  def todo = getTodo(id)
  if (patch.userId instanceof Integer) todo.userId = patch.userId
  if (patch.title instanceof String) todo.title = patch.title
  if (patch.completed instanceof Boolean) todo.completed = patch.completed
  return todo
}

@Delete('/todo/{id}')
deleteTodo(@PathVariable("id") Integer id)  {
  def todo = getTodo(id)
  todosById.remove(id)
  return todo
}

void onStart(InetAddress address, int port) {
  println "Started on $address:$port"
}

Garcon.fromInstance(this).tap {
  accept = ContentType.JSON
  contentType = ContentType.JSON
  address = "localhost"
  port = 8081
}.start()
```