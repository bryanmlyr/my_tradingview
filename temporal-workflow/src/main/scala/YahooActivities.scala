import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal, Unmarshaller}
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import io.temporal.activity.{ActivityInterface, ActivityMethod}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.collection.convert.ImplicitConversions.`iterator asScala`
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@ActivityInterface
trait YahooActivities {
  @ActivityMethod
  def retrieveDataFromYahoo(token: String): JsonNode

  @ActivityMethod
  def retrieveLast7DaysDataFromYahoo(token: String): JsonNode
}

class YahooActivitiesImpl extends YahooActivities {
  private val mapper = new ObjectMapper()

  implicit val system: ActorSystem = ActorSystem()
  implicit val jsonUnmarshaller: FromEntityUnmarshaller[JsonNode] =
    Unmarshaller.stringUnmarshaller.map { data =>
      mapper.readTree(data)
    }

  private def generateObjectNode(token: String, element: ((Long, Double, Double, Double), Double)): JsonNode = {
    val obj = mapper.createObjectNode()

    obj.put("symbol", token)
    obj.put("timestamp", element._1._1)
    obj.put("price", element._1._2)
    obj.put("open", element._1._3)
    obj.put("high", element._1._4)
    obj.put("low", element._2)

    obj
  }

  private def requestDataFromYahoo(token: String, from: Long, to: Long) = {
    val url = s"https://query1.finance.yahoo.com/v8/finance/chart/$token?interval=1m&period1=$from&period2=$to&events=history"
    println(url)

    val httpRes = Await.result(Http().singleRequest(HttpRequest(uri = url)), 10.seconds)
    val jsonNode = Await.result(Unmarshal(httpRes.entity).to[JsonNode], 10.seconds)

    val resultObject = jsonNode.get("chart").get("result").get(0)
    val timestamps = resultObject.get("timestamp").asInstanceOf[ArrayNode].elements().map(_.asLong()).toList

    val quote = resultObject.get("indicators").get("quote").get(0)
    val closes = quote.get("close").asInstanceOf[ArrayNode].elements().map(_.asDouble()).toList
    val opens = quote.get("open").asInstanceOf[ArrayNode].elements().map(_.asDouble()).toList
    val highs = quote.get("high").asInstanceOf[ArrayNode].elements().map(_.asDouble()).toList
    val lows = quote.get("low").asInstanceOf[ArrayNode].elements().map(_.asDouble()).toList

    timestamps.lazyZip(closes).lazyZip(opens).lazyZip(highs).lazyZip(lows).toList
  }

  override def retrieveDataFromYahoo(token: String): JsonNode = {
    val to = Instant.now.truncatedTo(ChronoUnit.MINUTES).getEpochSecond - 60
    val from = to - 60
    val res = requestDataFromYahoo(token, from, to).last
    val obj = generateObjectNode(token, res)
    println(from, to, token)
    println(obj)

    obj
  }

  override def retrieveLast7DaysDataFromYahoo(token: String): JsonNode = {
    val now = Instant.now.getEpochSecond
    val last7days = now - 60L*60L*24L*7L

    val z = requestDataFromYahoo(token, last7days, now)

    val obj = mapper.createObjectNode()
    val arrayNode = obj.putArray("results")

    z.foreach { element =>
      val obj = generateObjectNode(token, element)

      arrayNode.add(obj)
    }

    arrayNode
  }
}
