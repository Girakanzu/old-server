/**
 * For copyright information see the LICENSE document.
 */
 
package entice.server.controllers

import entice.server._, Net._
import entice.server.utils._
import entice.server.world._
import entice.protocol._
import akka.actor._
import akka.testkit._
import com.mongodb.casbah.commons.Imports._
import org.scalatest._
import org.scalatest.matchers._


class WorldDiffSpec(_system: ActorSystem) extends TestKit(_system)
    with WordSpec
    with MustMatchers 
    with BeforeAndAfterAll
    with ImplicitSender {

    def this() = this(ActorSystem("world-diff-spec"))

    def testPub(probe: ActorRef, msg: Typeable) { 
        MessageBusExtension(_system).publish(MessageEvent(probe, msg)) 
    }

    override def afterAll {
        TestKit.shutdownActorSystem(_system)
    }


    "A world-diff controller" must {

        val differ = _system.actorOf(Props[WorldDiff])
        val clients = ClientRegistryExtension(_system)
        val worlds = WorldRegistryExtension(_system)


        "capture components that changed, and send them to playing clients" in {

            // given
            val session = TestProbe()
            val client = Client(session.ref, null, Map(), None, state = Playing)
            val entity = worlds.get(client).create(new TypedSet[Component]() + Name("world-diff-spec1"))
            client.entity = Some(entity)
            clients.add(client)

            // wait so we dont conflict with the min diff time
            Thread.sleep(50)

            // when changing and then ticking
            entity.set(Name("world-diff-spec2"))
            testPub(self, Tick())

            session.expectMsgPF() {
                case UpdateCommand(_, l1, l2, _)
                    if (l1.contains(EntityView(entity.entity, AllCompsView(List(Name("world-diff-spec2")))))
                    &&  l2.contains(entity.entity)) => true
            }
            session.expectNoMsg

            // wait so we dont conflict with the min diff time
            Thread.sleep(50)

            // when changing and then flushing
            entity.set(Name("world-diff-spec3"))
            testPub(self, Flush())

            session.expectMsgPF() {
                case UpdateCommand(_, l1, _, _)
                     if (l1.contains(EntityView(entity.entity, AllCompsView(List(Name("world-diff-spec3")))))) => true
            }
        }
    }
}