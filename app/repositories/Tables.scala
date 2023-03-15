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
  lazy val schema: profile.SchemaDescription = Student.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Student
   *  @param id Database column id SqlType(uuid)
   *  @param email Database column email SqlType(text)
   *  @param name Database column name SqlType(text)
   *  @param phone Database column phone SqlType(text)
   *  @param paymentIntentId Database column payment_intent_id SqlType(text), Default(None) */
  case class StudentRow(id: java.util.UUID, email: String, name: String, phone: String, paymentIntentId: Option[String] = None)
  /** GetResult implicit for fetching StudentRow objects using plain SQL queries */
  implicit def GetResultStudentRow(implicit e0: GR[java.util.UUID], e1: GR[String], e2: GR[Option[String]]): GR[StudentRow] = GR{
    prs => import prs._
      StudentRow.tupled((<<[java.util.UUID], <<[String], <<[String], <<[String], <<?[String]))
  }
  /** Table description of table student. Objects of this class serve as prototypes for rows in queries. */
  class Student(_tableTag: Tag) extends profile.api.Table[StudentRow](_tableTag, "student") {
    def * = (id, email, name, phone, paymentIntentId).<>(StudentRow.tupled, StudentRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(email), Rep.Some(name), Rep.Some(phone), paymentIntentId)).shaped.<>({r=>import r._; _1.map(_=> StudentRow.tupled((_1.get, _2.get, _3.get, _4.get, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(uuid) */
    val id: Rep[java.util.UUID] = column[java.util.UUID]("id")
    /** Database column email SqlType(text) */
    val email: Rep[String] = column[String]("email")
    /** Database column name SqlType(text) */
    val name: Rep[String] = column[String]("name")
    /** Database column phone SqlType(text) */
    val phone: Rep[String] = column[String]("phone")
    /** Database column payment_intent_id SqlType(text), Default(None) */
    val paymentIntentId: Rep[Option[String]] = column[Option[String]]("payment_intent_id", O.Default(None))
  }
  /** Collection-like TableQuery object for table Student */
  lazy val Student = new TableQuery(tag => new Student(tag))
}
