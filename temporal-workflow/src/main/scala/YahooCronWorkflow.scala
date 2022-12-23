import io.temporal.activity.ActivityOptions
import io.temporal.workflow.{Workflow, WorkflowInterface, WorkflowMethod}

import java.time.Duration

@WorkflowInterface
trait YahooCronWorkflow {
  @WorkflowMethod
  def process(token: String): String
}

class YahooCronWorkflowImpl extends YahooCronWorkflow {
  private val yahooActivities = Workflow.newActivityStub(
    classOf[YahooActivities],
    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(1)).build()
  )

  private val kafkaActivities = Workflow.newActivityStub(
    classOf[KafkaActivities],
    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)).build()
  )

  override def process(token: String): String = {
    val node = yahooActivities.retrieveDataFromYahoo(token)

    kafkaActivities.produceNode(token, node)
    "🚀"
  }
}
