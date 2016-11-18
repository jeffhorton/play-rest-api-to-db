# Learning about Scala through Play Framework

It's been a while since I looked at the [Play Framework](https://www.playframework.com) and
I've been hearing things about [Scala](http://www.scala-lang.org) so I decided to give them
a go and learn some things along the way.

Here are my notes, comments and I'm pretty sure exactly each step I took so you can follow along.
Find the final code at [https://github.com/jeffhorton](https://github.com/jeffhorton).

This all took place on my Ubuntu 14.04 LTS desktop that you see me lugging around.

### Installing things

SCALA SBT Debs from [SBT](http://www.scala-sbt.org/download.html) exactly as their instructions say

Used the PlayFramework example at

Play Rest Api [Play](https://github.com/playframework/play-rest-api)

```
>git clone https://github.com/playframework/play-rest-api
```
Started SBT
```
>./sbt
```
Twenty odd minutes later
```
[error] java.lang.UnsupportedClassVersionError: com/typesafe/config/ConfigException : Unsupported major.minor version 52.0
[error] Use 'last' for the full log.
Project loading failed: (r)etry, (q)uit, (l)ast, or (i)gnore?
```
Ctrl-C and Time to update my java
```
>sudo add-apt-repository ppa:openjdk-r/ppa
>sudo apt-get update
>sudo apt-get install openjdk-8-jdk

The following extra packages will be installed:
  openjdk-8-jdk-headless openjdk-8-jre openjdk-8-jre-headless
Suggested packages:
  openjdk-8-demo openjdk-8-source visualvm icedtea-8-plugin
  openjdk-8-jre-jamvm fonts-ipafont-gothic fonts-ipafont-mincho
  ttf-telugu-fonts ttf-oriya-fonts ttf-kannada-fonts ttf-bengali-fonts
The following NEW packages will be installed:
  openjdk-8-jdk openjdk-8-jdk-headless openjdk-8-jre openjdk-8-jre-headless

>sudo update-alternatives --config java

There are 2 choices for the alternative java (providing /usr/bin/java).

  Selection    Path                                            Priority   Status
------------------------------------------------------------
* 0            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      auto mode
  1            /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java   1071      manual mode
  2            /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java   1069      manual mode


Press enter to keep the current choice[*], or type selection number: 2
update-alternatives: using /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java to provide /usr/bin/java (java) in manual mode

>./sbt
```
And the building continues.  Once the [play-rest-api] appears we 'run' to start it
```
[info] Set current project to play-rest-api (in build file:/home/jhorton/clients/bcubed/coding/scala/play/rest/play-rest-api/)
[play-rest-api] $ run
--- (Running the application, auto-reloading is enabled) ---
[info] p.c.s.NettyServer - Listening for HTTP on /0:0:0:0:0:0:0:0:9000
(Server started, use Ctrl+D to stop and go back to the console...)
```

Open the browser and load the site at localhost:9000
```
[info] Compiling 15 Scala sources and 1 Java source to /home/jhorton/clients/bcubed/coding/scala/play/rest/play-rest-api/target/scala-2.11/classes...
[info] 'compiler-interface' not yet compiled for Scala 2.11.8. Compiling...
[info]   Compilation completed in 13.94 s
[info] play.api.Play - Application started (Dev)
[debug] a.ErrorHandler - onClientError: statusCode = 404, uri = /favicon.ico, message =
```

Ok, I've got the index page
```
Play REST API

This is a placeholder page to show you the REST API.

/v1/posts
```
And following the link 
```
[{"id":"1","link":"/v1/posts/1","title":"title 1","body":"blog post 1"},{"id":"2","link":"/v1/posts/2","title":"title 2","body":"blog post 2"},{"id":"3","link":"/v1/posts/3","title":"title 3","body":"blog post 3"},{"id":"4","link":"/v1/posts/4","title":"title 4","body":"blog post 4"},{"id":"5","link":"/v1/posts/5","title":"title 5","body":"blog post 5"}]
```

### Looking around
I'd like to understand the routing and how we draw these pages
```
/conf/routes
GET        /                       controllers.HomeController.index

->         /v1/posts               v1.post.PostRouter

# Map static resources from the /public folder to the /assets URL path
GET        /assets/*file        controllers.Assets.at(path="/public", file)
```
Looks like a GET to / is a straightforward call to a controller method as in all MVC style setups

The "-> /v1/posts v1.post.PostRouter" seems a bit different, I'm going to follow that
```
/controllers/v1/post/PostRouter.scala
override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case POST(p"/") =>
      controller.process

    case GET(p"/${int(id)}") =>
      controller.show(id)

    case POST(p"/${int(id)}") =>
      controller.update(id)
  }
```
So a simple GET goes onto a controller.index, what controller? The injected one further up. 
```
class PostRouter @Inject()(controller: PostController) extends SimpleRouter {
```
Following the controller.index we get to this. A bit less obvious, I've added notes based on my readings
```
/v1/post/PostController.scala
def index: Action[AnyContent] = {           // Action[AnyContent], AnyContent seems to be the default
    action.async { implicit request =>      // async is so our handler can deal with a Future response
      handler.find.map { posts =>           // and be-nonblocking
        Ok(Json.toJson(posts))              // Take the reply, turn it out as JSON with a 200 code
      }
    }
  }
```
Other examples of this from the documentation
```
def index = Action {
  val ok = Ok("Hello world!")
  val notFound = NotFound
  val pageNotFound = NotFound(<h1>Page not found</h1>)
  val badRequest = BadRequest(views.html.form(formWithErrors))
  val oops = InternalServerError("Oops")
  val anyStatus = Status(488)("Strange response type")
  Ok("Hello world!")
}
```

Now to follow handler and then handler.find 
```
Still in /v1/post/PostController.scala, further up
handler: PostResourceHandler)(implicit ec: ExecutionContext)
```
This looks like all handler calls are passed off somewhere else. With my comments.
```
/v1/post/PostResourceHandler.scala
def find: Future[Iterable[PostResource]] = {			            // return a future list of PostResource
    postRepository.list().map { postDataList =>	                    // as a postRepository for a list
      postDataList.map(postData =>                                  // iterate the list() 
        createPostResource(postData)                                // and turn items into 
        )                                                           // PostResource objects
    }           
  }
```
Where does this postRepository.list() come from?
```
/v1/post/postRepository.scala
  private val postList = List(
    PostData(PostId("1"), "title 1", "blog post 1"),
    PostData(PostId("2"), "title 2", "blog post 2"),
    PostData(PostId("3"), "title 3", "blog post 3"),
    PostData(PostId("4"), "title 4", "blog post 4"),
    PostData(PostId("5"), "title 5", "blog post 5")
  )

  override def list(): Future[Iterable[PostData]] = {
    Future.successful {
      logger.trace(s"list: ")
      postList
    }
  }
```

Looks like we found where the http://localhost:9000/v1/posts data comes from.  What about a single ID?
```
/v1/post/PostRouter.scala
    case GET(p"/${int(id)}") =>
      controller.show(id)
```
Becomes
```
/v1/post/PostController.scala
  def show(id: String): Action[AnyContent] = {  // this looks about the same as our index
    action.async { implicit request =>			// request but now into handler.lookup
      handler.lookup(id).map { post =>
        Ok(Json.toJson(post))
      }
    }
  }
```
And on into
```
/v1/post/PostResourceHandler.scala
  def lookup(id: String): Future[Option[PostResource]] = {
    val postFuture = postRepository.get(PostId(id))	        // look for a single matching id.
    postFuture.map { maybePostData =>		                // Iterate a response 
      maybePostData.map { postData =>		                // and turn into a PostResource again
        createPostResource(postData)
      }
    }
  }
```
Following the chain again
```
/v1/post/postRepository.scala
   override def get(id: PostId): Future[Option[PostData]] = {
    Future.successful {
      logger.trace(s"get: id = $id")                        // With the postList default again again
      postList.find(post => post.id == id)                  // use build in List.find to search		
    }
  }
```

Ok, happy so far, seems to make sense. Want to follow the POST path too and then we will see about changing things

```
/v1/post/PostRouter.scala
case POST(p"/") =>
      controller.process
```
Becomes a lot more stuff this time.  Again I've added comments.
```
/v1/post/PostController.scala
def process: Action[AnyContent] = {
    action.async { implicit request =>
      processJsonPost()				        // hand our request to a function in this controller
    }
  }

 private def processJsonPost[A]()(		
      implicit request: PostRequest[A]): Future[Result] = {


    def failure(badForm: Form[PostFormInput]) = {		    // FAILURE handler
      Future.successful(BadRequest(badForm.errorsAsJson))   // if form.bind fails. Error to json style
    }


    def success(input: PostFormInput) = {			        // SUCCESS handler
                                                            // if form.bind.success this is
      handler.create(input).map { post =>			        // passed into the Repository
								                            // to look like a real PostResource
        Created(Json.toJson(post)).withHeaders(LOCATION -> post.link)
      }
    }


    form.bindFromRequest().fold(failure, success)	        // This takes the inbound POST request,
                                                            // tries to map it onto 'form'
                                                            // and specifies our event handlers above
  }

private val form: Form[PostFormInput] = {		            // tries to bind the post[title] and post [body]
    import play.api.data.Forms._			                // into a form object

    Form(
      mapping(
        "title" -> nonEmptyText,                            
        "body" -> text
      )(PostFormInput.apply)(PostFormInput.unapply)
    )
  }
```
A lot more going on there. It again seems straightforward. Get inbound data, try to make an object.
In the success handler you notice a call out to handler, that is 
```
/v1/post/PostResourceHandler.scala
 def create(postInput: PostFormInput): Future[PostResource] = {
    val data = PostData(PostId("999"), postInput.title, postInput.body)
    // We don't actually create the post, so return what we have
    postRepository.create(data).map { id =>
      createPostResource(data)
    }
  }
```
In this sample code there is no real storage so it takes the inbound post and fakes 
it back out as a PostResource again.

### Break stuff

Now that we have some sense what is happening I'm going to try and change things up. No big
refactorings, just enough to see what adding a real database might look like. 

Fair warning here. I tried SQLite3 first and kept getting a Timeout I couldn't resolve so I moved on.
While I could have gone Postgres just as easily I went with MySQL here. They should be nearly identical
except for the driver I think.

I've decided to try the Play Slick abstraction and using Play Evolutions for database migrations.

```
/built.sbt
libraryDependencies ++= Seq(				                // Play website suggests this form
  "com.typesafe.play" %% "play-slick" % "2.0.0"
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0"
)
```
Now update to get those installed
```
>./sbt update
[error] [/home/jhorton/clients/bcubed/coding/scala/play/rest/play-rest-api/build.sbt]:17: ')' expected but string literal found.
[error] [/home/jhorton/clients/bcubed/coding/scala/play/rest/play-rest-api/build.sbt]:33: ';' expected but ')' found.
```
Ok. So I tried this a few times in a few ways and never could figure out exactly what was wrong. In the version in my github
they are just plain lines. Dont' forget the MySQL driver either.
```
/build.sbt
libraryDependencies += "com.typesafe.play" %% "play-slick" % "2.0.0"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0"
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.34"
```
And I ran update again to make sure things installed
```
>./sbt update
```

Now for the configuration file to enable the database
```
/conf/application.conf
slick.dbs.default.driver = "slick.driver.MySQLDriver$"
slick.dbs.default.db.driver = "com.mysql.jdbc.Driver"
slick.dbs.default.db.url = "jdbc:mysql://localhost/already-created-database"
slick.dbs.default.db.user = "user"
slick.dbs.default.db.password = "password"
```

Ok, Let's start it again
```
>./sbt run
```

Now this worked perfectly for me. Everything here should run exactly as before. 

Time to add a migration to create a table in the database. According to the docs
a new directory with database name is needed. We are using default above so I'll do that here too.
The migrations(evolutions) are named 1,2,3,4.sql
```
/conf/evolutions/default/

1.sql

# Posts schema
# --- !Ups
CREATE TABLE Post (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    title varchar(255) NOT NULL,
    body varchar(255) NOT NULL,
    PRIMARY KEY (id)
);
# --- !Downs
DROP TABLE Post;
```
These files seem to be plain SQL commands appropriate for your needs. There seems to be a lot of 
confusion around evolutions. 

play.evolutions.db.default.autoApply=true

autoApply - Whether evolutions should be automatically applied. In dev mode, this will cause both ups and downs evolutions to be automatically applied. In prod mode, it will cause only ups evolutions to be automatically applied. Defaults to false.

autoApplyDowns - Whether down evolutions should be automatically applied. In prod mode, this will cause down evolutions to be automatically applied. Has no effect in dev mode. Defaults to false.
For example, to enable autoApply for all evolutions, you might set play.evolutions.autoApply=true in application.conf or in a system property. To disable autocommit for a datasource named default, you set play.evolutions.db.default.autocommit=false.

Evolutions appear to be only imagined as go forward with no rollback mechanism.

The general premise seems to be use them in Dev

Set play.evolutions.enabled=false in Production and let an Administrator add/change the tables.

With the 1.sql file in place if you didn't exit the ./sbt run from earlier when you hit the http://localhost:9000/
you should get 
```
An SQL script will be run on your database - [Apply this script now!]
```
Click it and the default index should load here.

Now I want to change the existing classes to interact with our database and it's Posts instead

I moved the existing /v1/post/PostRepository.scala to /v1/post/PostRepositoryDB.scala and started making changes.

Time to add the new imports 
```
/v1/post/PostRepositoryDB.scala
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.MySQLDriver.api._   
import slick.driver.JdbcProfile
```
And started changing bits
Replaced
```
class PostRepositoryImpl @Inject() extends PostRepository {

class PostRepositoryImplDB @Inject()(dbConfigProvider: DatabaseConfigProvider) extends PostRepositoryDB {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
```
This just changes our classnames and gets our Database in and available. Also changed
```
/v1/post/PostResourceHandler.scala
postRepository: PostRepository)(implicit ec: ExecutionContext) {

postRepository: PostRepositoryDB)(implicit ec: ExecutionContext) {
```
And a module binding change was needed too. This is a helper to inject components from the system.
```
/Module.scala
bind[PostRepository].to[PostRepositoryImpl].in[Singleton]

bind[PostRepositoryDB].to[PostRepositoryImplDB].in[Singleton]
```

After all these changes everything should still compile and run exactly as before with the fake data.
We haven't really changed anything but names yet and added some new imports and drivers.

Let's try and connect everything to the real database now
```
/v1/post/PostRepositoryDB.scala
  private val Posts = TableQuery[PostTable]

  class PostTable(tag: Tag) extends Table[PostData](tag, "Post") {  
      def id = column[Int]("id", O.PrimaryKey,O.AutoInc)       // An AutoInc id. this is O not zero btw.
      def title = column[String]("title")
      def body = column[String]("body")

      override def * =                                         // Makes these fields available 
        (id, title, body) <>(PostData.tupled, PostData.unapply)
  }
```
(tag, “Post”) must match the case of your table name in the real DB.

I didn't see a need for PostId so I changed all references that used 'id: PostId' to 'id: Int'
throughout the files
```
final case class PostData(id: Int, title: String, body: String)
```

Still in the same file, try using our db handle.
```
/v1/post/PostRepositoryDB.scala
 override def list(): Future[Seq[PostData]] = {
      dbConfig.db.run(Posts.result)
  }
```

Keep changing the references needed and get it compiling again, here to 'id: Int'
```
/v1/post/PostResourceHandler.scala
  def lookup(id: Int): Future[Option[PostResource]] = {
    val postFuture = postRepository.get(id)

and find and change this in def create()
    val data = PostData(999, postInput.title, postInput.body)
```
In the router too
```
/v1/post/PostRouter.scala
def link(id: Int): String = {

and 

case GET(p"/${int(id)}") =>             // this ${int(id)} form only matches an INT in the request
```
And one more
```
/v1/post/PostController.scala
def show(id: Int): Action[AnyContent] = {
```

After all of that, hitting the http://localhost:9000/v1/posts/ url now gives me the fantastic response
```
[]
```
That isn't a typo. We have gone to our database and found, nothing, as expected.

Now to update for a single ID query
```
/v1/post/PostRepositoryDB.scala
  override def get(id: Int): Future[Option[PostData]] = {
    //Future.successful {
    //  logger.trace(s"get: id = $id")
    //  postList.find(post => post.id == id)
    //  }
    val action = Posts.filter(_.id === id).result.headOption    // headOption is better at empties.
    dbConfig.db.run(action)
  }
```
Now the url http://localhost:9000/v1/posts/1 spits out 
```
null 
```
A note about the === comparison from the docs
```
Most operators mimic the plain Scala equivalents, but you have to use === instead of == for comparing two values for equality and =!= instead of != for inequality. This is necessary because these operators are already defined (with unsuitable types and semantics) on the base type Any, so they cannot be replaced by extension methods.
```

Now to save our own posts
```
/v1/post/PostResourceHandler.scala
 postRepository.create(data).map { id =>
      createPostResource(data)
      PostResource(id.toString, routerProvider.get.link(id), data.title, data.body)
    }
  }
```
Update the repo to respond with newly created ID's
```
/v1/post/PostRepositoryDB.scala
  def create(data: PostData): Future[Int] = {
    // Future.successful {
    //   logger.trace(s"create: data = $data")
    //   data.id
    // }

    val action = (Posts returning Posts.map(_.id)) += data
    dbConfig.db.run( action2 )
```

And a brand new Update method
```
/v1/post/PostResourceHandler.scala
def update(id: Int, postInput: PostFormInput): Future[PostResource] = {
    val data = PostData( id, postInput.title, postInput.body)
    postRepository.update(data).map { id =>
      createPostResource(data)
      PostResource(data.id.toString, routerProvider.get.link(data.id), data.title, data.body)
    }
  }
```

And the route for it
```
/v1/post/PostRouter.scala
    case POST(p"/${int(id)}") =>
      controller.update(id)
```

Changes to the controller to handle an inbound ID
```
/v1/post/PostController.scala
case class PostFormInput(id: Option[Int], title: String, body: String)	  //Option[Int]
    Form(
      mapping(
        "id"  -> optional(number),		                                  //optional number..
        "title" -> nonEmptyText,
        "body" -> text
      )(PostFormInput.apply)(PostFormInput.unapply)
    )
  }

and

  def update(id: Int): Action[AnyContent] = {
    action.async { implicit request =>
      updateJsonPost(id)
    }
  }

and

private def updateJsonPost[A](id:Int)(
    implicit request: PostRequest[A]): Future[Result] = {
    def failure(badForm: Form[PostFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: PostFormInput) = {
      handler.update(id, input).map { post =>
        Created(Json.toJson(post)).withHeaders(LOCATION -> post.link)
      }
    }

    form.bindFromRequest().fold(failure, success)
  }
```

That last bit of updateJsonPost could probably be worked around to go with the processJsonPost as the only actual 
change is in the success to call handler.update instead.

All these changes and what do we have? Hopefully it all still works for you. I tried very carefully to note each
step and change so with a bit of luck you will be in exactly the same place. Almost entirely everything you see 
here was copied directly from my editor and terminal windows in the order it happened.

New Posts
```
>curl --data "title=mytitle&body=mybody" http://localhost:9000/v1/posts
{"id":"1","link":"/v1/posts/1","title":"mytitle","body":"mybody"}
>curl --data "title=mytitle&body=mybody" http://localhost:9000/v1/posts
{"id":"2","link":"/v1/posts/2","title":"mytitle","body":"mybody"}
```

Updating?
```
>curl --data "title=mytitlafdfasdfe&body=mybodyadfasdfsdf" http://localhost:9000/v1/posts/2
>{"id":"2","link":"/v1/posts/1","title":"mytitlafdfasdfe","body":"mybodyadfasdfsdf"}
```

Fantastic.  To help you compare and contrast the github version of this code has only two commits.

First commit, a brand new empty copy of Play Rest Api [Play](https://github.com/playframework/play-rest-api)

Second commit, all of these changes at once to make comparing easier.
























