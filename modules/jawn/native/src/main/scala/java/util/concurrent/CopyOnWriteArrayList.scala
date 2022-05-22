package java.util.concurrent

class CopyOnWriteArrayList[E <: AnyRef](array: Array[E]) {
  def get(i: Int): E = array(i)
  def set(i: Int, e: E): E = {
    array(i) = e
    array(i)
  }
}
