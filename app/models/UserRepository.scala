package models
import play.api.Application
import zio._

import javax.inject.{Inject, Singleton}

// Controller
// UserService @Inject(ZioComponents)

object UserRepository {
  type UserRepository = Service

  trait Service {
    def getUser: Task[String]
  }

  def getUser: ZIO[UserRepository, Throwable, String] = ZIO.environmentWithZIO[UserRepository](_.get.getUser)

  def testDatabase: ULayer[Database] = ???

  def application(app: Application): ULayer[Application] =
    ZLayer.succeed(app)

  def realDatabase: ZLayer[Application, Nothing, Database] = {
    ZIO.service[Application].map(application => application.injector.instanceOf(classOf[Database])).toLayer
  }

  val live: ZLayer[Database, Nothing, UserRepository] =
    ZIO.service[Database].map(database => UserRepositoryLive(database)).toLayer
}

case class UserRepositoryLive(database: Database) extends UserRepository.Service {
  override def getUser: Task[String] =
    Task { s"USER! from ${database.connectionString}" }
}

trait Database {
  def connectionString: String
}

@Singleton
class DatabaseImpl @Inject() extends Database {
  override def connectionString: String = "localhost:5432"
}
