/**
 * For copyright information see the LICENSE document.
 */
 
package entice.server.controllers

import entice.server._
import entice.server.test._
import entice.server.utils._
import entice.server.world._
import entice.server.world.systems._
import entice.protocol._
import akka.actor._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.matchers._


class ChatSpec extends TestKit(ActorSystem(
    "chat-spec", 
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


    // actors under test
    val chat = TestActorRef[PreChat]
    val chatSys = TestActorRef[ChatSystem]

    // given
    val clients  = ClientRegistryExtension(system)
    val worlds = WorldRegistryExtension(system)


    override def afterAll  { TestKit.shutdownActorSystem(system) }


    "The chat system" must {


        "propagate a chat messages (clients need to be playing)" in {
            // given a few sessions with their client objs and entities
            val session1 = TestProbe(); 
            val session2 = TestProbe(); 
            val session3 = TestProbe(); 

            val client1 = Client(session1.ref, null, null, worlds.default, state = Playing)
            val client2 = Client(session2.ref, null, null, worlds.default, state = Playing)
            val client3 = Client(session3.ref, null, null, worlds.default, state = Playing)

            val world = worlds.default

            val ent1 = world.create(new TypedSet[Component]() + Name("chatspecname1")); client1.entity = Some(ent1)
            val ent2 = world.create(new TypedSet[Component]() + Name("chatspecname2")); client2.entity = Some(ent2)
            val ent3 = world.create(new TypedSet[Component]() + Name("chatspecname3")); client3.entity = Some(ent3)

            clients.add(client1)
            clients.add(client2)
            clients.add(client3)

            // chat messages
            fakePub(chat, session1.ref, ChatMessage(ent1, "hi"))
            session1.expectMsg(ChatMessage(ent1, "hi"))
            session2.expectMsg(ChatMessage(ent1, "hi"))
            session3.expectMsg(ChatMessage(ent1, "hi"))

            fakePub(chat, session2.ref, ChatMessage(ent2, "hi"))
            session1.expectMsg(ChatMessage(ent2, "hi"))
            session2.expectMsg(ChatMessage(ent2, "hi"))
            session3.expectMsg(ChatMessage(ent2, "hi"))

            fakePub(chat, session3.ref, ChatMessage(ent3, "hi"))
            session1.expectMsg(ChatMessage(ent3, "hi"))
            session2.expectMsg(ChatMessage(ent3, "hi"))
            session3.expectMsg(ChatMessage(ent3, "hi"))
        }
    }
}