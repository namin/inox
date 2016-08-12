
package inox
package ast

trait TreeOps { self: Trees =>

  trait TreeTransformer {
    def transform(id: Identifier, tpe: Type): (Identifier, Type) = (id, transform(tpe))

    def transform(v: Variable): Variable = {
      val (id, tpe) = transform(v.id, v.tpe)
      if ((id ne v.id) || (tpe ne v.tpe)) {
        Variable(id, tpe).copiedFrom(v)
      } else {
        v
      }
    }

    def transform(vd: ValDef): ValDef = {
      val (id, tpe) = transform(vd.id, vd.tpe)
      if ((id ne vd.id) || (tpe ne vd.tpe)) {
        ValDef(id, tpe).copiedFrom(vd)
      } else {
        vd
      }
    }

    def transform(e: Expr): Expr = {
      val (es, tps, builder) = deconstructor.deconstruct(e)

      var changed = false
      val newEs = for (e <- es) yield {
        val newE = transform(e)
        if (e ne newE) changed = true
        newE
      }

      val newTps = for (tp <- tps) yield {
        val newTp = transform(tp)
        if (tp ne newTp) changed = true
        newTp
      }

      if (changed) {
        builder(newEs, newTps).copiedFrom(e)
      } else {
        e
      }
    }

    def transform(t: Type): Type = {
      val (tps, builder) = deconstructor.deconstruct(t)

      var changed = false
      val newTps = for (tp <- tps) yield {
        val newTp = transform(tp)
        if (tp ne newTp) changed = true
        newTp
      }

      if (changed) {
        builder(newTps).copiedFrom(t)
      } else {
        t
      }
    }
  }

  trait TreeTraverser {
    def traverse(vd: ValDef): Unit = traverse(vd.tpe)

    def traverse(v: Variable): Unit = traverse(v.tpe)

    def traverse(e: Expr): Unit = {
      val (es, tps, _) = deconstructor.deconstruct(e)
      es.foreach(traverse)
      tps.foreach(traverse)
    }

    def traverse(tpe: Type): Unit = {
      val (tps, _) = deconstructor.deconstruct(tpe)
      tps.foreach(traverse)
    }
  }
}
