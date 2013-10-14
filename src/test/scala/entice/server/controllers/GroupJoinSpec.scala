/**
 * For copyright information see the LICENSE document.
 */
 
package entice.server.controllers

import entice.server._, Net._
import entice.server.test._
import entice.server.utils._
import entice.protocol._
import akka.actor._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.matchers._

import scala.language.postfixOps
import scala.concurrent.duration._


class GroupJoinSpec extends TestKit(ActorSystem(
    "group-join-spec", 
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
    val groupJoin = TestActorRef[GroupJoin]

    // given
    val clients = ClientRegistryExtension(system)
    val worlds = WorldRegistryExtension(system)
    

    override def afterAll  { TestKit.shutdownActorSystem(system) }
    

    "A group-join controller" must {


        "update invite and join-requests lists upon simple join request" in {
            // given
            val session = TestProbe()
            val e1, e2 = Entity(UUID())
            val r1 = worlds.default.use(e1, new TypedSet() + GroupLeader())
            val r2 = worlds.default.use(e2, new TypedSet() + GroupLeader())
            val client = Client(session.ref, null, null, worlds.default, Some(r1), Playing)
            clients.add(client)

            // when
            fakePub(groupJoin, session.ref, GroupMergeRequest(e2))
            session.expectNoMsg

            // must
            within(3 seconds) {
                r1[GroupLeader].invited must be(List(e2))
                r2[GroupLeader].joinRequests must be(List(e1))
            }
        }


        "update member/leader upon accepting a simple join request" in {
            // given
            val session = TestProbe()
            val e1, e2 = Entity(UUID())
            // e2 has invited us (e1) at some point...
            val r1 = worlds.default.use(e1, new TypedSet() + GroupLeader(joinRequests = List(e2)))
            val r2 = worlds.default.use(e2, new TypedSet() + GroupLeader(invited = List(e1)))
            val client = Client(session.ref, null, null, worlds.default, Some(r1), Playing)
            clients.add(client)

            // when
            fakePub(groupJoin, session.ref, GroupMergeRequest(e2))
            session.expectNoMsg

            // must
            within(3 seconds) {
                r1.get[GroupLeader] must be(None)
                r1[GroupMember].leader must be(e2)

                r2[GroupLeader].members must be(List(e1))
                r2[GroupLeader].invited must be(Nil)
            }
        }


        "update member-lists/leader upon accepting advanced join request" in {
            // given
            val session = TestProbe()
            val e1, e2 = Entity(UUID())
            val m11, m12, m21, m22 = Entity(UUID())
             // e2 has invited us (e1) at some point... (but we already have two groups)
            val r1 = worlds.default.use(e1, new TypedSet() + GroupLeader(members = List(m11, m12), joinRequests = List(e2)))
            val r2 = worlds.default.use(e2, new TypedSet() + GroupLeader(members = List(m21, m22), invited = List(e1)))
            val rm11 = worlds.default.use(m11, new TypedSet() + GroupMember(e1))
            val rm12 = worlds.default.use(m12, new TypedSet() + GroupMember(e1))
            val rm21 = worlds.default.use(m21, new TypedSet() + GroupMember(e2))
            val rm22 = worlds.default.use(m22, new TypedSet() + GroupMember(e2))
            val client = Client(session.ref, null, null, worlds.default, Some(r1), Playing)
            clients.add(client)

            // when
            fakePub(groupJoin, session.ref, GroupMergeRequest(e2))
            session.expectNoMsg

            // must
            within(3 seconds) {
                // me is now a member
                r1.get[GroupLeader] must be(None)
                r1[GroupMember].leader must be(e2)
                // the other leader now has me and my members as members
                r2[GroupLeader].members must be(List(m21, m22, e1, m11, m12))
                r2[GroupLeader].invited must be(Nil)
                // new leader for my former members
                rm11[GroupMember].leader must be(e2)
                rm12[GroupMember].leader must be(e2)
            }
        }
    }
}