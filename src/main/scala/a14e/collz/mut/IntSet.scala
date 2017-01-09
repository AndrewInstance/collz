package a14e.collz.mut

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable


object IntSet {

  implicit def canBuildFrom = new CanBuildFrom[IntSet, Int, IntSet] {
    def apply(from: IntSet): mutable.Builder[Int, IntSet] = apply()

    def apply(): mutable.Builder[Int, IntSet] = IntSet()
  }

  def apply(xs: Int*): IntSet = new IntSet(IntMap[Unit]()) ++= xs
}

/**
  * Created by User on 09.01.2017.
  */
class IntSet private[collz](val underlying: IntMap[Unit]) extends mutable.Set[Int] with mutable.Builder[Int, IntSet] {

  override def size: Int = underlying.size

  override def newBuilder: mutable.Builder[Int, mutable.Set[Int]] = IntSet()

  override def result(): mutable.Set[Int] = this

  override def +=(elem: Int): IntSet.this.type = {
    underlying(elem) = Unit
    this
  }

  override def -=(elem: Int): IntSet.this.type = {
    underlying -= elem
    this
  }

  override def contains(elem: Int): Boolean = underlying.contains(elem)

  override def iterator: Iterator[Int] = underlying.keysIterator

  override def foreach[U](f: (Int) => U): Unit = for ((k, _) <- underlying) f(k)

}