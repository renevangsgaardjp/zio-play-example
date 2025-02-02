package models

import zio.ZLayer
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.ZIOSpecDefault

object UserRepositorySpec extends ZIOSpecDefault {
  def spec = suite("UserRepository")(
    test("test get user") {
      for {
        user <- UserRepository.getUser
      } yield assert(user)(equalTo("USER! from localhost:666"))
    }.provideCustomLayer(testDatabase >>> UserRepository.live)
  )

  val testDatabase = ZLayer.succeed(new Database {
    override def connectionString: String = "localhost:666"
  })
}
