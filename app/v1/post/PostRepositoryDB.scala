package v1.post

import javax.inject.{Inject, Singleton}

import scala.concurrent.Future

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.MySQLDriver.api._   
import slick.driver.JdbcProfile

final case class PostData(id: Int, title: String, body: String)


/**
  * A pure non-blocking interface for the PostRepository.
  */
trait PostRepositoryDB {
  def create(data: PostData): Future[Int]

  def list(): Future[Iterable[PostData]]

  def get(id: Int): Future[Option[PostData]]

  def update(data: PostData): Future[Int]
}


@Singleton
class PostRepositoryImplDB @Inject()(dbConfigProvider: DatabaseConfigProvider) extends PostRepositoryDB {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  private val postList = List(
    PostData(1, "title 1", "blog post 1"),
    PostData(2, "title 2", "blog post 2"),
    PostData(3, "title 3", "blog post 3"),
    PostData(4, "title 4", "blog post 4"),
    PostData(5, "title 5", "blog post 5")
  )

  private val Posts = TableQuery[PostTable]

  class PostTable(tag: Tag) extends Table[PostData](tag, "Post") {
      def id = column[Int]("id", O.PrimaryKey,O.AutoInc)       // an autoinc id this is O not zero btw.
      def title = column[String]("title")
      def body = column[String]("body")

      // Looks like this gives the PostTable class these properties.
      override def * =
        (id, title, body) <>(PostData.tupled, PostData.unapply)
  }


  override def list(): Future[Seq[PostData]] = {
    Future.successful {
      logger.trace(s"list: ")
      postList
    }
    //dbConfig.db.run(Posts.result)
  }

  override def get(id: Int): Future[Option[PostData]] = {
    //Future.successful {
    //  logger.trace(s"get: id = $id")
    //  postList.find(post => post.id == id)
    //  }
    
    val action = Posts.filter(_.id === id).result.headOption
    dbConfig.db.run(action)

  }

  def create(data: PostData): Future[Int] = {
    // Future.successful {
    //   logger.trace(s"create: data = $data")
    //   data.id
    // }

    val action = (Posts returning Posts.map(_.id)) += data
    dbConfig.db.run( action )
  }

  def update(data: PostData): Future[Int] = {

      val action = Posts.filter(_.id === data.id).update( data )
      dbConfig.db.run(action) 
  }

}
