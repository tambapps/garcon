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
