package requester

import akka.stream.Materializer
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, Encoder, Encoders, SparkSession}
import requester.grpc._

import scala.concurrent.Future

class SparkRequester extends Serializable {
  val spark: SparkSession = SparkSession
    .builder()
    .master("local[*]")
    .appName("trading_view_requester")
    .getOrCreate()


  private def getData: DataFrame = {
    import spark.implicits._

    val objectMapper = new ObjectMapper()

    spark.read.format("parquet").load("/tmp/trading_view/").map { row =>
      val res = objectMapper.readTree(row(1).toString)

      (res.get("symbol").asText(),
        res.get("timestamp").asLong(),
        res.get("price").asDouble(),
        res.get("open").asDouble(),
        res.get("high").asDouble(),
        res.get("low").asDouble())
    }.toDF("symbol", "timestamp", "price", "open", "high", "low")
  }

  def getSymbols: Seq[String] = {
    import spark.implicits._

    getData.select("symbol").as[String].distinct().collect().toSeq
  }

  def getTimeSeries(symbol: String): Seq[TimeSeries] = {
    implicit val encoder: Encoder[TimeSeries] = Encoders.kryo(classOf[TimeSeries])

    getData.where(col("symbol") === symbol).where(col("price") !== 0).distinct().orderBy("timestamp").map { e =>

      TimeSeries(e.getLong(1), e.getDouble(2), e.getDouble(3), e.getDouble(4), e.getDouble(5))
    }.collect().toSeq
  }

}

class RequesterServiceImpl(implicit mat: Materializer) extends RequesterService {
  import mat.executionContext

  val sparkRequester: SparkRequester = new SparkRequester()

  override def requestListSymbols(in: RequestListSymbolsRequest): Future[RequestListSymbolReplay] = {
    Future.successful(RequestListSymbolReplay(sparkRequester.getSymbols))
  }

  override def requestTimeSeries(in: RequesterRequest): Future[RequesterReply] = {
    Future.successful(RequesterReply(sparkRequester.getTimeSeries(in.symbol)))
  }
}
