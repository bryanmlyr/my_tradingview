import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.client.{WorkflowClient, WorkflowOptions}
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory

import java.time.Duration

class JobCronInstance(token: String) {
  private val service = WorkflowServiceStubs.newLocalServiceStubs
  private val client = WorkflowClient.newInstance(service)
  private val factory = WorkerFactory.newInstance(client)

  private val worker = factory.newWorker(s"CRON-$token")

  worker.registerWorkflowImplementationTypes(classOf[YahooCronWorkflowImpl])
  worker.registerActivitiesImplementations(new YahooActivitiesImpl(), new KafkaActivitiesImpl())

  factory.start()

  private val workflowOptions = WorkflowOptions.newBuilder
    .setWorkflowId(s"yahoo-cron-$token")
    .setTaskQueue(s"CRON-$token")
    .setCronSchedule("* * * * *")
    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
  .build

  val workflow: YahooCronWorkflow = client.newWorkflowStub(
    classOf[YahooCronWorkflow],
    workflowOptions
  )

  WorkflowClient.start(() => {
    workflow.process(token)
  })
}

class JobInstance(token: String) {
  private val service = WorkflowServiceStubs.newLocalServiceStubs
  private val client = WorkflowClient.newInstance(service)
  private val factory = WorkerFactory.newInstance(client)

  private val worker = factory.newWorker(s"JOB-$token")

  worker.registerWorkflowImplementationTypes(classOf[YahooWorkflowImpl])
  worker.registerActivitiesImplementations(new YahooActivitiesImpl(), new KafkaActivitiesImpl())

  factory.start()

  private val workflowOptions = WorkflowOptions.newBuilder
    .setWorkflowId(s"yahoo-job-$token")
    .setTaskQueue(s"JOB-$token")
    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE)
    .setWorkflowExecutionTimeout(Duration.ofMinutes(60))
    .setWorkflowRunTimeout(Duration.ofMinutes(60)).build

  val workflow: YahooWorkflow = client.newWorkflowStub(
    classOf[YahooWorkflow],
    workflowOptions
  )

  WorkflowClient.start(() => {
    workflow.process(token)
  })
}

object Starter {
  def main(args: Array[String]): Unit = {
    new JobInstance("SHOP")
    new JobCronInstance("SHOP")

    new JobInstance("NFLX")
    new JobCronInstance("NFLX")

    new JobInstance("NET")
    new JobCronInstance("NET")

    new JobInstance("DDOG")
    new JobCronInstance("DDOG")

    new JobInstance("META")
    new JobCronInstance("META")
  }
}
