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
import play.api.libs.iteratee.Enumeratee
import play.api.libs.json.JsValue
import play.api.libs.json.Json._

object Application extends Controller {

  class HelloActor(myName: String) extends Actor {
	  def receive = {
	    case "hello" => println("hello from %s".format(myName))
	    case _       => println("huh?, said %s".format(myName))
	  }
  }
  
  
  def randomIntegersString: Some[String] = {
    
    val rString = (Random.nextInt(100) - 50) + ";" + (Random.nextInt(100) - 50)
      
    Some(rString)
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
//    	Promise.timeout(Some((Random.nextInt(100) - 50) + ";" + (Random.nextInt(100) - 50)), 500 milliseconds)
    	Promise.timeout(randomIntegersString, 500 milliseconds)
    }
  }
  
  lazy val clock2: Enumerator[String] = {
    
    import java.text._
    
    val dateFormat = new SimpleDateFormat("HH mm ss")
    
    Enumerator.generateM[String] {
//    	Promise.timeout(Some(dateFormat.format(new Date)), 500 milliseconds)
    	Promise.timeout(Some((Random.nextInt(100) + 100) + ";" + (Random.nextInt(150) * -1)), 900 milliseconds)
    }
  }
  
	val asJson: Enumeratee[String, JsValue] = Enumeratee.map[String] { 
	    case s : String => toJson(Map("verdi1" -> toJson(s), "verdi2" -> toJson(s)))
	    case _  => toJson(Map("verdi1" -> toJson("NA"), "verdi2" -> toJson("NA")))
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
      // >- Alias for interleave
      // &> Compose this Enumerator with an Enumeratee. Alias for through
//    	Ok.chunked((clock >- clock2) &> asJson ><> EventSource()).as("text/event-stream")
    	Ok.chunked((clock >- clock2).through(asJson).through(EventSource())).as("text/event-stream")
    }

    //     	(request.body \ "np:TimeSeries" headOption).map(_.text).map { name =>
    // http://bcomposes.wordpress.com/2012/05/04/basic-xml-processing-with-scala/
    // http://www.authorcode.com/text-animation-in-html5/
    // http://bcomposes.wordpress.com/2012/05/12/processing-json-in-scala-with-jerkson/
//    def feedXmlPost = Action(parse.xml) { request =>
//    	(request.body \ "np:TimeSeries" headOption).map(_.text).map { name =>
//    		Ok("Hello " + name)
//    	}
//    }
    
    
//    http://stackoverflow.com/questions/19241930/play-framework-parse-xml-to-model
//
//    import scala.xml.{Comment => _, _}
//
//
//	case class Comment(commentDate: String, commentText: String)
//	case class MyItem(id: Option[Long] = None, name: String, comments: List[Comment])
//	
//	object MyParser {
//	  def parse(el: Elem) =
//	    MyItem(Some((el \ "id").text.toLong), (el \ "name").text,
//	      (el \\ "comment") map { c => Comment((c \ "comment_date").text, (c \ "comment_text").text)} toList)

}
}