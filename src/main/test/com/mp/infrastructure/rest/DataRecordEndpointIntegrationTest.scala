package com.mp.infrastructure.rest
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.mp.domain.data_record.process_configuration.{DataRecordProcessConfig, DataRecordProcessConfigService}
import com.mp.domain.data_record.report.DataRecordReportFactory
import com.mp.domain.data_record.{DataRecord, DataRecordId, DataRecordService}
import com.mp.domain.shared.id.UuidFactory
import com.mp.domain.shared.{Amount, Name, PhoneNumber}
import com.mp.infrastructure.configuration.MongoTestContainer
import com.mp.infrastructure.configuration.http.HttpJsonUnmarshaller
import com.mp.infrastructure.configuration.mongo.MongoClient
import com.mp.infrastructure.persistence.data_record.DataRecordMongoRepository
import com.mp.infrastructure.persistence.data_record.process_configuration.DataRecordProcessConfigMongoRepository
import com.mp.infrastructure.rest.data_record.DataRecordEndpoint
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.syntax.EncoderOps
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import reactivemongo.api.bson.BSONDocument

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class DataRecordEndpointIntegrationTest
    extends AnyFunSuite
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll with BeforeAndAfterEach with HttpJsonUnmarshaller {
  import com.mp.infrastructure.persistence.data_record.DataRecordMongoDocs._
  import com.mp.infrastructure.rest.data_record.DataRecordApi._

  val mongoDataRecordCollectionName = "data_record"
  implicit val idFactory: UuidFactory = new UuidFactory()
  implicit val clock: Clock = Clock.system(ZoneId.of("Europe/Warsaw"))

  implicit val mongoClient: MongoClient = MongoTestContainer.getMongoClient
  implicit val dataRecordRepository: DataRecordMongoRepository = new DataRecordMongoRepository(mongoClient)
  implicit val dataRecordProcessConfigRepository: DataRecordProcessConfigMongoRepository = new DataRecordProcessConfigMongoRepository(mongoClient)
  implicit val dataRecordProcessConfigService: DataRecordProcessConfigService = new DataRecordProcessConfigService
  implicit val dataRecordService: DataRecordService = new DataRecordService
  implicit val dataRecordReportFactory: DataRecordReportFactory = new DataRecordReportFactory
  val routes: Route = new DataRecordEndpoint().route

  override def beforeAll(): Unit = {
    MongoTestContainer.start()
    initializeConfig()
  }

  override def beforeEach(): Unit = {
    val collection = Await.result(mongoClient.collection(mongoDataRecordCollectionName), 5.seconds)
    Await.result(collection.drop(failIfNotFound = false), 5.seconds)
  }

  override def afterAll(): Unit  = {
    MongoTestContainer.stop()
  }

  test("Create a new data record and verify MongoDB for ID and Name") {
    val createDataRecord = CreateDataRecordRequest(
      name = "John Doe",
      phoneNumber = "1234567890",
      amount = 100
    )

    val requestEntity = HttpEntity(
      ContentTypes.`application/json`,
      createDataRecord.asJson.noSpaces
    )

    Post("/api/data-records").withEntity(requestEntity) ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val responseJson = responseAs[CreateDataRecordResponse]

      val collection = Await.result(mongoClient.collection(mongoDataRecordCollectionName), 5.seconds)
      val records = Await.result(
        collection.find(BSONDocument("_id" -> responseJson.dataRecordId)).cursor[DataRecordDocument]().collect[List](),
        5.seconds
      )

      records.length shouldBe 1
      records.head.name shouldBe "John Doe"
      records.head.phoneNumber shouldBe "1234567890"
      records.head.amount shouldBe BigDecimal(100)
    }
  }

  test("Processing a record should return the processed according to requirements and update MongoDB") {
    val recordToProcessId = idFactory.generate()
    val records = Seq(
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("John Doe"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(9000.125)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Jane Doe"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(1491.50)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = Some(Instant.parse("2025-02-23T00:00:00Z"))
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("John Smith"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(3500.00)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Jane Smith"),
        phoneNumber = PhoneNumber("0987654321"),
        amount = Amount(BigDecimal(200.00)),
        createTime = Instant.parse("2025-02-23T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Adam Smith"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(1255.00)),
        createTime = Instant.parse("2025-02-23T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(recordToProcessId),
        name = Name("Joseph Smith"),
        phoneNumber = PhoneNumber("1234567999"),
        amount = Amount(BigDecimal(9520.00)),
        createTime = Instant.parse("2024-02-21T00:00:00Z"),
        processTime = None
      )
    )

    initializeData(records)
    dataRecordProcessConfigService.initializeCache()

    Get("/api/data-records") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val responseJson = responseAs[GetDataRecordResponse.ProcessedRecord]

      responseJson.name shouldBe "Joseph Smith"
      responseJson.phoneNumber shouldBe "1234567999"
      responseJson.amount shouldBe BigDecimal(9520.00)

      val collection = Await.result(mongoClient.collection(mongoDataRecordCollectionName), 5.seconds)
      val records = Await.result(
        collection.find(BSONDocument("_id" -> recordToProcessId)).cursor[DataRecordDocument]().collect[List](),
        5.seconds
      )

      records.length shouldBe 1
      records.head.processTime shouldBe defined
    }
  }

  test("Processing return empty response when no record matches criteria") {
    val records = Seq(
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("John Doe"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(9000.125)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Jane Doe"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(1491.50)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = Some(Instant.parse("2025-02-23T00:00:00Z"))
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("John Smith"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(3500.00)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Jane Smith"),
        phoneNumber = PhoneNumber("0987654321"),
        amount = Amount(BigDecimal(200.00)),
        createTime = Instant.parse("2025-02-23T00:00:00Z"),
        processTime = Some(Instant.parse("2025-02-24T00:00:00Z"))
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Adam Smith"),
        phoneNumber = PhoneNumber("0987654321"),
        amount = Amount(BigDecimal(1255.00)),
        createTime = Instant.parse("2025-02-23T00:00:00Z"),
        processTime = None
      ),
    )

    initializeData(records)

    Get("/api/data-records") ~> routes ~> check {
      status shouldBe StatusCodes.NoContent
    }
  }

  test("Get all records report without filtering") {
    val records = Seq(
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("John Doe"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(250)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Jane Doe"),
        phoneNumber = PhoneNumber("0987654321"),
        amount = Amount(BigDecimal(1000)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = Some(Instant.parse("2025-02-23T00:00:00Z"))
      )
    )

    initializeData(records)

    Get("/api/data-records/report") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val responseJson = responseAs[GetDataRecordReport]

      responseJson.entries.length shouldBe 2

      val johnRecord = responseJson.entries.find(_.phoneNumber == "1234567890").get
      johnRecord.records.head.name shouldBe "John Doe"
      johnRecord.records.head.content.amountsSum shouldBe BigDecimal(250)

      val janeRecord = responseJson.entries.find(_.phoneNumber == "0987654321").get
      janeRecord.records.head.name shouldBe "Jane Doe"
      janeRecord.records.head.content.amountsSum shouldBe BigDecimal(1000)
    }
  }

  test("Get report for only processed records") {
    val records = Seq(
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("John Doe"),
        phoneNumber = PhoneNumber("1234567890"),
        amount = Amount(BigDecimal(250)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = None
      ),
      DataRecord(
        id = DataRecordId(idFactory.generate()),
        name = Name("Jane Doe"),
        phoneNumber = PhoneNumber("0987654321"),
        amount = Amount(BigDecimal(1000)),
        createTime = Instant.parse("2025-02-22T00:00:00Z"),
        processTime = Some(Instant.parse("2025-02-23T00:00:00Z"))
      )
    )

    initializeData(records)

    Get("/api/data-records/report?onlyProcessedRecords=true") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      val responseJson = responseAs[GetDataRecordReport]

      responseJson.entries.length shouldBe 1

      val janeRecord = responseJson.entries.find(_.phoneNumber == "0987654321").get
      janeRecord.records.head.name shouldBe "Jane Doe"
      janeRecord.records.head.content.amountsSum shouldBe BigDecimal(1000)
    }
  }

  def initializeData(records: Seq[DataRecord]): Unit = {
    Future.sequence(records.map(dataRecordRepository.save)).map(_ => ()).onComplete {
      case Success(_)         => ()
      case Failure(exception) => throw exception
    }
  }

  private def initializeConfig(): Unit = {
    val defaultConfig = DataRecordProcessConfig(
      prioritiesByAmount = Set(
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(0), to = Some(BigDecimal(500))),
          priority = DataRecordProcessConfig.Priority(1)
        ),
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(501), to = Some(BigDecimal(3000))),
          priority = DataRecordProcessConfig.Priority(2)
        ),
        DataRecordProcessConfig.PriorityByAmountConfig(
          amountRange = DataRecordProcessConfig.AmountRange(from = BigDecimal(3001), to = None),
          priority = DataRecordProcessConfig.Priority(3)
        )
      )
    )

    dataRecordProcessConfigRepository.save(defaultConfig).onComplete {
      case Success(_) => ()
      case Failure(exception) => throw exception
    }
  }


}
