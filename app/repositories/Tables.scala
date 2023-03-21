package repositories
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends Tables {
  val profile = slick.jdbc.PostgresProfile
}

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Student.schema ++ Times.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Student
   *  @param id Database column id SqlType(uuid), PrimaryKey
   *  @param email Database column email SqlType(text)
   *  @param name Database column name SqlType(text)
   *  @param student Database column student SqlType(text), Default(None)
   *  @param level Database column level SqlType(text), Default(None)
   *  @param phone Database column phone SqlType(text)
   *  @param notes Database column notes SqlType(text), Default(None)
   *  @param paymentIntentId Database column payment_intent_id SqlType(text), Default(None)
   *  @param totalCost Database column total_cost SqlType(numeric), Default(None)
   *  @param paymentConfirmed Database column payment_confirmed SqlType(bool), Default(None) */
  case class StudentRow(id: java.util.UUID, email: String, name: String, student: Option[String] = None, level: Option[String] = None, phone: String, notes: Option[String] = None, paymentIntentId: Option[String] = None, totalCost: Option[scala.math.BigDecimal] = None, paymentConfirmed: Option[Boolean] = None)
  /** GetResult implicit for fetching StudentRow objects using plain SQL queries */
  implicit def GetResultStudentRow(implicit e0: GR[java.util.UUID], e1: GR[String], e2: GR[Option[String]], e3: GR[Option[scala.math.BigDecimal]], e4: GR[Option[Boolean]]): GR[StudentRow] = GR{
    prs => import prs._
    StudentRow.tupled((<<[java.util.UUID], <<[String], <<[String], <<?[String], <<?[String], <<[String], <<?[String], <<?[String], <<?[scala.math.BigDecimal], <<?[Boolean]))
  }
  /** Table description of table student. Objects of this class serve as prototypes for rows in queries. */
  class Student(_tableTag: Tag) extends profile.api.Table[StudentRow](_tableTag, "student") {
    def * = (id, email, name, student, level, phone, notes, paymentIntentId, totalCost, paymentConfirmed).<>(StudentRow.tupled, StudentRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(email), Rep.Some(name), student, level, Rep.Some(phone), notes, paymentIntentId, totalCost, paymentConfirmed)).shaped.<>({r=>import r._; _1.map(_=> StudentRow.tupled((_1.get, _2.get, _3.get, _4, _5, _6.get, _7, _8, _9, _10)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(uuid), PrimaryKey */
    val id: Rep[java.util.UUID] = column[java.util.UUID]("id", O.PrimaryKey)
    /** Database column email SqlType(text) */
    val email: Rep[String] = column[String]("email")
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")
    /** Database column student SqlType(text), Default(None) */
    val student: Rep[Option[String]] = column[Option[String]]("student", O.Default(None))
    /** Database column level SqlType(text), Default(None) */
    val level: Rep[Option[String]] = column[Option[String]]("level", O.Default(None))
    /** Database column phone SqlType(text) */
    val phone: Rep[String] = column[String]("phone")
    /** Database column notes SqlType(text), Default(None) */
    val notes: Rep[Option[String]] = column[Option[String]]("notes", O.Default(None))
    /** Database column payment_intent_id SqlType(text), Default(None) */
    val paymentIntentId: Rep[Option[String]] = column[Option[String]]("payment_intent_id", O.Default(None))
    /** Database column total_cost SqlType(numeric), Default(None) */
    val totalCost: Rep[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("total_cost", O.Default(None))
    /** Database column payment_confirmed SqlType(bool), Default(None) */
    val paymentConfirmed: Rep[Option[Boolean]] = column[Option[Boolean]]("payment_confirmed", O.Default(None))
  }
  /** Collection-like TableQuery object for table Student */
  lazy val Student = new TableQuery(tag => new Student(tag))

  /** Entity class storing rows of table Times
   *  @param id Database column id SqlType(uuid), PrimaryKey
   *  @param numberOfLessons Database column number_of_lessons SqlType(int4)
   *  @param lengthOfLessons Database column length_of_lessons SqlType(int4)
   *  @param startDate Database column start_date SqlType(timestamp)
   *  @param endDate Database column end_date SqlType(timestamp)
   *  @param studentId Database column student_id SqlType(uuid), Default(None) */
  case class TimesRow(id: java.util.UUID, numberOfLessons: Int, lengthOfLessons: Int, startDate: java.sql.Timestamp, endDate: java.sql.Timestamp, studentId: Option[java.util.UUID] = None)
  /** GetResult implicit for fetching TimesRow objects using plain SQL queries */
  implicit def GetResultTimesRow(implicit e0: GR[java.util.UUID], e1: GR[Int], e2: GR[java.sql.Timestamp], e3: GR[Option[java.util.UUID]]): GR[TimesRow] = GR{
    prs => import prs._
    TimesRow.tupled((<<[java.util.UUID], <<[Int], <<[Int], <<[java.sql.Timestamp], <<[java.sql.Timestamp], <<?[java.util.UUID]))
  }
  /** Table description of table times. Objects of this class serve as prototypes for rows in queries. */
  class Times(_tableTag: Tag) extends profile.api.Table[TimesRow](_tableTag, "times") {
    def * = (id, numberOfLessons, lengthOfLessons, startDate, endDate, studentId).<>(TimesRow.tupled, TimesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(numberOfLessons), Rep.Some(lengthOfLessons), Rep.Some(startDate), Rep.Some(endDate), studentId)).shaped.<>({r=>import r._; _1.map(_=> TimesRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(uuid), PrimaryKey */
    val id: Rep[java.util.UUID] = column[java.util.UUID]("id", O.PrimaryKey)
    /** Database column number_of_lessons SqlType(int4) */
    val numberOfLessons: Rep[Int] = column[Int]("number_of_lessons")
    /** Database column length_of_lessons SqlType(int4) */
    val lengthOfLessons: Rep[Int] = column[Int]("length_of_lessons")
    /** Database column start_date SqlType(timestamp) */
    val startDate: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("start_date")
    /** Database column end_date SqlType(timestamp) */
    val endDate: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("end_date")
    /** Database column student_id SqlType(uuid), Default(None) */
    val studentId: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("student_id", O.Default(None))

    /** Foreign key referencing Student (database name times_student) */
    lazy val studentFk = foreignKey("times_student", studentId, Student)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Times */
  lazy val Times = new TableQuery(tag => new Times(tag))
}
