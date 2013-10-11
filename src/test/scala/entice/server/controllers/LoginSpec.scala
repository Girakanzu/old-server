/**
 * For copyright information see the LICENSE document.
 */
 
package entice.server.controllers

import entice.server._
import entice.server.test._
import entice.server.utils._
import entice.server.database._
import entice.protocol._
import akka.actor._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.matchers._


class LoginSpec extends TestKit(ActorSystem(
    "login-spec", 
    config = ConfigFactory.parseString("""
        akka {
          loglevel = WARNING
        }
    """)))

    with WordSpec
    with MustMatchers 
    with BeforeAndAfterAll
    with OneInstancePerTest
    with ImplicitSender {


    // actor under test
    val login = TestActorRef[Login]

    // given
    val acc = Account(email = "loginspec@entice.org", password = "test")
    val noacc = Account(email = "nonexisting@entice.org", password = "test")
    val char1 = Character(accountId = acc.id, name = Name("login-spec-char1"))
    val char2 = Character(accountId = acc.id, name = Name("login-spec-char2"))


    override def beforeAll { 
        // given an existing acc
        Account.create(acc)
        Character.create(char1)
        Character.create(char2)

        // given a nonexisting acc
        Account.create(noacc)
        Account.delete(noacc)
    }

    override def afterAll {
        Account.delete(acc)
        Character.delete(char1)
        Character.delete(char2)

        TestKit.shutdownActorSystem(system)
    }


    "A login controller" must {       


        "accept clients with a valid login request, and reply with a login success" in {
            val probe = TestProbe()
            fakePub(login, probe.ref, LoginRequest("loginspec@entice.org", "test"))
            probe.expectMsgClass(classOf[LoginSuccess])
            probe.expectNoMsg
        }


        "reply to any invalid login requests with an error code" in {
            val probe = TestProbe()
            fakePub(login, probe.ref, LoginRequest("nonexisting@entice.org", "test"))
            probe.expectMsgPF() {
                case LoginFail(errorMsg) if errorMsg != "" => true
            }
            probe.expectNoMsg
        }

    }
}