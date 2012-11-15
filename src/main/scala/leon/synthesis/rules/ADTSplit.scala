package leon
package synthesis
package rules

import purescala.Trees._
import purescala.Common._
import purescala.TypeTrees._
import purescala.TreeOps._
import purescala.Extractors._
import purescala.Definitions._

class ADTSplit(synth: Synthesizer) extends Rule("ADT Split.", synth, 90) {
  def applyOn(task: Task): RuleResult = {
    val p = task.problem

    val candidate = p.as.collect {
      case IsTyped(id, AbstractClassType(cd)) =>

        val optCases = for (dcd <- cd.knownDescendents) yield dcd match {
          case ccd : CaseClassDef =>
            val toVal = Implies(p.c, CaseClassInstanceOf(ccd, Variable(id)))

            val isImplied = synth.solver.solveSAT(Not(toVal)) match {
              case (Some(false), _) => true
              case _ => false
            }

            if (!isImplied) {
              Some(ccd)
            } else {
              None
            }
          case _ =>
            None
        }

        val cases = optCases.flatten

        if (!cases.isEmpty) {
          Some((id, cases))
        } else {
          None
        }
    }


    candidate.find(_.isDefined) match {
      case Some(Some((id, cases))) =>
        val oas = p.as.filter(_ != id)

        val subInfo = for(ccd <- cases) yield {
           val subId  = FreshIdentifier(ccd.id.name, true).setType(CaseClassType(ccd))
           val subPre = CaseClassInstanceOf(ccd, Variable(id))
           val subPhi = subst(id -> Variable(subId), p.phi)
           val subProblem = Problem(subId :: oas, And(p.c, subPre), subPhi, p.xs)
           val subPattern = CaseClassPattern(Some(subId), ccd, ccd.fieldsIds.map(id => WildcardPattern(None)))

           (subProblem, subPre, subPattern)
        }


        val onSuccess: List[Solution] => Solution = {
          case sols =>
            var globalPre = List[Expr]()

            val cases = for ((sol, (problem, pre, pattern)) <- (sols zip subInfo)) yield {
              globalPre ::= And(pre, sol.pre)

              SimpleCase(pattern, sol.term)
            }

            Solution(Or(globalPre), sols.flatMap(_.defs).toSet, MatchExpr(Variable(id), cases))
        }

        HeuristicStep(synth, p, subInfo.map(_._1).toList, onSuccess)
      case _ =>
        RuleInapplicable
    }
  }
}
