package controllers

import java.text.SimpleDateFormat
import java.util.Date

import scala.concurrent.duration.DurationInt
import scala.util.Random

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import play.api.libs.EventSource
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Action
import play.api.mvc.Controller

object Application extends Controller {

  class HelloActor(myName: String) extends Actor {
	  def receive = {
	    case "hello" => println("hello from %s".format(myName))
	    case _       => println("huh?, said %s".format(myName))
	  }
  }
  
    /** 
   * A String Enumerator producing a formatted Time message every 500 millis.
   * A callback enumerator is pure an can be applied on several Iteratee.
   */
  lazy val clock: Enumerator[String] = {
    
    import java.text._
    
    val dateFormat = new SimpleDateFormat("HH mm ss")
    
    Enumerator.generateM[String] {
//    	Promise.timeout(Some(dateFormat.format(new Date)), 500 milliseconds)
    	Promise.timeout(Some(Random.nextInt(100) + ""), 500 milliseconds)
    }
  }
  
  
  
  // Define a generic event,
	  trait ZapEvent {
		  def event: String
 
		  // event is a specific Server sent event attribute
		  def price: String // a price holds the currency
	  }
	  
	  // This is an airfare price update
	case class AirfareMessage(price: String) extends ZapEvent {
		override def event = "airfare"
	}
	
	// Defines the Enumerator for various kind of ZapEvent
	object Streams {	
	  val airfareStream: Enumerator[ZapEvent] = Enumerator.generateM[ZapEvent] {
		  Promise.timeout(Some(AirfareMessage(Random.nextInt(500) + 100 + " EUR")), Random.nextInt(3000))
	  }
	}

  def index = Action {
    
    // Actor test from http://alvinalexander.com/scala/simple-scala-akka-actor-examples-hello-world-actors
     val system = ActorSystem("HelloSystem")
     // default Actor constructor
     val helloActor = system.actorOf(Props(new HelloActor("Tryggve")), name = "helloactor")
     helloActor ! "hello"
     helloActor ! "buenos dias"
    
    Ok(views.html.index("Ants!"))
  }

    def stream = Action {
    	Ok.chunked(clock &> EventSource()).as("text/event-stream")
    }

}