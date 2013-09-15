/**
 * For copyright information see the LICENSE document.
 */

package entice.server.login

import entice.server._
import entice.server.utils._
import entice.protocol.utils.MessageBus._

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }

import java.net.InetSocketAddress


/**
 * Delicious cake slice ;) - Replaces default API slice
 * Implements the whole login-server functionality.
 */
trait LoginApiSlice extends CoreSlice with ApiSlice {

    lazy val clientRegistry = new Registry[Client]

    // handler actors
    val loginHandler = actorSystem.actorOf(Props(classOf[LoginHandler], reactor, clientRegistry), "login")
    val dispatchHandler = actorSystem.actorOf(Props(classOf[DispatchHandler], serverActor, reactor, clientRegistry), "dispatch")
}


/**
 * Understands the LS2GS communication protocol.
 */
case class LoginServer(system: ActorSystem, port: Int) extends Actor 
    with CoreSlice 
    with LoginApiSlice 
    with NetSlice 
    with ActorSlice {

    import entice.server.ReactorActor._


    override lazy val actorSystem = system
    override lazy val localAddress = new InetSocketAddress(port)


    override def receive = super.receive orElse {
        case msg: GS2LS =>
            val gameSrv = sender
            reactor ! Publish(Sender(actor = gameSrv), msg)
    }
}