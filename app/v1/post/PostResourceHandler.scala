package v1.post

import javax.inject.{Inject, Provider}

import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json._

/**
  * DTO for displaying post information.
  */
case class PostResource(id: String, link: String, title: String, body: String)

object PostResource {

  /**
    * Mapping to write a PostResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[PostResource] {
    def writes(post: PostResource): JsValue = {
      Json.obj(
        "id" -> post.id,
        "link" -> post.link,
        "title" -> post.title,
        "body" -> post.body
      )
    }
  }
}

/**
  * Controls access to the backend data, returning [[PostResource]]
  */
class PostResourceHandler @Inject()(
    routerProvider: Provider[PostRouter],
    postRepository: PostRepositoryDB)(implicit ec: ExecutionContext) {

  def create(postInput: PostFormInput): Future[PostResource] = {
    val data = PostData(999, postInput.title, postInput.body)
    
    postRepository.create(data).map { id =>
      createPostResource(data)
      PostResource(id.toString, routerProvider.get.link(id), data.title, data.body)
    }
  }

  def lookup(id: Int): Future[Option[PostResource]] = {
    val postFuture = postRepository.get(id)
    postFuture.map { maybePostData =>
      maybePostData.map { postData =>
        createPostResource(postData)
      }
    }
  }

  def find: Future[Iterable[PostResource]] = {
    postRepository.list().map { postDataList =>
      postDataList.map(postData => createPostResource(postData))
    }
  }

  private def createPostResource(p: PostData): PostResource = {
    PostResource(p.id.toString, routerProvider.get.link(p.id), p.title, p.body)
  }

def update(id: Int, postInput: PostFormInput): Future[PostResource] = {
    val data = PostData( id, postInput.title, postInput.body)
    
    postRepository.update(data).map { id =>
      createPostResource(data)
      PostResource(data.id.toString, routerProvider.get.link(data.id), data.title, data.body)
    }
  }

}
