/*
* This source code is licensed under the MIT license found in the
* LICENSE.txt file in the root directory of this source tree
*/
package a14e.collz.immut

import scala.collection.generic.{CanBuildFrom, GenericCompanion}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * простой класс для роутинга и распределения по узлам
  * основная особенность, что распределение ключей по узлам не записит от истории добавления
  * а зависит только от текущих элементов в коллекции.
  * причем не может быть дублирующихся элементов.
  *
  * Добавление занимает сложность O(n*log(n)), а удаление O(n)
  * А сам роутинг O(1). Поэтому добавления и удаления должны быть по возможности редки
  *
  * пример использования: узлы в кластере и распределение между ними
  *
  * {{{
  *   val router = Router("127.0.0.1:5555", "127.0.0.1:5556", "127.0.0.1:5557")
  *   val id = getIdForUser(...)
  *   val ipForUser = router.route(id)
  * }}}
  *
  * если узел удалился или появился мы можем спокойно добавлять и на разных компах
  * распределение будет одинаковыми будет указывать на одни и те же узлы,
  *
  * {{{
  *   val router = Router("127.0.0.1:5555", "127.0.0.1:5556", "127.0.0.1:5557")
  *   val newRouter = router - "127.0.0.1:5555"
  *   val id = getIdForUser(...)
  *   val ipForUser = newRouter.route(id) // все кроме "127.0.0.1:5555"
  * }}}
  *
  * к сожалению, это требование делает более сложнреализацию механизма, который бы оставлял
  * значения для ключений на прошлых узлах, то есть все значения для ключений при добавлении и удалении
  * перераспределяются между узлами равномерно
  *
  * кто-нибудь сконтребьютите нормальную реализацию =)
  *
  * Created by Borisenko Andrew on 28.01.2017.
  */
class Router[T: ClassTag : Ordering] private(private val underlying: Array[T],
                                  private val underlyingSet: Set[T]) extends Iterable[T] with Traversable[T] {


  override def size: Int = underlying.length

  def length: Int = underlying.length

  override def isEmpty: Boolean = underlying.isEmpty

  def +(x: T): Router[T] = {
    val newSet = underlyingSet + x
    if (newSet.size == underlyingSet.size) this
    else {
      val res = (underlying :+ x).sorted
      new Router(res, newSet)
    }
  }

  def contains(x: T): Boolean = underlyingSet.contains(x)

  def ++(xs: TraversableOnce[T]): Router[T] = {
    val buffer = new ArrayBuffer[T](underlying.length) ++= underlying
    var newSet = underlyingSet
    var updated = false
    for (x <- xs) {
      if (!newSet(x)) {
        updated = true
        newSet += x
        buffer += x
      }
    }
    val res = scala.util.Sorting.stableSort[T](buffer)
    new Router(res, newSet)
  }

  def -(x: T): Router[T] = {
    if (underlyingSet(x)) {
      val res = new Array[T](underlying.length - 1)
      var i = 0
      for (temp <- underlying)
        if (temp != x) {
          res(i) = temp
          i += 1
        }

      val newSet = underlyingSet - x
      new Router(res, newSet)
    } else this
  }

  // на основе хешей из стандартной библиотеки
  // который использует мур мур хэш =)
  private def betterHash(hcode: Int): Int = {
    //    hcode ^ (hcode >>> 16)
    var h: Int = hcode + ~(hcode << 9)
    h = h ^ (h >>> 14)
    h = h + (h << 4)
    h ^ (h >>> 10)
  }

  def route[KEY](key: KEY): T = {
    if (isEmpty)
      throw new UnsupportedOperationException("route on empty router")

    val h = betterHash(key.hashCode())
    val i = h.abs % underlying.length
    underlying(i)
  }


  override def foreach[U](f: (T) => U): Unit = underlying.foreach(f)

  override def iterator: Iterator[T] = underlying.iterator
}

object Router {
  def empty[T: ClassTag : Ordering] = new Router[T](new Array[T](0), Set[T]())

  def apply[T: ClassTag : Ordering](xs: T*): Router[T] = empty[T] ++ xs


  implicit def canBuildFrom[A: ClassTag : Ordering]: CanBuildFrom[TraversableOnce[_], A, Router[A]] =
    new CanBuildFrom[TraversableOnce[_], A, Router[A]] {

      override def apply(from: TraversableOnce[_]): mutable.Builder[A, Router[A]] = newBuilder[A]

      override def apply(): mutable.Builder[A, Router[A]] = newBuilder[A]
    }

  def newBuilder[A: ClassTag : Ordering]: mutable.Builder[A, Router[A]] = new mutable.Builder[A, Router[A]] {
    private var internal = empty[A]

    override def +=(elem: A): this.type = {
      internal = internal + elem
      this
    }

    override def ++=(elem: TraversableOnce[A]): this.type = {
      internal = internal ++ elem
      this
    }

    override def clear(): Unit = {
      internal = empty[A]
    }

    override def result(): Router[A] = internal
  }
}


