
import io.temporal.activity.ActivityOptions
import io.temporal.workflow.{Workflow, WorkflowInterface, WorkflowMethod}

import java.time.Duration

@WorkflowInterface
trait YahooWorkflow {
  @WorkflowMethod
  def process(token: String): String
}

class YahooWorkflowImpl extends YahooWorkflow {
  private val yahooActivities = Workflow.newActivityStub(
    classOf[YahooActivities],
    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)).build()
  )

  private val kafkaActivities = Workflow.newActivityStub(
    classOf[KafkaActivities],
    ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)).build()
  )

  override def process(token: String): String = {
    val nodeList = yahooActivities.retrieveLast7DaysDataFromYahoo(token)
    kafkaActivities.produceBatchNode(token, nodeList)
    "🚀"
  }
}
