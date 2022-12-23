import org.apache.spark.sql._
import org.apache.spark.sql.streaming.OutputMode

object Main {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .master("local[*]")
      .appName("trading_view")
      .getOrCreate()

    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("startingOffsets", "earliest")
      .option("subscribe", "inbound")
      .load()
      .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")

    val outputStream = df
      .writeStream
      .format("parquet")
      .option("path", "/tmp/trading_view")
      .option("checkpointLocation", "/tmp/checkpoint_location/trading_view")
      .outputMode(OutputMode.Append())
      .start

    outputStream.awaitTermination()
  }
}
