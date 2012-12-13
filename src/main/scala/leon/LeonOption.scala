package leon

/** Describes a command-line option. */
sealed abstract class LeonOption {
  val name: String
}

/** Boolean (on/off) options. Present means "on". */
case class LeonFlagOption(name: String) extends LeonOption
/** Options of the form --option=value. */
case class LeonValueOption(name: String, value: String) extends LeonOption {
  def splitList : Seq[String] = value.split(':').map(_.trim).filter(!_.isEmpty)

  def asInt(ctx : LeonContext) : Option[Int] = try {
    Some(value.toInt)
  } catch {
    case _ : Throwable =>
      ctx.reporter.error("Option --%s takes an integer as value.".format(name))
      None
  }
}

sealed abstract class LeonOptionDef {
  val name: String
  val usageOption: String
  val usageDesc: String
  val isFlag: Boolean
}
case class LeonFlagOptionDef(name: String, usageOption: String, usageDesc: String) extends LeonOptionDef {
  val isFlag = true
}

case class LeonValueOptionDef(name: String, usageOption: String, usageDesc: String) extends LeonOptionDef {
  val isFlag = false
}

object ListValue {
  def unapply(value: String): Option[Seq[String]] = Some(value.split(':').map(_.trim).filter(!_.isEmpty))
}
